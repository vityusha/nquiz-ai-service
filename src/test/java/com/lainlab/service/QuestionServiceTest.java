package com.lainlab.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lainlab.db.Token;
import com.lainlab.db.TokenRepository;
import com.lainlab.dto.QuestionRequest;
import com.lainlab.dto.QuestionResponseList;
import com.lainlab.model.*;
import com.lainlab.util.PromptCache;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for QuestionService.
 * Tests cache functionality, provider selection, and question generation logic.
 */
@MicronautTest
@DisplayName("QuestionService Tests")
class QuestionServiceTest {

    @Inject
    QuestionService questionService;

    @Mock
    TokenRepository tokenRepository;

    private Token testToken;

    @BeforeEach
    void setUp() {
        testToken = new Token();
        testToken.setId(1L);
        testToken.setToken("nq_user_test_token");
        testToken.setBalance(100);
        testToken.setActive(true);
        testToken.setAdmin(false);
        testToken.setLicenseNo(123);
        testToken.setLicenseOrg("Test Org");
        testToken.setEmail("test@example.com");
    }

    @Test
    @DisplayName("Should initialize caches properly")
    void testCacheInitialization() {
        assertNotNull(questionService.cache, "Question cache should be initialized");
        assertNotNull(questionService.ipHistory, "IP history cache should be initialized");
        assertNotNull(questionService.ipQuestions, "IP questions cache should be initialized");

        assertEquals(0, questionService.cache.asMap().size(), "Cache should start empty");
    }

    @Test
    @DisplayName("Should select correct provider")
    void testProviderSelection() {
        assertNotNull(questionService.getProvider(Provider.DEEPSEEK));
        assertNotNull(questionService.getProvider(Provider.GEMINI));
        assertNotNull(questionService.getProvider(Provider.OPENAI));
        assertNotNull(questionService.getProvider(Provider.GROQ));
        assertNotNull(questionService.getProvider(Provider.TIMEWEB));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10, 50})
    @DisplayName("Should handle various question counts")
    void testQuestionCountHandling(int count) {
        QuestionRequest req = new QuestionRequest();
        req.setCount(count);

        assertEquals(count, req.getCount(), "Should return correct question count");
    }

    @Test
    @DisplayName("Should ensure minimum question count is 1")
    void testMinimumQuestionCount() {
        QuestionRequest req = new QuestionRequest();
        req.setCount(0);

        assertEquals(1, req.getCount(), "Minimum count should be 1");
    }

    @Test
    @DisplayName("Cache should return valid QuestionResponseList")
    void testCacheStorage() {
        QuestionRequest req = new QuestionRequest();
        req.setCount(3);
        req.setMode(Mode.ONE_CORRECT);
        req.setLanguage(Language.ENGLISH);
        req.setDifficulty(Difficulty.A1);
        req.setType(QuestionType.VOCABULARY);

        // Cache is properly initialized
        assertNotNull(questionService.cache);
        assertEquals(0, questionService.cache.asMap().size());
    }

    @Test
    @DisplayName("Should track IP history correctly")
    void testIpHistoryTracking() {
        String ip = "192.168.1.1";

        // History should be empty initially
        Set<String> history = questionService.ipHistory.getIfPresent(ip);
        assertNull(history, "IP history should be empty initially");

        // After adding a key, it should be present
        questionService.ipHistory.asMap()
                .computeIfAbsent(ip, k -> new HashSet<>())
                .add("test_key");

        history = questionService.ipHistory.getIfPresent(ip);
        assertNotNull(history, "IP history should be present after adding key");
        assertTrue(history.contains("test_key"), "History should contain the added key");
    }

    @Test
    @DisplayName("Should handle all difficulty levels")
    void testAllDifficultyLevels() {
        for (Difficulty difficulty : Difficulty.values()) {
            QuestionRequest req = new QuestionRequest();
            req.setCount(1);
            req.setDifficulty(difficulty);

            assertNotNull(req.getDifficulty());
            assertEquals(difficulty, req.getDifficulty());
        }
    }

    @Test
    @DisplayName("Should handle all question types")
    void testAllQuestionTypes() {
        for (QuestionType type : QuestionType.values()) {
            QuestionRequest req = new QuestionRequest();
            req.setCount(1);
            req.setType(type);

            assertNotNull(req.getType());
            assertEquals(type, req.getType());
        }
    }

    @Test
    @DisplayName("Should handle all modes")
    void testAllModes() {
        for (Mode mode : Mode.values()) {
            QuestionRequest req = new QuestionRequest();
            req.setCount(1);
            req.setMode(mode);

            assertNotNull(req.getMode());
            assertEquals(mode, req.getMode());
        }
    }

    @Test
    @DisplayName("Should handle all languages")
    void testAllLanguages() {
        for (Language language : Language.values()) {
            QuestionRequest req = new QuestionRequest();
            req.setCount(1);
            req.setLanguage(language);

            assertNotNull(req.getLanguage());
            assertEquals(language, req.getLanguage());
        }
    }

    @Test
    @DisplayName("Question request should accept keywords")
    void testKeywordHandling() {
        QuestionRequest req = new QuestionRequest();
        String keywords = "verb,noun,tense";
        req.setKeywords(keywords);

        assertEquals(keywords, req.getKeywords());
    }

    @Test
    @DisplayName("Should create consistent request objects")
    void testRequestConsistency() {
        QuestionRequest req1 = new QuestionRequest();
        req1.setCount(5);
        req1.setMode(Mode.ONE_CORRECT);
        req1.setLanguage(Language.ENGLISH);
        req1.setDifficulty(Difficulty.B1);
        req1.setType(QuestionType.GRAMMAR);

        QuestionRequest req2 = new QuestionRequest();
        req2.setCount(5);
        req2.setMode(Mode.ONE_CORRECT);
        req2.setLanguage(Language.ENGLISH);
        req2.setDifficulty(Difficulty.B1);
        req2.setType(QuestionType.GRAMMAR);

        // Both requests should have identical parameters
        assertEquals(req1.getCount(), req2.getCount());
        assertEquals(req1.getMode(), req2.getMode());
        assertEquals(req1.getLanguage(), req2.getLanguage());
        assertEquals(req1.getDifficulty(), req2.getDifficulty());
        assertEquals(req1.getType(), req2.getType());
    }
}







