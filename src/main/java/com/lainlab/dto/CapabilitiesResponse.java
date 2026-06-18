package com.lainlab.dto;

import com.lainlab.model.Language;
import com.lainlab.model.Provider;

import java.util.List;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public record CapabilitiesResponse(
        List<LanguageDescription> languages,
        List<QuestionTypeDescription> questionTypes,
        List<DifficultyDescription> difficulties,
        List<ModeDescription> modes,
        List<Provider> providers,
        int maxQuestions
) {}
