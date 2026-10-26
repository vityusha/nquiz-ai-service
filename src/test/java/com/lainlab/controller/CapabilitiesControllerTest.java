package com.lainlab.controller;

import com.lainlab.db.Token;
import com.lainlab.db.TokenRepository;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@DisplayName("CapabilitiesController Tests")
class CapabilitiesControllerTest {

    private static final String TOKEN = "sk_user_capabilities_test";

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    TokenRepository tokenRepository;

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
        Token token = new Token();
        token.setToken(TOKEN);
        token.setBalance(10);
        token.setActive(true);
        token.setAdmin(false);
        token.setLicenseNo(901);
        token.setLicenseOrg("Capabilities Test");
        token.setEmail("caps@test.com");
        token.setCreatedAt(LocalDateTime.now());
        tokenRepository.save(token);
    }

    private static HttpRequest<?> get(String uri) {
        return HttpRequest.GET(uri).bearerAuth(TOKEN);
    }

    @Test
    @DisplayName("Should return capabilities with default lang=en")
    void shouldReturnDefaultEnglishCapabilities() {
        var response = client.toBlocking().retrieve(
                get("/api/capabilities"), String.class);
        assertNotNull(response);
        assertTrue(response.contains("languages"));
        assertTrue(response.contains("questionTypes"));
        assertTrue(response.contains("difficulties"));
        assertTrue(response.contains("modes"));
        assertTrue(response.contains("providers"));
        assertTrue(response.contains("maxQuestions"));
    }

    @Test
    @DisplayName("Should return capabilities with lang=ru query param")
    void shouldReturnRussianCapabilities() {
        var response = client.toBlocking().retrieve(
                get("/api/capabilities?lang=ru"), String.class);
        assertNotNull(response);
        assertTrue(response.contains("languages"));
    }

    @Test
    @DisplayName("Should require authorization header")
    void shouldRequireAuth() {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(HttpRequest.GET("/api/capabilities"), String.class)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("Should return 402 JSON error for user token with zero balance")
    void shouldReturnPaymentRequiredOnZeroBalance() {
        Token token = tokenRepository.findByToken(TOKEN).orElseThrow();
        token.setBalance(0);
        tokenRepository.update(token);

        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(get("/api/capabilities"), String.class)
        );
        assertEquals(HttpStatus.PAYMENT_REQUIRED, ex.getStatus());
        String body = ex.getResponse().getBody(String.class).orElse("");
        assertTrue(ex.getResponse().getContentType().map(Object::toString).orElse("").contains("json"));
        assertTrue(body.contains("Insufficient balance"), body);
    }

    @Test
    @DisplayName("Should reject invalid token")
    void shouldRejectInvalidToken() {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(
                        HttpRequest.GET("/api/capabilities").bearerAuth("sk_user_invalid"), String.class)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("Should include all provider values")
    void shouldIncludeAllProviders() {
        var response = client.toBlocking().retrieve(
                get("/api/capabilities"), String.class);
        assertNotNull(response);
        assertTrue(response.contains("OPENAI"));
        assertTrue(response.contains("DEEPSEEK"));
        assertTrue(response.contains("GEMINI"));
    }

    @Test
    @DisplayName("Should include all difficulty levels (A1-C2)")
    void shouldIncludeAllDifficulties() {
        var response = client.toBlocking().retrieve(
                get("/api/capabilities"), String.class);
        assertNotNull(response);
        assertTrue(response.contains("A1"));
        assertTrue(response.contains("A2"));
        assertTrue(response.contains("B1"));
        assertTrue(response.contains("B2"));
        assertTrue(response.contains("C1"));
        assertTrue(response.contains("C2"));
    }

    @Test
    @DisplayName("Should include all question types")
    void shouldIncludeAllQuestionTypes() {
        var response = client.toBlocking().retrieve(
                get("/api/capabilities"), String.class);
        assertNotNull(response);
        assertTrue(response.contains("GRAMMAR"));
        assertTrue(response.contains("TENSES"));
        assertTrue(response.contains("ARTICLES"));
        assertTrue(response.contains("VOCABULARY"));
    }

    @Test
    @DisplayName("Should include all answer modes")
    void shouldIncludeAllModes() {
        var response = client.toBlocking().retrieve(
                get("/api/capabilities"), String.class);
        assertNotNull(response);
        assertTrue(response.contains("ONE_CORRECT"));
        assertTrue(response.contains("MULTI_CORRECT"));
        assertTrue(response.contains("ORDERING"));
        assertTrue(response.contains("MATCHING"));
    }
}
