package com.aquadev.aiexecutor.consumer;

import com.aquadev.aiexecutor.handler.HomeworkExecutionHandler;
import com.aquadev.commonlibs.HomeworkExecutionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HomeworkExecutionConsumer {

    private final HomeworkExecutionHandler handler;

    @KafkaListener(
            topics = "${kafka.homework-execution-topic}",
            containerFactory = "homeworkExecutionKafkaListenerContainerFactory"
    )
    public void consume(
            @Payload HomeworkExecutionEvent event,
            Acknowledgment acknowledgment) {

        log.info("Kafka received event: {}", event);

        if (event.id() == null) throw new IllegalArgumentException("id is null for event");
        if (event.homeworkId() == null) throw new IllegalArgumentException("homeworkId is null");
        if (event.homeworkUrl() == null || event.homeworkUrl().isBlank())
            throw new IllegalArgumentException("homeworkUrl empty");

        handler.handle(event);
        acknowledgment.acknowledge();
    }
}
