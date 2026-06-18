package com.lainlab.dto;

import com.lainlab.util.PromptBuilder;

public record LLMRequest(PromptBuilder.PromptBundle bundle, int maxTokens) {}
