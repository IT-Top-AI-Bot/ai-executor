package com.aquadev.aiexecutor.config.kafka;

import com.aquadev.commonlibs.HomeworkExecutionEvent;
import com.aquadev.commonlibs.HomeworkExecutionResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaFailedResultPublisher {

    private static final int MAX_ERROR_MESSAGE_LENGTH = 500;

    private final KafkaTopicProperties kafkaTopicProperties;
    private final KafkaTemplate<String, HomeworkExecutionResultEvent> resultKafkaTemplate;

    @SuppressWarnings("unchecked")
    public void publishFailedResult(ConsumerRecord<?, ?> consumerRecord, Exception ex) {
        String errorMessage = buildErrorMessage(ex);

        if (consumerRecord.value() instanceof HomeworkExecutionEvent event && event.id() != null) {
            sendFailedResult(event.id(), errorMessage);
        } else if (consumerRecord.value() instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof HomeworkExecutionEvent event && event.id() != null) {
                    sendFailedResult(event.id(), errorMessage);
                }
            }
        } else {
            log.warn("Cannot publish FAILED result event: invalid payload for topic={} partition={} offset={}",
                    consumerRecord.topic(), consumerRecord.partition(), consumerRecord.offset());
        }
    }

    private void sendFailedResult(UUID executionId, String errorMessage) {
        HomeworkExecutionResultEvent failedEvent = HomeworkExecutionResultEvent.failed(executionId, errorMessage);
        ProducerRecord<String, HomeworkExecutionResultEvent> record =
                new ProducerRecord<>(kafkaTopicProperties.homeworkResultTopic(), executionId.toString(), failedEvent);
        try {
            var metadata = resultKafkaTemplate.send(record).get(10, TimeUnit.SECONDS).getRecordMetadata();
            log.info("FAILED result sent: executionId={}, topic={}, partition={}, offset={}",
                    executionId, metadata.topic(), metadata.partition(), metadata.offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while sending FAILED result event for executionId={}", executionId, e);
        } catch (Exception sendEx) {
            log.error("Could not send FAILED result event for executionId={}", executionId, sendEx);
        }
    }

    private String buildErrorMessage(Exception ex) {
        Throwable rootCause = ex;
        for (Throwable c = ex; c != null; c = c.getCause()) rootCause = c;

        String message = "%s: %s".formatted(
                rootCause.getClass().getSimpleName(),
                rootCause.getMessage() == null ? "n/a" : rootCause.getMessage());
        if (message.length() <= MAX_ERROR_MESSAGE_LENGTH) {
            return message;
        }
        return message.substring(0, MAX_ERROR_MESSAGE_LENGTH) + "... [truncated]";
    }
}
