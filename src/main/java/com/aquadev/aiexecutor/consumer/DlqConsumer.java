package com.aquadev.aiexecutor.consumer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DlqConsumer {

    private final MeterRegistry meterRegistry;
    private Counter dlqCounter;

    @PostConstruct
    void init() {
        dlqCounter = Counter.builder("ai.homework.dlq")
                .description("Number of homework execution events sent to DLQ")
                .register(meterRegistry);
    }

    @KafkaListener(
            topics = "${kafka.homework-execution-dlq-topic}",
            containerFactory = "dlqListenerContainerFactory"
    )
    public void consume(ConsumerRecord<String, String> record) {
        dlqCounter.increment();
        log.error("DLQ message received — topic={} partition={} offset={} key={} payload={}",
                record.topic(), record.partition(), record.offset(), record.key(), record.value());
    }
}
