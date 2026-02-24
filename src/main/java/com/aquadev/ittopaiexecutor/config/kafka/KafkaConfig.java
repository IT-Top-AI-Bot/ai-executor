package com.aquadev.ittopaiexecutor.config.kafka;

import com.aquadev.ittopaiexecutor.dto.kafka.HomeworkExecutionEvent;
import com.aquadev.ittopaiexecutor.dto.kafka.HomeworkExecutionResultEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
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
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@EnableKafka
@Configuration
@EnableConfigurationProperties(KafkaTopicProperties.class)
public class KafkaConfig {

    @Bean
    public ConsumerFactory<String, HomeworkExecutionEvent> homeworkExecutionConsumerFactory(
            org.springframework.boot.autoconfigure.kafka.KafkaProperties bootKafkaProps,
            ObjectMapper objectMapper
    ) {
        var configs = bootKafkaProps.buildConsumerProperties(null);
        configs.remove(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG);

        var deserializer = new JsonDeserializer<>(HomeworkExecutionEvent.class, objectMapper);
        deserializer.setUseTypeHeaders(false);

        return new DefaultKafkaConsumerFactory<>(configs,
                new org.apache.kafka.common.serialization.StringDeserializer(),
                new ErrorHandlingDeserializer<>(deserializer));
    }

    @Bean(name = "homeworkExecutionKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, HomeworkExecutionEvent>
    homeworkExecutionKafkaListenerContainerFactory(
            ConsumerFactory<String, HomeworkExecutionEvent> homeworkExecutionConsumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate
    ) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, HomeworkExecutionEvent>();
        factory.setConsumerFactory(homeworkExecutionConsumerFactory);
        factory.setConcurrency(1);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        var recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (rec, ex) -> {
                    log.error("Sending to DLQ topic={} partition={} offset={}: {}",
                            rec.topic(), rec.partition(), rec.offset(), ex.getMessage(), ex);
                    return new TopicPartition(rec.topic() + ".DLQ", rec.partition());
                }
        );

        var errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000, 3));
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
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.RETRIES_CONFIG, Integer.toString(Integer.MAX_VALUE));
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        DefaultKafkaProducerFactory<String, HomeworkExecutionResultEvent> pf =
                new DefaultKafkaProducerFactory<>(config);

        pf.setValueSerializer(new JsonSerializer<>(objectMapper));

        return pf;
    }

    @Bean
    public KafkaTemplate<String, HomeworkExecutionResultEvent> resultKafkaTemplate(
            ProducerFactory<String, HomeworkExecutionResultEvent> resultProducerFactory
    ) {
        return new KafkaTemplate<>(resultProducerFactory);
    }

    @Bean
    public ProducerFactory<String, Object> dlqProducerFactory(KafkaProperties props) {
        return new DefaultKafkaProducerFactory<>(props.buildProducerProperties());
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> dlqProducerFactory) {
        return new KafkaTemplate<>(dlqProducerFactory);
    }
}
