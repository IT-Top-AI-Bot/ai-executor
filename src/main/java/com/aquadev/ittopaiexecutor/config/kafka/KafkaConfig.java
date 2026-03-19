package com.aquadev.ittopaiexecutor.config.kafka;

import com.aquadev.commonlibs.HomeworkExecutionEvent;
import com.aquadev.commonlibs.HomeworkExecutionResultEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class KafkaConfig {

    private static final String HDR_CORRELATION_ID = "correlationId";
    private static final String HDR_TRACEPARENT = "traceparent";
    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    @Bean
    public ConsumerFactory<String, HomeworkExecutionEvent> homeworkExecutionConsumerFactory(
            KafkaProperties bootKafkaProps,
            ObjectMapper objectMapper
    ) {
        var configs = bootKafkaProps.buildConsumerProperties();
        configs.remove(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG);

        return new DefaultKafkaConsumerFactory<>(configs,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(new JacksonDeserializer<>(HomeworkExecutionEvent.class, objectMapper)));
    }

    @Bean(name = "homeworkExecutionKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, HomeworkExecutionEvent>
    homeworkExecutionKafkaListenerContainerFactory(
            ConsumerFactory<String, HomeworkExecutionEvent> homeworkExecutionConsumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate,
            KafkaTemplate<String, HomeworkExecutionResultEvent> resultKafkaTemplate,
            KafkaTopicProperties kafkaTopicProperties
    ) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, HomeworkExecutionEvent>();
        factory.setConsumerFactory(homeworkExecutionConsumerFactory);
        factory.setConcurrency(1);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);

        var deadLetterRecoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (rec, ex) -> {
                    log.error("Sending to DLQ topic={} partition={} offset={}: {}",
                            rec.topic(), rec.partition(), rec.offset(), ex.getMessage(), ex);
                    return new TopicPartition(rec.topic() + ".DLQ", rec.partition());
                }
        );

        var errorHandler = new DefaultErrorHandler((record, ex) -> {
            publishFailedResult(record, ex, resultKafkaTemplate, kafkaTopicProperties);
            deadLetterRecoverer.accept(record, ex);
        }, new FixedBackOff(1000, 3));

        errorHandler.setCommitRecovered(true);
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("Retrying topic={} partition={} offset={} attempt={} due to {}",
                        record.topic(), record.partition(), record.offset(), deliveryAttempt, ex.toString()));

        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
        errorHandler.addNotRetryableExceptions(DeserializationException.class);

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    @Bean
    public ProducerFactory<String, HomeworkExecutionResultEvent> resultProducerFactory(
            KafkaProperties props,
            ObjectMapper objectMapper
    ) {
        Map<String, Object> config = new HashMap<>(props.buildProducerProperties());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.RETRIES_CONFIG, Integer.toString(Integer.MAX_VALUE));
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        DefaultKafkaProducerFactory<String, HomeworkExecutionResultEvent> pf =
                new DefaultKafkaProducerFactory<>(config);
        pf.setValueSerializer(new JacksonSerializer<>(objectMapper));
        return pf;
    }

    @Bean
    public KafkaTemplate<String, HomeworkExecutionResultEvent> resultKafkaTemplate(
            ProducerFactory<String, HomeworkExecutionResultEvent> resultProducerFactory
    ) {
        return new KafkaTemplate<>(resultProducerFactory);
    }

    @Bean
    public ProducerFactory<String, Object> dlqProducerFactory(KafkaProperties props, ObjectMapper objectMapper) {
        Map<String, Object> config = new HashMap<>(props.buildProducerProperties());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        DefaultKafkaProducerFactory<String, Object> pf = new DefaultKafkaProducerFactory<>(config);
        pf.setValueSerializer(new SmartSerializer(objectMapper));
        return pf;
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> dlqProducerFactory) {
        return new KafkaTemplate<>(dlqProducerFactory);
    }

    private void publishFailedResult(
            ConsumerRecord<?, ?> record,
            Exception ex,
            KafkaTemplate<String, HomeworkExecutionResultEvent> resultKafkaTemplate,
            KafkaTopicProperties kafkaTopicProperties
    ) {
        if (!(record.value() instanceof HomeworkExecutionEvent event) || event.id() == null) {
            log.warn("Cannot publish FAILED result event: invalid payload for topic={} partition={} offset={}",
                    record.topic(), record.partition(), record.offset());
            return;
        }

        String errorMessage = buildErrorMessage(ex);
        HomeworkExecutionResultEvent failedEvent = HomeworkExecutionResultEvent.failed(event.id(), errorMessage);

        ProducerRecord<String, HomeworkExecutionResultEvent> failedRecord =
                new ProducerRecord<>(kafkaTopicProperties.homeworkResultTopic(), event.id().toString(), failedEvent);

        copyHeaderIfPresent(record, failedRecord, HDR_CORRELATION_ID);
        copyHeaderIfPresent(record, failedRecord, HDR_TRACEPARENT);

        try {
            var metadata = resultKafkaTemplate.send(failedRecord).get(10, TimeUnit.SECONDS).getRecordMetadata();
            log.info("FAILED result sent: executionId={}, topic={}, partition={}, offset={}",
                    event.id(), metadata.topic(), metadata.partition(), metadata.offset());
        } catch (Exception sendEx) {
            log.error("Could not send FAILED result event for executionId={}", event.id(), sendEx);
        }
    }

    private void copyHeaderIfPresent(
            ConsumerRecord<?, ?> source,
            ProducerRecord<String, HomeworkExecutionResultEvent> target,
            String headerName
    ) {
        var header = source.headers().lastHeader(headerName);
        if (header != null && header.value() != null) {
            target.headers().add(headerName, header.value());
        }
    }

    private String buildErrorMessage(Exception ex) {
        String message = "%s: %s".formatted(
                ex.getClass().getSimpleName(),
                ex.getMessage() == null ? "n/a" : ex.getMessage());
        if (message.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_ERROR_MESSAGE_LENGTH) + "... [truncated]";
    }

    private record JacksonDeserializer<T>(Class<T> targetType, ObjectMapper objectMapper) implements Deserializer<T> {

        @Override
            public T deserialize(String topic, byte[] data) {
                if (data == null) return null;
                try {
                    return objectMapper.readValue(data, targetType);
                } catch (Exception e) {
                    throw new SerializationException("Error deserializing JSON from topic: " + topic, e);
                }
            }
        }

    private record JacksonSerializer<T>(ObjectMapper objectMapper) implements Serializer<T> {

        @Override
            public byte[] serialize(String topic, T data) {
                if (data == null) return null;
                try {
                    return objectMapper.writeValueAsBytes(data);
                } catch (JsonProcessingException e) {
                    throw new SerializationException("Error serializing JSON for topic: " + topic, e);
                }
            }
        }

    private record SmartSerializer(ObjectMapper objectMapper) implements Serializer<Object> {

        @Override
            public byte[] serialize(String topic, Object data) {
                if (data == null) return null;
                if (data instanceof byte[] bytes) return bytes;
                try {
                    return objectMapper.writeValueAsBytes(data);
                } catch (JsonProcessingException e) {
                    throw new SerializationException("Error serializing JSON for topic: " + topic, e);
                }
            }
        }
}
