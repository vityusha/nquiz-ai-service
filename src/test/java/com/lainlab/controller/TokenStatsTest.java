package com.lainlab.controller;

import com.lainlab.db.AiResponseLog;
import com.lainlab.db.AiResponseLogRepository;
import com.lainlab.db.QuestionRepository;
import com.lainlab.db.Token;
import com.lainlab.db.TokenRepository;
import com.lainlab.dto.QuestionRequest;
import com.lainlab.dto.QuestionResponseList;
import com.lainlab.model.Mode;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@DisplayName("GET /admin/tokens/stats Tests")
class TokenStatsTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    TokenRepository tokenRepository;

    @Inject
    QuestionRepository questionRepository;

    @Inject
    AiResponseLogRepository aiResponseLogRepository;

    private Token adminToken;
    private Token userTokenA;
    private Token userTokenB;

    @BeforeEach
    void setUp() {
        aiResponseLogRepository.deleteAll();
        questionRepository.deleteAll();
        tokenRepository.deleteAll();

        adminToken = new Token();
        adminToken.setToken("sk_admin_stats_test");
        adminToken.setBalance(0);
        adminToken.setActive(true);
        adminToken.setAdmin(true);
        adminToken.setLicenseNo(0);
        adminToken.setLicenseOrg("Admin");
        adminToken.setEmail("admin@localhost");
        adminToken.setCreatedAt(LocalDateTime.now());
        tokenRepository.save(adminToken);

        userTokenA = new Token();
        userTokenA.setToken("sk_user_stats_test_a");
        userTokenA.setBalance(1000);
        userTokenA.setActive(true);
        userTokenA.setAdmin(false);
        userTokenA.setLicenseNo(100);
        userTokenA.setLicenseOrg("Org A");
        userTokenA.setEmail("a@example.com");
        userTokenA.setCreatedAt(LocalDateTime.now());
        tokenRepository.save(userTokenA);

        userTokenB = new Token();
        userTokenB.setToken("sk_user_stats_test_b");
        userTokenB.setBalance(500);
        userTokenB.setActive(true);
        userTokenB.setAdmin(false);
        userTokenB.setLicenseNo(200);
        userTokenB.setLicenseOrg("Org B");
        userTokenB.setEmail("b@example.com");
        userTokenB.setCreatedAt(LocalDateTime.now());
        tokenRepository.save(userTokenB);
    }

    @Test
    @DisplayName("GET /admin/tokens/stats - should return 200 with all stats fields")
    void testStats_ReturnsAllFields() {
        HttpResponse<Map> response = client.toBlocking().exchange(
            HttpRequest.GET("/admin/tokens/stats")
                .bearerAuth(adminToken.getToken()),
            Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        Map body = response.body();
        assertNotNull(body);
        assertTrue(body.containsKey("totalActiveLicenseNo"));
        assertTrue(body.containsKey("totalAiRequests"));
        assertTrue(body.containsKey("totalQuestionsStored"));
        assertTrue(body.containsKey("licenses"));
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("GET /admin/tokens/stats - should return zero counts when no AI data")
    void testStats_EmptyData() {
        HttpResponse<Map> response = client.toBlocking().exchange(
            HttpRequest.GET("/admin/tokens/stats")
                .bearerAuth(adminToken.getToken()),
            Map.class
        );

        Map body = response.body();
        assertEquals(0, ((Number) body.get("totalActiveLicenseNo")).longValue());
        assertEquals(0, ((Number) body.get("totalAiRequests")).longValue());
        assertEquals(0, ((Number) body.get("totalQuestionsStored")).longValue());
        List<Map> licenses = (List<Map>) body.get("licenses");
        assertEquals(2, licenses.size());
        licenses.forEach(l -> {
            assertEquals(0L, ((Number) l.get("ai_requests")).longValue());
            assertEquals(0L, ((Number) l.get("questions_stored")).longValue());
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("GET /admin/tokens/stats - should count AI requests and questions correctly")
    void testStats_CountsAiRequestsAndQuestions() {
        seedAiLog(userTokenA.getId(), 3);
        seedAiLog(userTokenB.getId(), 2);
        questionRepository.insertQuestion(userTokenA.getId(), "{\"mode\":\"ONE_CORRECT\",\"difficulty\":\"B1\",\"type\":\"GRAMMAR\",\"language\":\"ENGLISH\",\"keywords\":\"a\"}");
        questionRepository.insertQuestion(userTokenA.getId(), "{\"mode\":\"ONE_CORRECT\",\"difficulty\":\"B2\",\"type\":\"VOCABULARY\",\"language\":\"ENGLISH\",\"keywords\":\"b\"}");
        questionRepository.insertQuestion(userTokenB.getId(), "{\"mode\":\"MATCHING\",\"difficulty\":\"B1\",\"type\":\"GRAMMAR\",\"language\":\"GERMAN\",\"keywords\":\"c\"}");

        HttpResponse<Map> response = client.toBlocking().exchange(
            HttpRequest.GET("/admin/tokens/stats")
                .bearerAuth(adminToken.getToken()),
            Map.class
        );

        Map body = response.body();
        assertEquals(2, ((Number) body.get("totalActiveLicenseNo")).longValue());
        assertEquals(5, ((Number) body.get("totalAiRequests")).longValue());
        assertEquals(3, ((Number) body.get("totalQuestionsStored")).longValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("GET /admin/tokens/stats - should include per-license breakdown")
    void testStats_PerLicenseBreakdown() {
        seedAiLog(userTokenA.getId(), 4);
        seedAiLog(userTokenB.getId(), 1);
        questionRepository.insertQuestion(userTokenA.getId(), "{\"mode\":\"ONE_CORRECT\",\"difficulty\":\"B1\",\"type\":\"GRAMMAR\",\"language\":\"ENGLISH\",\"keywords\":\"x\"}");
        questionRepository.insertQuestion(userTokenA.getId(), "{\"mode\":\"ONE_CORRECT\",\"difficulty\":\"B1\",\"type\":\"GRAMMAR\",\"language\":\"ENGLISH\",\"keywords\":\"y\"}");
        questionRepository.insertQuestion(userTokenB.getId(), "{\"mode\":\"MATCHING\",\"difficulty\":\"B2\",\"type\":\"VOCABULARY\",\"language\":\"GERMAN\",\"keywords\":\"z\"}");

        HttpResponse<Map> response = client.toBlocking().exchange(
            HttpRequest.GET("/admin/tokens/stats")
                .bearerAuth(adminToken.getToken()),
            Map.class
        );

        Map body = response.body();
        List<Map> licenses = (List<Map>) body.get("licenses");
        assertEquals(2, licenses.size());

        Map statsA = licenses.stream()
            .filter(l -> ((Number) l.get("license_no")).intValue() == 100)
            .findFirst().orElseThrow();
        assertEquals("Org A", statsA.get("license_org"));
        assertEquals(4L, ((Number) statsA.get("ai_requests")).longValue());
        assertEquals(2L, ((Number) statsA.get("questions_stored")).longValue());

        Map statsB = licenses.stream()
            .filter(l -> ((Number) l.get("license_no")).intValue() == 200)
            .findFirst().orElseThrow();
        assertEquals("Org B", statsB.get("license_org"));
        assertEquals(1L, ((Number) statsB.get("ai_requests")).longValue());
        assertEquals(1L, ((Number) statsB.get("questions_stored")).longValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("GET /admin/tokens/stats - should include tokens with zero activity")
    void testStats_IncludesInactiveTokens() {
        seedAiLog(userTokenA.getId(), 1);

        HttpResponse<Map> response = client.toBlocking().exchange(
            HttpRequest.GET("/admin/tokens/stats")
                .bearerAuth(adminToken.getToken()),
            Map.class
        );

        Map body = response.body();
        List<Map> licenses = (List<Map>) body.get("licenses");
        assertEquals(2, licenses.size());

        Map statsB = licenses.stream()
            .filter(l -> ((Number) l.get("license_no")).intValue() == 200)
            .findFirst().orElseThrow();
        assertEquals(0L, ((Number) statsB.get("ai_requests")).longValue());
        assertEquals(0L, ((Number) statsB.get("questions_stored")).longValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("GET /admin/tokens/stats - should exclude admin tokens from per-license stats")
    void testStats_ExcludesAdminFromLicenseStats() {
        seedAiLog(userTokenA.getId(), 1);

        HttpResponse<Map> response = client.toBlocking().exchange(
            HttpRequest.GET("/admin/tokens/stats")
                .bearerAuth(adminToken.getToken()),
            Map.class
        );

        Map body = response.body();
        List<Map> licenses = (List<Map>) body.get("licenses");
        licenses.forEach(l -> {
            int licenseNo = ((Number) l.get("license_no")).intValue();
            assertTrue(licenseNo > 0, "Admin tokens should not appear in per-license stats");
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("GET /admin/tokens/stats - should work with seeded admin token")
    void testStats_AdminTokenAuthWorks() {
        HttpResponse<Map> response = client.toBlocking().exchange(
            HttpRequest.GET("/admin/tokens/stats")
                .bearerAuth(adminToken.getToken()),
            Map.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        Map body = response.body();
        assertNotNull(body);
        assertTrue(body.containsKey("totalActiveLicenseNo"));
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("GET /admin/tokens/stats - should sort per-license stats by aiRequests descending")
    void testStats_SortedByAiRequestsDesc() {
        seedAiLog(userTokenA.getId(), 1);
        seedAiLog(userTokenB.getId(), 5);

        HttpResponse<Map> response = client.toBlocking().exchange(
            HttpRequest.GET("/admin/tokens/stats")
                .bearerAuth(adminToken.getToken()),
            Map.class
        );

        Map body = response.body();
        List<Map> licenses = (List<Map>) body.get("licenses");
        assertEquals(2, licenses.size());

        int firstLicenseNo = ((Number) licenses.get(0).get("license_no")).intValue();
        int secondLicenseNo = ((Number) licenses.get(1).get("license_no")).intValue();
        assertEquals(200, firstLicenseNo);
        assertEquals(100, secondLicenseNo);
    }

    // ──────────────────────────────────────────────
    // helpers
    // ──────────────────────────────────────────────

    private void seedAiLog(Long tokenId, int count) {
        QuestionRequest req = new QuestionRequest();
        req.setMode(Mode.ONE_CORRECT);
        req.setCount(1);

        QuestionResponseList resp = new QuestionResponseList();
        resp.setQuestions(List.of());

        for (int i = 0; i < count; i++) {
            AiResponseLog log = new AiResponseLog();
            log.setTokenId(tokenId);
            log.setIpAddress("127.0.0.1");
            log.setRequest(req);
            log.setResponse(resp);
            log.setCreatedAt(Instant.now());
            aiResponseLogRepository.save(log);
        }
    }
}
