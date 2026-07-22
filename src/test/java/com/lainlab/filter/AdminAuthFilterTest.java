package com.lainlab.filter;

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
@DisplayName("AdminAuthFilter Tests")
class AdminAuthFilterTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    TokenRepository tokenRepository;

    private Token adminToken;
    private Token userToken;
    private Token inactiveAdminToken;

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();

        adminToken = new Token();
        adminToken.setToken("sk_admin_auth_test");
        adminToken.setBalance(0);
        adminToken.setActive(true);
        adminToken.setAdmin(true);
        adminToken.setLicenseNo(9001);
        adminToken.setLicenseOrg("Admin");
        adminToken.setEmail("admin@localhost");
        adminToken.setCreatedAt(LocalDateTime.now());
        tokenRepository.save(adminToken);

        userToken = new Token();
        userToken.setToken("sk_user_auth_test");
        userToken.setBalance(100);
        userToken.setActive(true);
        userToken.setAdmin(false);
        userToken.setLicenseNo(9002);
        userToken.setLicenseOrg("User Org");
        userToken.setEmail("user@localhost");
        userToken.setCreatedAt(LocalDateTime.now());
        tokenRepository.save(userToken);

        inactiveAdminToken = new Token();
        inactiveAdminToken.setToken("sk_admin_inactive_auth_test");
        inactiveAdminToken.setBalance(0);
        inactiveAdminToken.setActive(false);
        inactiveAdminToken.setAdmin(true);
        inactiveAdminToken.setLicenseNo(9003);
        inactiveAdminToken.setLicenseOrg("Inactive Admin");
        inactiveAdminToken.setEmail("inactive@localhost");
        inactiveAdminToken.setCreatedAt(LocalDateTime.now());
        tokenRepository.save(inactiveAdminToken);
    }

    @Test
    @DisplayName("Should reject request without Authorization header")
    void shouldRejectMissingAuth() {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(
                        HttpRequest.GET("/admin/tokens/all"), String.class)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("Should reject malformed Authorization header")
    void shouldRejectMalformedAuth() {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(
                        HttpRequest.GET("/admin/tokens/all")
                                .header("Authorization", "Basic invalid"),
                        String.class)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("Should reject non-existent token")
    void shouldRejectInvalidToken() {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(
                        HttpRequest.GET("/admin/tokens/all")
                                .bearerAuth("sk_nonexistent_token_xyz"),
                        String.class)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("Should reject non-admin token")
    void shouldRejectNonAdminToken() {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(
                        HttpRequest.GET("/admin/tokens/all")
                                .bearerAuth(userToken.getToken()),
                        String.class)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("Should reject inactive admin token")
    void shouldRejectInactiveAdminToken() {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(
                        HttpRequest.GET("/admin/tokens/all")
                                .bearerAuth(inactiveAdminToken.getToken()),
                        String.class)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("Should allow valid admin token")
    void shouldAllowValidAdminToken() {
        assertDoesNotThrow(() ->
                client.toBlocking().exchange(
                        HttpRequest.GET("/admin/tokens/all")
                                .bearerAuth(adminToken.getToken()),
                        String.class)
        );
    }

    @Test
    @DisplayName("Filter should apply only to /admin/** paths")
    void shouldApplyOnlyToAdminPaths() {
        assertTrue(AdminAuthFilter.class.getAnnotation(
                io.micronaut.http.annotation.ServerFilter.class).value()[0].contains("/admin/**"));
    }
}
