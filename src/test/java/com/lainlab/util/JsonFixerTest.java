package com.lainlab.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonFixer Tests")
class JsonFixerTest {

    @Test
    @DisplayName("Should strip Markdown json code fences")
    void shouldStripJsonCodeFences() {
        String input = "```json\n{\"key\": \"value\"}\n```";
        String result = JsonFixer.fix(input);
        assertEquals("{\"key\": \"value\"}", result);
    }

    @Test
    @DisplayName("Should strip bare Markdown code fences")
    void shouldStripBareCodeFences() {
        String input = "```\n{\"key\": \"value\"}\n```";
        String result = JsonFixer.fix(input);
        assertEquals("{\"key\": \"value\"}", result);
    }

    @Test
    @DisplayName("Should remove trailing commas before closing braces")
    void shouldRemoveTrailingCommas() {
        String input = "{\"key\": \"value\",}";
        String result = JsonFixer.fix(input);
        assertEquals("{\"key\": \"value\"}", result);
    }

    @Test
    @DisplayName("Should remove trailing commas in arrays")
    void shouldRemoveTrailingCommasInArrays() {
        String input = "[1, 2, 3,]";
        String result = JsonFixer.fix(input);
        assertEquals("[1, 2, 3]", result);
    }

    @Test
    @DisplayName("Should unwrap quoted JSON")
    void shouldUnwrapQuotedJson() {
        String input = "\"{\\\"key\\\": \\\"value\\\"}\"";
        String result = JsonFixer.fix(input);
        assertEquals("{\"key\": \"value\"}", result);
    }

    @Test
    @DisplayName("Should trim whitespace")
    void shouldTrimWhitespace() {
        String input = "  {\"key\": \"value\"}  ";
        String result = JsonFixer.fix(input);
        assertEquals("{\"key\": \"value\"}", result);
    }

    @Test
    @DisplayName("Should handle combined issues: code fences + trailing comma")
    void shouldHandleCombinedIssues() {
        String input = "```json\n{\"items\": [1, 2,],}\n```";
        String result = JsonFixer.fix(input);
        assertEquals("{\"items\": [1, 2]}", result);
    }

    @Test
    @DisplayName("Should pass through already-valid JSON")
    void shouldPassValidJson() {
        String input = "{\"valid\": true, \"count\": 42}";
        String result = JsonFixer.fix(input);
        assertEquals(input, result);
    }

    @Test
    @DisplayName("Should handle empty object")
    void shouldHandleEmptyObject() {
        String input = "{}";
        String result = JsonFixer.fix(input);
        assertEquals("{}", result);
    }

    @Test
    @DisplayName("Should handle empty array")
    void shouldHandleEmptyArray() {
        String input = "[]";
        String result = JsonFixer.fix(input);
        assertEquals("[]", result);
    }
}
