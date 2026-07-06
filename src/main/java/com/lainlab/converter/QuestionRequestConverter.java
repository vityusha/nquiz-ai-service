package com.lainlab.converter;

import com.lainlab.dto.QuestionRequest;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.data.model.runtime.convert.AttributeConverter;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Singleton;

import java.io.IOException;

@Singleton
public class QuestionRequestConverter implements AttributeConverter<QuestionRequest, String> {

    private final ObjectMapper objectMapper;

    // Micronaut автоматически внедрит Serde ObjectMapper
    public QuestionRequestConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // Вызывается перед INSERT/UPDATE в базу
    @Override
    public String convertToPersistedValue(QuestionRequest value, ConversionContext context) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException e) {
            throw new RuntimeException("Error converting QuestionRequest to JSON", e);
        }
    }

    // Вызывается после SELECT из базы
    @Override
    public QuestionRequest convertToEntityValue(String value, ConversionContext context) {
        if (value == null || value.isEmpty()) return null;
        try {
            return objectMapper.readValue(value, QuestionRequest.class);
        } catch (IOException e) {
            throw new RuntimeException("Error converting JSON to QuestionRequest", e);
        }
    }
}
