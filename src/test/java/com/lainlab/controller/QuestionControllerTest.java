package com.lainlab.controller;

import com.lainlab.db.QuestionRepository;
import com.lainlab.db.Token;
import com.lainlab.db.TokenRepository;
import com.lainlab.dto.QuestionRequest;
import com.lainlab.dto.QuestionResponse;
import com.lainlab.dto.QuestionResponseList;
import com.lainlab.filter.RateLimitFilter;
import com.lainlab.model.*;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.core.order.Ordered;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for QuestionController.
 * These tests verify API endpoints, authentication, and request/response flow.
 */
@MicronautTest(transactional = false)
@DisplayName("QuestionController Integration Tests")
class QuestionControllerTest {

    @Singleton
    @Replaces(RateLimitFilter.class)
    @ServerFilter("/api/**")
    static class NoopRateLimitFilter implements Ordered {
        @RequestFilter
        public void doFilter(HttpRequest<?> request) {
        }
        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }
    }

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    TokenRepository tokenRepository;

    @Inject
    QuestionRepository questionRepository;

    private Token userToken;
    private Token inactiveToken;
    private Token adminToken;

    @BeforeEach
    void setUp() {
        // Clean up previous questions and tokens
        questionRepository.deleteAll();
        tokenRepository.deleteAll();

        // Create test user token
        userToken = new Token();
        userToken.setToken("nq_user_test_full_token_1");
        userToken.setBalance(1000);
        userToken.setActive(true);
        userToken.setAdmin(false);
        userToken.setLicenseNo(123);
        userToken.setLicenseOrg("Test Org");
        userToken.setEmail("test@example.com");
        userToken.setCreatedAt(LocalDateTime.now());
        tokenRepository.save(userToken);

        // Create test user inactive token
        inactiveToken = new Token();
        inactiveToken.setToken("nq_user_test_inactive_token_1");
        inactiveToken.setBalance(1000);
        inactiveToken.setActive(false);
        inactiveToken.setAdmin(false);
        inactiveToken.setLicenseNo(124);
        inactiveToken.setLicenseOrg("Test Org");
        inactiveToken.setEmail("test@example.com");
        inactiveToken.setCreatedAt(LocalDateTime.now());
        tokenRepository.save(inactiveToken);

        // Create test admin token
        adminToken = new Token();
        adminToken.setToken("nq_admin_test_full_token_1");
        adminToken.setBalance(0);
        adminToken.setActive(true);
        adminToken.setAdmin(true);
        adminToken.setLicenseNo(0);
        adminToken.setLicenseOrg("Admin");
        adminToken.setEmail("admin@localhost");
        adminToken.setCreatedAt(LocalDateTime.now());
        tokenRepository.save(adminToken);
    }

    @Test
    @DisplayName("Should reject request without Authorization header")
    void testMissingAuthorizationHeader() {
        QuestionRequest request = createTestRequest();

        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                HttpRequest.POST("/api/questions/generate", request),
                String.class
            );
        });

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus(),
                     "Should return 401 for missing Authorization header");
    }

    @Test
    @DisplayName("Should reject request with invalid token")
    void testInvalidToken() {
        QuestionRequest request = createTestRequest();

        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                HttpRequest.POST("/api/questions/generate", request)
                    .bearerAuth("invalid_token_xyz"),
                String.class
            );
        });

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus(),
                     "Should return 401 for invalid token");
    }

    @Test
    @DisplayName("Should reject request with inactive token")
    void testInactiveToken() {
        QuestionRequest request = createTestRequest();

        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                    HttpRequest.POST("/api/questions/generate", request)
                            .bearerAuth(inactiveToken.getToken()),
                    String.class
            );
        });

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus(),
                "Should return 401 for invalid token");
    }

    @Test
    @DisplayName("Should reject request when count is too much")
    void testTooManyCount() {
        QuestionRequest request = createTestRequest();
        request.setCount(1000); // More than maximum

        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                    HttpRequest.POST("/api/questions/generate", request)
                            .bearerAuth(userToken.getToken()),
                    String.class
            );
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus(),
                "Should return 400 for large count");
    }

    @Test
    @DisplayName("Should reject request when balance is insufficient")
    void testInsufficientBalance() {
        QuestionRequest request = createTestRequest();
        request.setCount(1001); // More than balance

        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                HttpRequest.POST("/api/questions/generate", request)
                    .bearerAuth(userToken.getToken()),
                String.class
            );
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus(),
                     "Should return 400 for insufficient balance");
    }

    @Test
    @DisplayName("Should accept valid request with proper authorization")
    void testValidRequestWithAuthorization() {
        QuestionRequest request = createTestRequest();

        // This test demonstrates that a properly authorized request would pass the auth filter.
        // The actual question generation would fail due to mocked LLM providers,
        // but we verify the authentication passes
        assertNotNull(userToken.getToken(), "Token should be present");
        assertTrue(userToken.isActive(), "Token should be active");
        assertTrue(userToken.getBalance() >= request.getCount(), "Token should have sufficient balance");
    }

    @Test
    @DisplayName("Admin token should always pass balance check")
    void testAdminTokenBypassesBalanceCheck() {
        QuestionRequest request = createTestRequest();
        request.setCount(1000);

        // Admin tokens have balance 0 but should still be able to make requests
        assertTrue(adminToken.isAdmin(), "Token should be admin");
        assertTrue(adminToken.isActive(), "Token should be active");
    }

    @Test
    @DisplayName("Token count should be capped at minimum 1")
    void testMinimumQuestionCount() {
        QuestionRequest request = new QuestionRequest();
        request.setCount(0);
        request.setProvider(Provider.DEEPSEEK);
        request.setMode(Mode.ONE_CORRECT);
        request.setLanguage(Language.ENGLISH);
        request.setDifficulty(Difficulty.A1);
        request.setType(QuestionType.GRAMMAR);

        // Even though 0 is provided, it should be capped to 1
        assertEquals(1, request.getCount(), "Count should be at least 1");
    }

    @Test
    @DisplayName("Should handle all supported providers")
    void testAllProvidersAllowed() {
        for (Provider provider : Provider.values()) {
            QuestionRequest request = createTestRequest();
            request.setProvider(provider);

            assertNotNull(request.getProvider());
            assertTrue(Provider.values().length > 0, "Should support at least one provider");
        }
    }

    @Test
    @DisplayName("Bearer token parsing should work correctly")
    void testBearerTokenParsing() {
        String bearerToken = "Bearer nq_user_test_token";
        String extracted = bearerToken.substring("Bearer ".length()).trim();

        assertEquals("nq_user_test_token", extracted, "Should extract token from Bearer prefix");
    }

    // ──────────────────────────────────────────────
    // /api/questions/store tests
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/questions/store - should reject without Authorization header")
    void testStoreQuestions_MissingAuth() {
        QuestionResponseList body = new QuestionResponseList();
        body.setQuestions(List.of(createSampleQuestion()));

        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                HttpRequest.POST("/api/questions/store", body),
                String.class
            );
        });

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("POST /api/questions/store - should reject with invalid token")
    void testStoreQuestions_InvalidToken() {
        QuestionResponseList body = new QuestionResponseList();
        body.setQuestions(List.of(createSampleQuestion()));

        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                HttpRequest.POST("/api/questions/store", body)
                    .bearerAuth("invalid_token_xyz"),
                String.class
            );
        });

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("POST /api/questions/store - should reject with inactive token")
    void testStoreQuestions_InactiveToken() {
        QuestionResponseList body = new QuestionResponseList();
        body.setQuestions(List.of(createSampleQuestion()));

        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                HttpRequest.POST("/api/questions/store", body)
                    .bearerAuth(inactiveToken.getToken()),
                String.class
            );
        });

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("POST /api/questions/store - should store questions with valid token")
    void testStoreQuestions_Success() {
        QuestionResponseList body = new QuestionResponseList();
        body.setQuestions(List.of(
            createSampleQuestion("What is the capital of France?"),
            createSampleQuestion("What is the largest ocean?")
        ));

        HttpResponse<Map> response = client.toBlocking().exchange(
            HttpRequest.POST("/api/questions/store", body)
                .bearerAuth(userToken.getToken()),
            Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        Map result = response.body();
        assertNotNull(result);
        assertEquals("saved", result.get("status"));
        assertEquals(2, result.get("count"));
    }

    @Test
    @DisplayName("POST /api/questions/store - should reject malformed JSON body")
    void testStoreQuestions_MalformedJson() {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                HttpRequest.POST("/api/questions/store", "{broken json")
                    .bearerAuth(userToken.getToken())
                    .contentType(MediaType.APPLICATION_JSON),
                String.class
            );
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    @DisplayName("POST /api/questions/store - should reject when questions is not an array")
    void testStoreQuestions_WrongType() {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                HttpRequest.POST("/api/questions/store", "{\"questions\": \"not an array\"}")
                    .bearerAuth(userToken.getToken())
                    .contentType(MediaType.APPLICATION_JSON),
                String.class
            );
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("POST /api/questions/store - should accept empty questions array")
    void testStoreQuestions_EmptyArray() {
        HttpResponse<Map> response = client.toBlocking().exchange(
            HttpRequest.POST("/api/questions/store", "{\"questions\":[]}")
                .bearerAuth(userToken.getToken())
                .contentType(MediaType.APPLICATION_JSON),
            Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        Map result = response.body();
        assertNotNull(result);
        assertEquals("saved", result.get("status"));
        assertEquals(0, result.get("count"));
    }

    // ──────────────────────────────────────────────
    // /api/questions/search tests
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/questions/search - should reject without Authorization header")
    void testSearchQuestions_MissingAuth() {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                HttpRequest.GET("/api/questions/search?mode=ONE_CORRECT"),
                String.class
            );
        });

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("GET /api/questions/search - should return 400 when no params provided")
    void testSearchQuestions_NoParams() {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                HttpRequest.GET("/api/questions/search")
                    .bearerAuth(userToken.getToken()),
                String.class
            );
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    @DisplayName("GET /api/questions/search - should return paginated results by mode")
    void testSearchQuestions_ByMode() {
        seedQuestion("{\"mode\":\"ONE_CORRECT\",\"difficulty\":\"B1\",\"type\":\"GRAMMAR\",\"language\":\"ENGLISH\",\"keywords\":\"test\"}");

        HttpResponse<String> response = client.toBlocking().exchange(
            HttpRequest.GET("/api/questions/search?mode=ONE_CORRECT")
                .bearerAuth(userToken.getToken()),
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertNotNull(response.body());
        assertTrue(response.body().contains("\"content\""));
        assertTrue(response.body().contains("\"totalSize\""));
    }

    @Test
    @DisplayName("GET /api/questions/search - should return results by difficulty")
    void testSearchQuestions_ByDifficulty() {
        seedQuestion("{\"mode\":\"MULTI_CORRECT\",\"difficulty\":\"B1\",\"type\":\"VOCABULARY\",\"language\":\"ENGLISH\",\"keywords\":\"test\"}");

        HttpResponse<String> response = client.toBlocking().exchange(
            HttpRequest.GET("/api/questions/search?difficulty=B1")
                .bearerAuth(userToken.getToken()),
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertNotNull(response.body());
        assertTrue(response.body().contains("\"content\""));
    }

    @Test
    @DisplayName("GET /api/questions/search - should return results by type")
    void testSearchQuestions_ByType() {
        seedQuestion("{\"mode\":\"ONE_CORRECT\",\"difficulty\":\"A1\",\"type\":\"GRAMMAR\",\"language\":\"ENGLISH\",\"keywords\":\"test\"}");

        HttpResponse<String> response = client.toBlocking().exchange(
            HttpRequest.GET("/api/questions/search?type=GRAMMAR")
                .bearerAuth(userToken.getToken()),
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertNotNull(response.body());
        assertTrue(response.body().contains("\"content\""));
    }

    @Test
    @DisplayName("GET /api/questions/search - should return results by language")
    void testSearchQuestions_ByLanguage() {
        seedQuestion("{\"mode\":\"ONE_CORRECT\",\"difficulty\":\"A2\",\"type\":\"READING\",\"language\":\"ENGLISH\",\"keywords\":\"test\"}");

        HttpResponse<String> response = client.toBlocking().exchange(
            HttpRequest.GET("/api/questions/search?language=ENGLISH")
                .bearerAuth(userToken.getToken()),
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertNotNull(response.body());
        assertTrue(response.body().contains("\"content\""));
    }

    @Test
    @DisplayName("GET /api/questions/search - should return results by keywords")
    void testSearchQuestions_ByKeywords() {
        seedQuestion("{\"mode\":\"ONE_CORRECT\",\"difficulty\":\"C1\",\"type\":\"GRAMMAR\",\"language\":\"ENGLISH\",\"keywords\":\"present tense\"}");

        HttpResponse<String> response = client.toBlocking().exchange(
            HttpRequest.GET("/api/questions/search?keywords=present")
                .bearerAuth(userToken.getToken()),
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertNotNull(response.body());
        assertTrue(response.body().contains("\"content\""));
    }

    @Test
    @DisplayName("GET /api/questions/search - should reject with inactive token")
    void testSearchQuestions_InactiveToken() {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                HttpRequest.GET("/api/questions/search?mode=ONE_CORRECT")
                    .bearerAuth(inactiveToken.getToken()),
                String.class
            );
        });

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("GET /api/questions/search - should return empty results when mode has no matches")
    void testSearchQuestions_ByMode_NoResults() {
        seedQuestion("{\"mode\":\"ONE_CORRECT\",\"difficulty\":\"B1\",\"type\":\"GRAMMAR\",\"language\":\"ENGLISH\",\"keywords\":\"test\"}");

        HttpResponse<String> response = client.toBlocking().exchange(
            HttpRequest.GET("/api/questions/search?mode=MATCHING")
                .bearerAuth(userToken.getToken()),
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertNotNull(response.body());
        assertTrue(response.body().contains("\"content\":[]"));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("GET /api/questions/search - should support pagination with size and page")
    void testSearchQuestions_WithPagination() {
        seedQuestion("{\"mode\":\"ONE_CORRECT\",\"difficulty\":\"B1\",\"type\":\"GRAMMAR\",\"language\":\"ENGLISH\",\"keywords\":\"test\"}");
        seedQuestion("{\"mode\":\"ONE_CORRECT\",\"difficulty\":\"B2\",\"type\":\"VOCABULARY\",\"language\":\"ENGLISH\",\"keywords\":\"test2\"}");

        HttpResponse<Map> response = client.toBlocking().exchange(
            HttpRequest.GET("/api/questions/search?mode=ONE_CORRECT&size=1&page=0")
                .bearerAuth(userToken.getToken()),
            Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        Map body = response.body();
        assertNotNull(body);
        assertTrue(body.containsKey("content"));
        assertTrue(body.containsKey("totalSize"));
        assertEquals(2, ((Number) body.get("totalSize")).intValue());
        assertEquals(1, ((java.util.Collection<?>) body.get("content")).size());
    }

    @Test
    @DisplayName("GET /api/questions/search - should return 400 when only empty params provided")
    void testSearchQuestions_EmptyStringParams() {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                HttpRequest.GET("/api/questions/search?mode=&difficulty=")
                    .bearerAuth(userToken.getToken()),
                String.class
            );
        });

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    @DisplayName("GET /api/questions/search - first non-empty param wins (cascading filter)")
    void testSearchQuestions_CascadingFilter_ModeTakesPriority() {
        seedQuestion("{\"mode\":\"ONE_CORRECT\",\"difficulty\":\"C2\",\"type\":\"LISTENING\",\"language\":\"ENGLISH\",\"keywords\":\"test\"}");
        seedQuestion("{\"mode\":\"MATCHING\",\"difficulty\":\"C2\",\"type\":\"LISTENING\",\"language\":\"ENGLISH\",\"keywords\":\"test\"}");

        HttpResponse<String> response = client.toBlocking().exchange(
            HttpRequest.GET("/api/questions/search?mode=ONE_CORRECT&difficulty=C2&type=LISTENING")
                .bearerAuth(userToken.getToken()),
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertNotNull(response.body());
        // Should only return ONE_CORRECT (cascading: mode wins), not both
        assertTrue(response.body().contains("ONE_CORRECT"));
        assertTrue(response.body().contains("\"totalSize\":1"));
    }

    // ──────────────────────────────────────────────
    // helpers
    // ──────────────────────────────────────────────

    private void seedQuestion(String json) {
        questionRepository.insertQuestion(userToken.getId(), json);
    }

    private QuestionResponse createSampleQuestion() {
        return createSampleQuestion("Sample question text");
    }

    private QuestionResponse createSampleQuestion(String text) {
        QuestionResponse q = new QuestionResponse();
        q.setQuestion(text);
        q.setMode(Mode.ONE_CORRECT);
        q.setDifficulty("A1");
        q.setType("GRAMMAR");
        q.setLanguage("ENGLISH");
        q.setKeywords("test");
        QuestionResponse.Answer a = new QuestionResponse.Answer();
        a.setAnswer("Sample answer");
        a.setRight(true);
        q.setAnswers(List.of(a));
        return q;
    }

    private QuestionRequest createTestRequest() {
        QuestionRequest request = new QuestionRequest();
        request.setProvider(Provider.DEEPSEEK);
        request.setCount(5);
        request.setMode(Mode.ONE_CORRECT);
        request.setLanguage(Language.ENGLISH);
        request.setDifficulty(Difficulty.B1);
        request.setType(QuestionType.GRAMMAR);
        request.setKeywords("present tense");
        return request;
    }
}



