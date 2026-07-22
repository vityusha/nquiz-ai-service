package com.lainlab.controller;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@DisplayName("CapabilitiesController Tests")
class CapabilitiesControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    @DisplayName("Should return capabilities with default lang=en")
    void shouldReturnDefaultEnglishCapabilities() {
        var response = client.toBlocking().retrieve(
                HttpRequest.GET("/api/capabilities"), String.class);
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
                HttpRequest.GET("/api/capabilities?lang=ru"), String.class);
        assertNotNull(response);
        assertTrue(response.contains("languages"));
    }

    @Test
    @DisplayName("Should not require authorization header")
    void shouldNotRequireAuth() {
        assertDoesNotThrow(() ->
                client.toBlocking().exchange(HttpRequest.GET("/api/capabilities"), String.class)
        );
    }

    @Test
    @DisplayName("Should include all provider values")
    void shouldIncludeAllProviders() {
        var response = client.toBlocking().retrieve(
                HttpRequest.GET("/api/capabilities"), String.class);
        assertNotNull(response);
        assertTrue(response.contains("OPENAI"));
        assertTrue(response.contains("DEEPSEEK"));
        assertTrue(response.contains("GEMINI"));
    }

    @Test
    @DisplayName("Should include all difficulty levels (A1-C2)")
    void shouldIncludeAllDifficulties() {
        var response = client.toBlocking().retrieve(
                HttpRequest.GET("/api/capabilities"), String.class);
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
                HttpRequest.GET("/api/capabilities"), String.class);
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
                HttpRequest.GET("/api/capabilities"), String.class);
        assertNotNull(response);
        assertTrue(response.contains("ONE_CORRECT"));
        assertTrue(response.contains("MULTI_CORRECT"));
        assertTrue(response.contains("ORDERING"));
        assertTrue(response.contains("MATCHING"));
    }
}
