package com.lainlab.util;

import com.lainlab.model.Difficulty;
import com.lainlab.model.QuestionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PromptLabels Tests")
class PromptLabelsTest {

    private static final Random FIXED = new Random(42);

    @Test
    @DisplayName("Should provide non-empty labels for every difficulty")
    void shouldCoverAllDifficulties() {
        for (Difficulty difficulty : Difficulty.values()) {
            PromptLabels.Entry entry = PromptLabels.forDifficulty(difficulty);
            assertFalse(entry.title().isBlank(), difficulty.name() + " title");
            assertFalse(entry.description().isBlank(), difficulty.name() + " description");
        }
    }

    @Test
    @DisplayName("Should provide non-empty labels for every question type")
    void shouldCoverAllQuestionTypes() {
        for (QuestionType type : QuestionType.values()) {
            PromptLabels.Entry entry = PromptLabels.forQuestionType(type);
            assertFalse(entry.title().isBlank(), type.name() + " title");
            assertFalse(entry.description().isBlank(), type.name() + " description");
        }
        assertEquals(QuestionType.values().length, Arrays.stream(QuestionType.values()).count());
    }

    @Test
    @DisplayName("Should provide type-aware batch variation hints")
    void shouldProvideTypeAwareVariations() {
        for (QuestionType type : QuestionType.values()) {
            String variation = PromptLabels.randomVariation(type, FIXED);
            assertFalse(variation.isBlank(), type.name());
            assertTrue(variation.length() > 20, type.name() + " should be a concrete instruction");
        }
    }

    @Test
    @DisplayName("TENSES variations should mention tense-related diversity")
    void tensesVariationsShouldStayOnTopic() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 20; i++) {
            seen.add(PromptLabels.randomVariation(QuestionType.TENSES, new Random(i)));
        }
        assertTrue(seen.size() >= 2, "TENSES should have multiple variation options");
        assertTrue(seen.stream().anyMatch(v -> v.toLowerCase().contains("past")
                || v.toLowerCase().contains("present")
                || v.toLowerCase().contains("future")
                || v.toLowerCase().contains("tense")
                || v.toLowerCase().contains("aspect")));
    }
}
