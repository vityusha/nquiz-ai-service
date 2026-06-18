package com.lainlab.util;

import com.lainlab.model.Mode;

import java.util.HashMap;
import java.util.Map;

public class PromptBuilder {

    public static PromptBundle build(
            String systemTemplate,
            String userTemplate,
            Map<String, Object> ctx,
            Mode mode
    ) {
        // 2) OpenAI
        String system = apply(systemTemplate, ctx);
        String user   = apply(userTemplate, ctx);

        // 3) Gemini
        String combined = system + "\n\n" + user;

        return new PromptBundle(system, user, combined);
    }

    private static String apply(String template, Map<String, Object> ctx) {
        String result = template;
        for (var e : ctx.entrySet()) {
            result = result.replace("{{" + e.getKey() + "}}", e.getValue() != null ? e.getValue().toString() : "");
        }
        return result;
    }

    // Bundle for OpenAI + Gemini
    public record PromptBundle(String system, String user, String combined) {}
}
