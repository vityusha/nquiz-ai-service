package com.lainlab.dto;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record LanguageDescription(
        String language,
        String title,
        String description
) {};
