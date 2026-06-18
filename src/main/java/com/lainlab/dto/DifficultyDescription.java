package com.lainlab.dto;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record DifficultyDescription(
        String level,
        String title,
        String description
) {}
