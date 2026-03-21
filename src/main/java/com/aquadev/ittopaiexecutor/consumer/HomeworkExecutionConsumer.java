package com.aquadev.ittopaiexecutor.consumer;

import com.aquadev.commonlibs.HomeworkExecutionEvent;
import com.aquadev.ittopaiexecutor.handler.HomeworkExecutionHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
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
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {

        log.info("Kafka received: executionId={}, homeworkId={}, specId={}, url={}, partition={}, offset={}",
                event.id(), event.homeworkId(), event.specId(), event.homeworkUrl(), partition, offset);

        if (event.id() == null) throw new IllegalArgumentException("id is null");
        if (event.homeworkId() == null) throw new IllegalArgumentException("homeworkId is null");
        if (event.homeworkUrl() == null || event.homeworkUrl().isBlank())
            throw new IllegalArgumentException("homeworkUrl empty");

        handler.handle(event);
        acknowledgment.acknowledge();
    }
}
