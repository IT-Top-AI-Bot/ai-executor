package com.aquadev.aiexecutor.config.kafka.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

public record SmartKafkaSerializer(ObjectMapper objectMapper) implements Serializer<Object> {

    @Override
    public byte[] serialize(String topic, Object data) {
        if (data == null) return new byte[0];
        if (data instanceof byte[] bytes) return bytes;
        try {
            return objectMapper.writeValueAsBytes(data);
        } catch (JsonProcessingException e) {
            throw new SerializationException("Error serializing JSON for topic: " + topic, e);
        }
    }
}
