package com.lainlab.controller;

import com.lainlab.db.QuestionRepository;
import com.lainlab.db.Token;
import com.lainlab.db.TokenRepository;
import com.lainlab.filter.RateLimitFilter;
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
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@DisplayName("QuestionSearchController Tests")
class QuestionSearchControllerTest {

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

    @BeforeEach
    void setUp() {
        questionRepository.deleteAll();
        tokenRepository.deleteAll();

        userToken = new Token();
        userToken.setToken("nq_user_test_search_token_1");
        userToken.setBalance(1000);
        userToken.setActive(true);
        userToken.setAdmin(false);
        userToken.setLicenseNo(200);
        userToken.setLicenseOrg("Test Org");
        userToken.setEmail("search-test@example.com");
        userToken.setCreatedAt(LocalDateTime.now());
        tokenRepository.save(userToken);
    }

    @Test
    @DisplayName("GET /search - should return HTML with 200 when no filters")
    void testSearchPage_NoFilters() {
        HttpResponse<String> response = client.toBlocking().exchange(
            HttpRequest.GET("/search"),
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertTrue(response.getContentType().isPresent());
        assertTrue(response.getContentType().get().toString().contains("text/html"));
        assertNotNull(response.body());
        assertTrue(response.body().contains("Question Search"));
        assertTrue(response.body().contains("NQuiz"));
    }

    @Test
    @DisplayName("GET /search - should return empty state when no questions match")
    void testSearchPage_NoResults() {
        seedQuestion("{\"mode\":\"ONE_CORRECT\",\"difficulty\":\"B1\",\"type\":\"GRAMMAR\",\"language\":\"ENGLISH\",\"keywords\":\"test\"}");

        HttpResponse<String> response = client.toBlocking().exchange(
            HttpRequest.GET("/search?mode=MATCHING"),
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertNotNull(response.body());
        assertTrue(response.body().contains("No questions found"));
    }

    @Test
    @DisplayName("GET /search - should return results when mode filter matches")
    void testSearchPage_WithModeFilter() {
        seedQuestion("{\"mode\":\"ONE_CORRECT\",\"difficulty\":\"B1\",\"type\":\"GRAMMAR\",\"language\":\"ENGLISH\",\"keywords\":\"test\"}");

        HttpResponse<String> response = client.toBlocking().exchange(
            HttpRequest.GET("/search?mode=ONE_CORRECT"),
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertNotNull(response.body());
        assertTrue(response.body().contains("1 found"));
        assertTrue(response.body().contains("ONE_CORRECT"));
    }

    @Test
    @DisplayName("GET /search - should return results with combined filters")
    void testSearchPage_CombinedFilters() {
        seedQuestion("{\"mode\":\"ONE_CORRECT\",\"difficulty\":\"B1\",\"type\":\"GRAMMAR\",\"language\":\"ENGLISH\",\"keywords\":\"summer\"}");
        seedQuestion("{\"mode\":\"ONE_CORRECT\",\"difficulty\":\"B2\",\"type\":\"VOCABULARY\",\"language\":\"ENGLISH\",\"keywords\":\"winter\"}");
        seedQuestion("{\"mode\":\"MATCHING\",\"difficulty\":\"B1\",\"type\":\"GRAMMAR\",\"language\":\"ENGLISH\",\"keywords\":\"test\"}");

        HttpResponse<String> response = client.toBlocking().exchange(
            HttpRequest.GET("/search?mode=ONE_CORRECT&difficulty=B1"),
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertNotNull(response.body());
        assertTrue(response.body().contains("1 found"));
        assertTrue(response.body().contains("ONE_CORRECT"));
        assertTrue(response.body().contains("B1"));
    }

    @Test
    @DisplayName("GET /search - should return results with keywords filter (LIKE search)")
    void testSearchPage_KeywordsFilter() {
        seedQuestion("{\"mode\":\"ONE_CORRECT\",\"difficulty\":\"B1\",\"type\":\"GRAMMAR\",\"language\":\"ENGLISH\",\"keywords\":\"present tense\"}");
        seedQuestion("{\"mode\":\"ONE_CORRECT\",\"difficulty\":\"B1\",\"type\":\"GRAMMAR\",\"language\":\"ENGLISH\",\"keywords\":\"past simple\"}");

        HttpResponse<String> response = client.toBlocking().exchange(
            HttpRequest.GET("/search?keywords=present"),
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertNotNull(response.body());
        assertTrue(response.body().contains("1 found"));
        assertTrue(response.body().contains("present tense"));
    }

    @Test
    @DisplayName("GET /search - should populate filter dropdowns with distinct values")
    void testSearchPage_PopulatesFilters() {
        seedQuestion("{\"mode\":\"ONE_CORRECT\",\"difficulty\":\"B1\",\"type\":\"GRAMMAR\",\"language\":\"ENGLISH\",\"keywords\":\"test\"}");
        seedQuestion("{\"mode\":\"MATCHING\",\"difficulty\":\"B2\",\"type\":\"VOCABULARY\",\"language\":\"GERMAN\",\"keywords\":\"test\"}");

        HttpResponse<String> response = client.toBlocking().exchange(
            HttpRequest.GET("/search"),
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertNotNull(response.body());
        assertTrue(response.body().contains("ONE_CORRECT"));
        assertTrue(response.body().contains("MATCHING"));
        assertTrue(response.body().contains("B1"));
        assertTrue(response.body().contains("B2"));
    }

    @Test
    @DisplayName("GET /search - should respect page parameter")
    void testSearchPage_Pagination() {
        for (int i = 0; i < 25; i++) {
            seedQuestion("{\"mode\":\"ONE_CORRECT\",\"difficulty\":\"B1\",\"type\":\"GRAMMAR\",\"language\":\"ENGLISH\",\"keywords\":\"test\"}");
        }

        HttpResponse<String> response = client.toBlocking().exchange(
            HttpRequest.GET("/search?mode=ONE_CORRECT&page=1"),
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertNotNull(response.body());
        assertTrue(response.body().contains("Page 2 of 2"));
    }

    @Test
    @DisplayName("GET /search - should show total count in header")
    void testSearchPage_TotalCount() {
        seedQuestion("{\"mode\":\"ONE_CORRECT\",\"difficulty\":\"B1\",\"type\":\"GRAMMAR\",\"language\":\"ENGLISH\",\"keywords\":\"test\"}");
        seedQuestion("{\"mode\":\"ONE_CORRECT\",\"difficulty\":\"B1\",\"type\":\"GRAMMAR\",\"language\":\"ENGLISH\",\"keywords\":\"test\"}");

        HttpResponse<String> response = client.toBlocking().exchange(
            HttpRequest.GET("/search?mode=ONE_CORRECT"),
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertNotNull(response.body());
        assertTrue(response.body().contains("2 found"));
    }

    @Test
    @DisplayName("GET /search - should render correct HTML structure")
    void testSearchPage_HtmlStructure() {
        HttpResponse<String> response = client.toBlocking().exchange(
            HttpRequest.GET("/search"),
            String.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertNotNull(response.body());
        String html = response.body();
        assertTrue(html.contains("<title>Question Search - NQuiz</title>"));
        assertTrue(html.contains("id=\"searchForm\""));
        assertTrue(html.contains("id=\"resultsSection\""));
        assertTrue(html.contains("id=\"paginationContainer\""));
        assertTrue(html.contains("name=\"mode\""));
        assertTrue(html.contains("name=\"difficulty\""));
        assertTrue(html.contains("name=\"type\""));
        assertTrue(html.contains("name=\"language\""));
        assertTrue(html.contains("name=\"keywords\""));
    }

    // ──────────────────────────────────────────────
    // helpers
    // ──────────────────────────────────────────────

    private void seedQuestion(String json) {
        questionRepository.insertQuestion(userToken.getId(), json);
    }
}
