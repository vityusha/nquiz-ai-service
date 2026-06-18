package com.lainlab.dto;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record QuestionTypeDescription(
        String type,
        String title,
        String description
) {}
