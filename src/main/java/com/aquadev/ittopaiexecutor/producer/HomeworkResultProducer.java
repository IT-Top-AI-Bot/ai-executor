package com.aquadev.ittopaiexecutor.producer;

import com.aquadev.ittopaiexecutor.config.kafka.KafkaTopicProperties;
import com.aquadev.ittopaiexecutor.dto.kafka.HomeworkExecutionResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class HomeworkResultProducer {

    public static final String HDR_CORRELATION_ID = "correlationId";

    private final KafkaTopicProperties kafkaTopicProperties;
    private final KafkaTemplate<String, HomeworkExecutionResultEvent> resultKafkaTemplate;

    public void sendCompleted(
            UUID executionId,
            String s3Key,
            @Nullable String traceparent,
            @Nullable String correlationId
    ) {
        send(executionId, HomeworkExecutionResultEvent.completed(executionId, s3Key),
                traceparent, correlationId);
    }

    public void sendFailed(
            UUID executionId,
            String errorMessage,
            @Nullable String traceparent,
            @Nullable String correlationId
    ) {
        send(executionId, HomeworkExecutionResultEvent.failed(executionId, errorMessage),
                traceparent, correlationId);
    }

    private void send(
            UUID executionId,
            HomeworkExecutionResultEvent event,
            @Nullable String traceparent,
            @Nullable String correlationId
    ) {
        String key = executionId.toString();

        ProducerRecord<String, HomeworkExecutionResultEvent> record =
                new ProducerRecord<>(kafkaTopicProperties.homeworkResultTopic(), key, event);

        if (correlationId != null && !correlationId.isBlank()) {
            record.headers().add(HDR_CORRELATION_ID, correlationId.getBytes(StandardCharsets.UTF_8));
        }
        if (traceparent != null && !traceparent.isBlank()) {
            record.headers().add("traceparent", traceparent.getBytes(StandardCharsets.UTF_8));
        }

        try {
            SendResult<String, HomeworkExecutionResultEvent> result =
                    resultKafkaTemplate.send(record).get(10, TimeUnit.SECONDS);

            RecordMetadata md = result.getRecordMetadata();
            log.info("Result sent: executionId={}, status={}, topic={}, partition={}, offset={}",
                    executionId, event.status(), md.topic(), md.partition(), md.offset());

        } catch (Exception ex) {
            log.error("Failed to send result event: executionId={}, status={}",
                    executionId, event.status(), ex);
            throw new IllegalStateException("Failed to publish result for executionId=" + executionId, ex);
        }
    }
}
