package com.aquadev.aiexecutor.config.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kafka")
public record KafkaTopicProperties(
        String homeworkExecutionTopic,
        String homeworkResultTopic,
        String homeworkExecutionDlqTopic
) {
}
