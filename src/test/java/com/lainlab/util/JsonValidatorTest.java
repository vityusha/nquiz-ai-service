package com.lainlab.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lainlab.model.Mode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonValidator Tests")
class JsonValidatorTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    private JsonNode parse(String json) throws Exception {
        return mapper.readTree(json);
    }

    @Test
    @DisplayName("Should accept valid ONE_CORRECT question")
    void shouldAcceptValidOneCorrect() throws Exception {
        JsonNode root = parse("""
                {
                  "questions": [{
                    "question": "What is 2+2?",
                    "answers": [
                      {"answer": "3", "right": false},
                      {"answer": "4", "right": true}
                    ]
                  }]
                }
                """);

        List<String> errors = JsonValidator.validateQuestionsArray(root, Mode.ONE_CORRECT);
        assertTrue(errors.isEmpty(), "Valid question should have no errors: " + errors);
    }

    @Test
    @DisplayName("Should accept valid MULTI_CORRECT question")
    void shouldAcceptValidMultiCorrect() throws Exception {
        JsonNode root = parse("""
                {
                  "questions": [{
                    "question": "Select primes:",
                    "answers": [
                      {"answer": "2", "right": true},
                      {"answer": "3", "right": true},
                      {"answer": "4", "right": false}
                    ]
                  }]
                }
                """);

        List<String> errors = JsonValidator.validateQuestionsArray(root, Mode.MULTI_CORRECT);
        assertTrue(errors.isEmpty(), "Valid MULTI_CORRECT should have no errors: " + errors);
    }

    @Test
    @DisplayName("Should accept valid ORDERING question")
    void shouldAcceptValidOrdering() throws Exception {
        JsonNode root = parse("""
                {
                  "questions": [{
                    "question": "Sort by size:",
                    "answers": [
                      {"answer": "small"},
                      {"answer": "medium"},
                      {"answer": "large"}
                    ]
                  }]
                }
                """);

        List<String> errors = JsonValidator.validateQuestionsArray(root, Mode.ORDERING);
        assertTrue(errors.isEmpty(), "Valid ORDERING should have no errors: " + errors);
    }

    @Test
    @DisplayName("Should accept valid MATCHING question")
    void shouldAcceptValidMatching() throws Exception {
        JsonNode root = parse("""
                {
                  "questions": [{
                    "question": "Match countries to capitals:",
                    "answers": [
                      {"answer": "France - Paris"},
                      {"answer": "Germany - Berlin"},
                      {"answer": "Spain - Madrid"}
                    ]
                  }]
                }
                """);

        List<String> errors = JsonValidator.validateQuestionsArray(root, Mode.MATCHING);
        assertTrue(errors.isEmpty(), "Valid MATCHING should have no errors: " + errors);
    }

    @Test
    @DisplayName("Should reject missing questions field")
    void shouldRejectMissingQuestions() throws Exception {
        JsonNode root = parse("""
                {"data": []}
                """);

        List<String> errors = JsonValidator.validateQuestionsArray(root, Mode.ONE_CORRECT);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("'questions'"));
    }

    @Test
    @DisplayName("Should reject non-array questions")
    void shouldRejectNonArrayQuestions() throws Exception {
        JsonNode root = parse("""
                {"questions": "not an array"}
                """);

        List<String> errors = JsonValidator.validateQuestionsArray(root, Mode.ONE_CORRECT);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("array"));
    }

    @Test
    @DisplayName("Should reject empty questions array")
    void shouldRejectEmptyQuestionsArray() throws Exception {
        JsonNode root = parse("""
                {"questions": []}
                """);

        List<String> errors = JsonValidator.validateQuestionsArray(root, Mode.ONE_CORRECT);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("empty"));
    }

    @Test
    @DisplayName("Should reject ONE_CORRECT with missing question text")
    void shouldRejectMissingQuestionText() throws Exception {
        JsonNode root = parse("""
                {
                  "questions": [{
                    "answers": [{"answer": "a", "right": true}]
                  }]
                }
                """);

        List<String> errors = JsonValidator.validateQuestionsArray(root, Mode.ONE_CORRECT);
        assertTrue(errors.stream().anyMatch(e -> e.contains("missing or invalid 'question' field")));
    }

    @Test
    @DisplayName("Should reject ONE_CORRECT with multiple right answers")
    void shouldRejectMultipleRightAnswers() throws Exception {
        JsonNode root = parse("""
                {
                  "questions": [{
                    "question": "Test?",
                    "answers": [
                      {"answer": "a", "right": true},
                      {"answer": "b", "right": true},
                      {"answer": "c", "right": false}
                    ]
                  }]
                }
                """);

        List<String> errors = JsonValidator.validateQuestionsArray(root, Mode.ONE_CORRECT);
        assertTrue(errors.stream().anyMatch(e -> e.contains("exactly one answer must have right=true, found 2")));
    }

    @Test
    @DisplayName("Should reject ONE_CORRECT with no right answer")
    void shouldRejectNoRightAnswer() throws Exception {
        JsonNode root = parse("""
                {
                  "questions": [{
                    "question": "Test?",
                    "answers": [
                      {"answer": "a", "right": false},
                      {"answer": "b", "right": false}
                    ]
                  }]
                }
                """);

        List<String> errors = JsonValidator.validateQuestionsArray(root, Mode.ONE_CORRECT);
        assertTrue(errors.stream().anyMatch(e -> e.contains("exactly one answer must have right=true, found 0")));
    }

    @Test
    @DisplayName("Should reject ONE_CORRECT with missing answer text")
    void shouldRejectMissingAnswerText() throws Exception {
        JsonNode root = parse("""
                {
                  "questions": [{
                    "question": "Test?",
                    "answers": [{"right": true}]
                  }]
                }
                """);

        List<String> errors = JsonValidator.validateQuestionsArray(root, Mode.ONE_CORRECT);
        assertTrue(errors.stream().anyMatch(e -> e.contains("missing or invalid 'answer'")));
    }

    @Test
    @DisplayName("Should reject ORDERING with only 1 answer")
    void shouldRejectOrderingWithOneAnswer() throws Exception {
        JsonNode root = parse("""
                {
                  "questions": [{
                    "question": "Sort:",
                    "answers": [{"answer": "only"}]
                  }]
                }
                """);

        List<String> errors = JsonValidator.validateQuestionsArray(root, Mode.ORDERING);
        assertTrue(errors.stream().anyMatch(e -> e.contains("at least 3")));
    }

    @Test
    @DisplayName("Should reject MATCHING with missing separator")
    void shouldRejectMatchingWithoutSeparator() throws Exception {
        JsonNode root = parse("""
                {
                  "questions": [{
                    "question": "Match:",
                    "answers": [
                      {"answer": "FranceParis"},
                      {"answer": "GermanyBerlin"},
                      {"answer": "SpainMadrid"}
                    ]
                  }]
                }
                """);

        List<String> errors = JsonValidator.validateQuestionsArray(root, Mode.MATCHING);
        assertTrue(errors.stream().anyMatch(e -> e.contains("' - '")));
    }

    @Test
    @DisplayName("Should reject MATCHING with only 1 answer")
    void shouldRejectMatchingWithOneAnswer() throws Exception {
        JsonNode root = parse("""
                {
                  "questions": [{
                    "question": "Match:",
                    "answers": [{"answer": "A - B"}]
                  }]
                }
                """);

        List<String> errors = JsonValidator.validateQuestionsArray(root, Mode.MATCHING);
        assertTrue(errors.stream().anyMatch(e -> e.contains("at least 3")));
    }

    @Test
    @DisplayName("Should handle multiple questions with mixed validity")
    void shouldHandleMultipleQuestions() throws Exception {
        JsonNode root = parse("""
                {
                  "questions": [
                    {
                      "question": "Valid?",
                      "answers": [{"answer": "yes", "right": true}]
                    },
                    {
                      "answers": [{"answer": "no", "right": true}]
                    }
                  ]
                }
                """);

        List<String> errors = JsonValidator.validateQuestionsArray(root, Mode.ONE_CORRECT);
        assertFalse(errors.isEmpty(), "Should have errors for second question");
        assertTrue(errors.stream().anyMatch(e -> e.contains("questions[1]")));
    }
}
