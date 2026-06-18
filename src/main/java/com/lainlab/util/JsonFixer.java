package com.lainlab.util;

public class JsonFixer {

    public static String fix(String content) {
        content = content.trim();

        // JSON insude string
        if (content.startsWith("\"") && content.endsWith("\"")) {
            content = content.substring(1, content.length() - 1);
            content = content.replace("\\\"", "\"");
        }

        // Убираем запятые перед закрывающими скобками
        content = content.replaceAll(",\\s*([}\\]])", "$1");

        // Убираем Markdown-кодовые блоки
        if (content.startsWith("```")) {
            content = content.replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();
        }

        return content;
    }
}
