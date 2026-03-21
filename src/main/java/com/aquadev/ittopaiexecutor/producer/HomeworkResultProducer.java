package com.aquadev.ittopaiexecutor.producer;

import com.aquadev.commonlibs.HomeworkExecutionResultEvent;
import com.aquadev.ittopaiexecutor.config.kafka.KafkaTopicProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class HomeworkResultProducer {

    private final KafkaTopicProperties kafkaTopicProperties;
    private final KafkaTemplate<String, HomeworkExecutionResultEvent> resultKafkaTemplate;

    public void sendCompleted(UUID executionId, String s3Key) {
        send(executionId, HomeworkExecutionResultEvent.completed(executionId, s3Key));
    }

    public void sendCompletedText(UUID executionId, String text) {
        send(executionId, HomeworkExecutionResultEvent.completedText(executionId, text));
    }

    private void send(UUID executionId, HomeworkExecutionResultEvent event) {
        String key = executionId.toString();

        ProducerRecord<String, HomeworkExecutionResultEvent> producerRecord =
                new ProducerRecord<>(kafkaTopicProperties.homeworkResultTopic(), key, event);

        try {
            SendResult<String, HomeworkExecutionResultEvent> result =
                    resultKafkaTemplate.send(producerRecord).get(10, TimeUnit.SECONDS);

            RecordMetadata md = result.getRecordMetadata();
            log.info("Result sent: executionId={}, status={}, topic={}, partition={}, offset={}",
                    executionId, event.status(), md.topic(), md.partition(), md.offset());

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing result for executionId=" + executionId, ex);
        } catch (Exception ex) {
            log.error("Failed to send result event: executionId={}, status={}",
                    executionId, event.status(), ex);
            throw new IllegalStateException("Failed to publish result for executionId=" + executionId, ex);
        }
    }
}
