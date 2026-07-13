package com.lainlab.db;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@DisplayName("QuestionRepository tests")
class QuestionRepositoryTest {

    @Inject
    TokenRepository tokenRepository;

    @Inject
    QuestionRepository questionRepository;

    private Token testToken;

    @BeforeEach
    void setUp() {
        // Delete questions first to avoid FK violations, then tokens
        questionRepository.deleteAll();
        tokenRepository.deleteAll();

        testToken = new Token();
        testToken.setToken("nq_user_question_test");
        testToken.setLicenseNo(999);
        testToken.setLicenseOrg("QuestionTestOrg");
        testToken.setEmail("question@test.local");
        testToken.setBalance(10);
        testToken.setActive(true);
        testToken.setAdmin(false);
        testToken.setCreatedAt(Instant.now().atOffset(java.time.ZoneOffset.UTC).toLocalDateTime());
        tokenRepository.save(testToken);
    }

    @Test
    @DisplayName("Should insert a question and retrieve it")
    void shouldInsertAndRetrieveQuestion() {
        String questionText = "{\"question\":\"What is 2+2?\"}";
        questionRepository.insertQuestion(testToken.getId(), questionText);

        List<QuestionEntity> all = questionRepository.findAll();
        assertEquals(1, all.size(), "Should have exactly one question");
        QuestionEntity found = all.get(0);
        assertEquals(questionText, found.getQuestion());
        assertEquals(testToken.getId(), found.getTokenId());
    }

    @Test
    @DisplayName("Should not insert duplicate questions (INSERT OR IGNORE)")
    void shouldNotInsertDuplicateQuestions() {
        String questionText = "{\"question\":\"What is the capital of France?\"}";

        // First insert
        questionRepository.insertQuestion(testToken.getId(), questionText);

        // Second insert with same question text
        questionRepository.insertQuestion(testToken.getId(), questionText);

        // Count should be 1 — the duplicate was ignored
        long count = questionRepository.count();
        assertEquals(1, count, "Duplicate question should not be inserted");
    }

    @Test
    @DisplayName("Should allow different questions with same token")
    void shouldAllowDifferentQuestionsWithSameToken() {
        String question1 = "{\"question\":\"Question A\"}";
        String question2 = "{\"question\":\"Question B\"}";

        questionRepository.insertQuestion(testToken.getId(), question1);
        questionRepository.insertQuestion(testToken.getId(), question2);

        long count = questionRepository.count();
        assertEquals(2, count, "Different questions should both be inserted");
    }
}
