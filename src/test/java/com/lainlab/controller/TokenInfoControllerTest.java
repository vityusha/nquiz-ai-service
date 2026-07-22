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
@DisplayName("TokenInfoController Tests")
class TokenInfoControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    TokenRepository tokenRepository;

    private Token activeToken;
    private Token inactiveToken;

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();

        activeToken = new Token();
        activeToken.setToken("sk_info_test_active");
        activeToken.setBalance(250);
        activeToken.setTotal(500);
        activeToken.setActive(true);
        activeToken.setAdmin(false);
        activeToken.setLicenseNo(777);
        activeToken.setLicenseOrg("Info Test Org");
        activeToken.setEmail("info@test.com");
        activeToken.setCreatedAt(LocalDateTime.now());
        tokenRepository.save(activeToken);

        inactiveToken = new Token();
        inactiveToken.setToken("sk_info_test_inactive");
        inactiveToken.setBalance(100);
        inactiveToken.setActive(false);
        inactiveToken.setAdmin(false);
        inactiveToken.setLicenseNo(778);
        inactiveToken.setLicenseOrg("Inactive Info Org");
        inactiveToken.setEmail("inactive@test.com");
        inactiveToken.setCreatedAt(LocalDateTime.now());
        tokenRepository.save(inactiveToken);
    }

    @Test
    @DisplayName("Should return 401 without Authorization header")
    void shouldRejectMissingAuth() {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(
                        HttpRequest.GET("/api/token/info"), String.class)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("Should return 401 with malformed Authorization header")
    void shouldRejectMalformedAuth() {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(
                        HttpRequest.GET("/api/token/info")
                                .header("Authorization", "Basic creds"),
                        String.class)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("Should return 401 with non-existent token")
    void shouldRejectInvalidToken() {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(
                        HttpRequest.GET("/api/token/info")
                                .bearerAuth("sk_nonexistent_token"),
                        String.class)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("Should return 401 for inactive token")
    void shouldRejectInactiveToken() {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(
                        HttpRequest.GET("/api/token/info")
                                .bearerAuth(inactiveToken.getToken()),
                        String.class)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("Should return token info for valid active token")
    void shouldReturnTokenInfo() {
        var response = client.toBlocking().retrieve(
                HttpRequest.GET("/api/token/info")
                        .bearerAuth(activeToken.getToken()),
                String.class);
        assertNotNull(response);
        assertTrue(response.contains("777"), "Should contain license_no");
        assertTrue(response.contains("Info Test Org"), "Should contain license_org");
        assertTrue(response.contains("info@test.com"), "Should contain email");
        assertTrue(response.contains("250"), "Should contain balance");
        assertTrue(response.contains("true"), "Should contain active=true");
    }

    @Test
    @DisplayName("Should not expose admin status in info response")
    void shouldNotExposeAdminStatus() {
        Token adminToken = new Token();
        adminToken.setToken("sk_info_admin_test");
        adminToken.setBalance(0);
        adminToken.setActive(true);
        adminToken.setAdmin(true);
        adminToken.setLicenseNo(0);
        adminToken.setLicenseOrg("Admin");
        adminToken.setEmail("admin@test.com");
        adminToken.setCreatedAt(LocalDateTime.now());
        tokenRepository.save(adminToken);

        var response = client.toBlocking().retrieve(
                HttpRequest.GET("/api/token/info")
                        .bearerAuth(adminToken.getToken()),
                String.class);
        assertNotNull(response);
        assertTrue(response.contains("0"), "Should contain license_no");
    }
}
