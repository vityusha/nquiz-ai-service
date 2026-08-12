package com.lainlab.util;

import com.lainlab.model.Difficulty;
import com.lainlab.model.QuestionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PromptLabels Tests")
class PromptLabelsTest {

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
}
