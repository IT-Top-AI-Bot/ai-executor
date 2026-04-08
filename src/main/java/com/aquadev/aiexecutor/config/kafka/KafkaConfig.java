package com.aquadev.aiexecutor.config.kafka;

import com.aquadev.aiexecutor.config.kafka.serializer.JacksonKafkaDeserializer;
import com.aquadev.aiexecutor.config.kafka.serializer.JacksonKafkaSerializer;
import com.aquadev.aiexecutor.config.kafka.serializer.SmartKafkaSerializer;
import com.aquadev.commonlibs.HomeworkExecutionEvent;
import com.aquadev.commonlibs.HomeworkExecutionResultEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jspecify.annotations.NonNull;
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
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({KafkaTopicProperties.class, KafkaProperties.class})
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;
    private final ObjectMapper objectMapper;

    @Bean
    public ConsumerFactory<String, HomeworkExecutionEvent> homeworkExecutionConsumerFactory() {
        var configs = kafkaProperties.buildConsumerProperties();
        configs.remove(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG);
        return new DefaultKafkaConsumerFactory<>(configs,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(new JacksonKafkaDeserializer<>(HomeworkExecutionEvent.class, objectMapper)));
    }

    @Bean(name = "homeworkExecutionKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, HomeworkExecutionEvent>
    homeworkExecutionKafkaListenerContainerFactory(
            ConsumerFactory<String, HomeworkExecutionEvent> homeworkExecutionConsumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate,
            KafkaFailedResultPublisher failedResultPublisher
    ) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, HomeworkExecutionEvent>();
        factory.setConsumerFactory(homeworkExecutionConsumerFactory);
        factory.setConcurrency(1);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        factory.getContainerProperties().setObservationEnabled(true);

        var deadLetterRecoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (consumerRecord, ex) -> {
                    log.error("Sending to DLQ topic={} partition={} offset={}: {}",
                            consumerRecord.topic(), consumerRecord.partition(), consumerRecord.offset(), ex.getMessage(), ex);
                    return new TopicPartition(consumerRecord.topic() + ".DLQ", consumerRecord.partition());
                }
        );

        var errorHandler = buildErrorHandler(failedResultPublisher, deadLetterRecoverer);
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
        errorHandler.addNotRetryableExceptions(DeserializationException.class);

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    private @NonNull DefaultErrorHandler buildErrorHandler(
            KafkaFailedResultPublisher failedResultPublisher,
            DeadLetterPublishingRecoverer deadLetterRecoverer
    ) {
        var backOff = new ExponentialBackOff(60_000L, 2.0);
        backOff.setMaxInterval(300_000L);
        backOff.setMaxElapsedTime(1_800_000L);

        var errorHandler = new DefaultErrorHandler((consumerRecord, ex) -> {
            failedResultPublisher.publishFailedResult(consumerRecord, ex);
            deadLetterRecoverer.accept(consumerRecord, ex);
        }, backOff);

        errorHandler.setCommitRecovered(true);
        errorHandler.setRetryListeners((consumerRecord, ex, deliveryAttempt) -> {
            Throwable rootCause = ex;
            for (Throwable c = ex; c != null; c = c.getCause()) rootCause = c;
            assert rootCause != null;
            log.warn("Retrying topic={} partition={} offset={} attempt={}: {} - {}",
                    consumerRecord.topic(), consumerRecord.partition(), consumerRecord.offset(), deliveryAttempt,
                    rootCause.getClass().getSimpleName(), rootCause.getMessage());
        });
        return errorHandler;
    }

    @Bean
    public ProducerFactory<String, HomeworkExecutionResultEvent> resultProducerFactory() {
        Map<String, Object> config = new HashMap<>(kafkaProperties.buildProducerProperties());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.RETRIES_CONFIG, Integer.toString(Integer.MAX_VALUE));
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        DefaultKafkaProducerFactory<String, HomeworkExecutionResultEvent> pf =
                new DefaultKafkaProducerFactory<>(config);
        pf.setValueSerializer(new JacksonKafkaSerializer<>(objectMapper));
        return pf;
    }

    @Bean
    public KafkaTemplate<String, HomeworkExecutionResultEvent> resultKafkaTemplate(
            ProducerFactory<String, HomeworkExecutionResultEvent> resultProducerFactory
    ) {
        var template = new KafkaTemplate<>(resultProducerFactory);
        template.setObservationEnabled(true);
        return template;
    }

    @Bean
    public ConsumerFactory<String, String> dlqConsumerFactory() {
        Map<String, Object> configs = new HashMap<>(kafkaProperties.buildConsumerProperties());
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.GROUP_ID_CONFIG, "it-top-ai-executor-dlq-monitor");
        return new DefaultKafkaConsumerFactory<>(configs);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> dlqListenerContainerFactory(
            ConsumerFactory<String, String> dlqConsumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
        factory.setConsumerFactory(dlqConsumerFactory);
        return factory;
    }

    @Bean
    public ProducerFactory<String, Object> dlqProducerFactory() {
        Map<String, Object> config = new HashMap<>(kafkaProperties.buildProducerProperties());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        DefaultKafkaProducerFactory<String, Object> pf = new DefaultKafkaProducerFactory<>(config);
        pf.setValueSerializer(new SmartKafkaSerializer(objectMapper));
        return pf;
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> dlqProducerFactory) {
        var template = new KafkaTemplate<>(dlqProducerFactory);
        template.setObservationEnabled(true);
        return template;
    }
}
