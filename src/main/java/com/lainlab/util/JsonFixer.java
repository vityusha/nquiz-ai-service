package com.lainlab.util;

public class JsonFixer {

    public static String fix(String content) {
        content = content.trim();

        // JSON insude string
        if (content.startsWith("\"") && content.endsWith("\"")) {
            content = content.substring(1, content.length() - 1);
            content = content.replace("\\\"", "\"");
        }

        // Remove trailing commas before closing brackets
        content = content.replaceAll(",\\s*([}\\]])", "$1");

        // Strip Markdown code fences
        if (content.startsWith("```")) {
            content = content.replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();
        }

        return content;
    }
}
