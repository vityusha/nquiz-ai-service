package com.lainlab.util;

import com.lainlab.model.Mode;
import io.micronaut.context.annotation.Context;
import io.micronaut.core.io.ResourceResolver;

import jakarta.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Singleton
@Context
public class PromptCache {

    private final String systemSinglePrompt;
    private final String systemMultiPrompt;
    private final String systemOrderingPrompt;
    private final String systemMatchingPrompt;

    private final String userPrompt;

    public PromptCache(ResourceResolver resolver) {
        this.systemSinglePrompt = load(resolver, "prompt_single.tmpl");
        this.systemMultiPrompt = load(resolver, "prompt_multi.tmpl");
        this.systemOrderingPrompt = load(resolver, "prompt_ordering.tmpl");
        this.systemMatchingPrompt = load(resolver, "prompt_matching.tmpl");

        this.userPrompt   = load(resolver, "prompt_user.tmpl");
    }

    private String load(ResourceResolver resolver, String path) {
        return resolver.getResource("classpath:prompts/" + path)
                .map(url -> {
                    try (InputStream is = url.openStream()) {
                        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to load prompt: " + path, e);
                    }
                })
                .orElseThrow(() -> new RuntimeException("Prompt not found: " + path));
    }

    public String system(Mode mode) {
        return switch (mode) {
            case ONE_CORRECT -> systemSinglePrompt;
            case MULTI_CORRECT -> systemMultiPrompt;
            case MATCHING -> systemMatchingPrompt;
            case ORDERING -> systemOrderingPrompt;
        };
    }

    public String user() {
        return userPrompt;
    }
}
