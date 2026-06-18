package com.lainlab.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Model enums.
 * Verifies that all enum types are correctly defined and accessible.
 */
@DisplayName("Model Enum Tests")
class ModelEnumTest {

    @Test
    @DisplayName("Provider enum should contain all providers")
    void testProviderEnum() {
        Provider[] providers = Provider.values();

        assertTrue(providers.length > 0, "Should have at least one provider");
        assertTrue(contains(providers, Provider.DEEPSEEK), "Should contain DEEPSEEK");
        assertTrue(contains(providers, Provider.GEMINI), "Should contain GEMINI");
        assertTrue(contains(providers, Provider.OPENAI), "Should contain OPENAI");
        assertTrue(contains(providers, Provider.GROQ), "Should contain GROQ");
    }

    @Test
    @DisplayName("Language enum should contain multiple languages")
    void testLanguageEnum() {
        Language[] languages = Language.values();

        assertTrue(languages.length > 0, "Should have at least one language");
    }

    @Test
    @DisplayName("Difficulty enum should contain multiple levels")
    void testDifficultyEnum() {
        Difficulty[] difficulties = Difficulty.values();

        assertTrue(difficulties.length > 0, "Should have at least one difficulty level");
    }

    @Test
    @DisplayName("QuestionType enum should contain multiple types")
    void testQuestionTypeEnum() {
        QuestionType[] types = QuestionType.values();

        assertTrue(types.length > 0, "Should have at least one question type");
    }

    @Test
    @DisplayName("Mode enum should contain multiple modes")
    void testModeEnum() {
        Mode[] modes = Mode.values();

        assertTrue(modes.length > 0, "Should have at least one mode");
    }

    @Test
    @DisplayName("Enum values should be obtainable by name")
    void testEnumValueOf() {
        Provider provider = Provider.valueOf("DEEPSEEK");
        assertEquals(Provider.DEEPSEEK, provider);

        Language language = Language.valueOf(Language.values()[0].name());
        assertNotNull(language);
    }

    @Test
    @DisplayName("Enum should have consistent string representation")
    void testEnumStringRepresentation() {
        String providerName = Provider.GEMINI.name();
        assertEquals("GEMINI", providerName);
    }

    private <T> boolean contains(T[] array, T value) {
        for (T item : array) {
            if (item.equals(value)) {
                return true;
            }
        }
        return false;
    }
}

