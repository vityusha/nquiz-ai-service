package com.lainlab.converter;

import com.lainlab.dto.QuestionResponseList;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.data.model.runtime.convert.AttributeConverter;
import io.micronaut.serde.ObjectMapper;
import jakarta.inject.Singleton;

import java.io.IOException;

@Singleton
public class QuestionResponseListConverter implements AttributeConverter<QuestionResponseList, String> {

    private final ObjectMapper objectMapper;

    public QuestionResponseListConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String convertToPersistedValue(QuestionResponseList value, ConversionContext context) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException e) {
            throw new RuntimeException("Error converting QuestionResponseList to JSON", e);
        }
    }

    @Override
    public QuestionResponseList convertToEntityValue(String value, ConversionContext context) {
        if (value == null || value.isEmpty()) return null;
        try {
            return objectMapper.readValue(value, QuestionResponseList.class);
        } catch (IOException e) {
            throw new RuntimeException("Error converting JSON to QuestionResponseList", e);
        }
    }
}
