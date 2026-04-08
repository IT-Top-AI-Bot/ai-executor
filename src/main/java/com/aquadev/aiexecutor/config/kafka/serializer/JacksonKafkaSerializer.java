package com.aquadev.aiexecutor.config.kafka.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

public record JacksonKafkaSerializer<T>(ObjectMapper objectMapper) implements Serializer<T> {

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) return new byte[0];
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Error serializing JSON for topic: " + topic, e);
        }
    }
}
