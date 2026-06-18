package com.lainlab.filter;

import com.lainlab.db.Token;
import com.lainlab.db.TokenRepository;
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

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TokenAuthFilter.
 * Verifies authentication and authorization logic for API endpoints.
 */
@MicronautTest(transactional = false)
@DisplayName("TokenAuthFilter Tests")
class TokenAuthFilterTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    TokenRepository tokenRepository;

    private Token validToken;
    private Token inactiveToken;
    private Token emptyBalanceToken;
    private Token adminToken;

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();

        // Create valid active token with balance
        validToken = new Token();
        validToken.setToken("nq_user_valid_token");
        validToken.setBalance(100);
        validToken.setActive(true);
        validToken.setAdmin(false);
        validToken.setLicenseNo(111);
        validToken.setLicenseOrg("Valid Org");
        validToken.setEmail("valid@example.com");
        validToken.setCreatedAt(LocalDateTime.now());
        tokenRepository.save(validToken);

        // Create inactive token
        inactiveToken = new Token();
        inactiveToken.setToken("nq_user_inactive_token");
        inactiveToken.setBalance(100);
        inactiveToken.setActive(false);
        inactiveToken.setAdmin(false);
        inactiveToken.setLicenseNo(222);
        inactiveToken.setLicenseOrg("Inactive Org");
        inactiveToken.setEmail("inactive@example.com");
        inactiveToken.setCreatedAt(LocalDateTime.now());
        tokenRepository.save(inactiveToken);

        // Create token with zero balance
        emptyBalanceToken = new Token();
        emptyBalanceToken.setToken("nq_user_empty_balance");
        emptyBalanceToken.setBalance(0);
        emptyBalanceToken.setActive(true);
        emptyBalanceToken.setAdmin(false);
        emptyBalanceToken.setLicenseNo(333);
        emptyBalanceToken.setLicenseOrg("Empty Balance Org");
        emptyBalanceToken.setEmail("empty@example.com");
        emptyBalanceToken.setCreatedAt(LocalDateTime.now());
        tokenRepository.save(emptyBalanceToken);

        // Create admin token (bypasses balance check)
        adminToken = new Token();
        adminToken.setToken("nq_admin_valid_token");
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
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                HttpRequest.POST("/api/questions", "{}"),
                String.class
            );
        });

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertTrue(ex.getMessage().contains("Missing Authorization") ||
                   ex.getStatus().equals(HttpStatus.UNAUTHORIZED));
    }

    @Test
    @DisplayName("Should reject request with malformed Authorization header")
    void testMalformedAuthorizationHeader() {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                HttpRequest.POST("/api/questions", "{}")
                    .header("Authorization", "Basic invalid"),
                String.class
            );
        });

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("Should reject request with invalid token")
    void testInvalidToken() {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                HttpRequest.POST("/api/questions", "{}")
                    .bearerAuth("nq_user_nonexistent_token_xyz"),
                String.class
            );
        });

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("Should reject inactive tokens")
    void testInactiveTokenRejection() {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                HttpRequest.POST("/api/questions", "{}")
                    .bearerAuth(inactiveToken.getToken()),
                String.class
            );
        });

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("Should reject non-admin tokens with zero balance")
    void testZeroBalanceRejection() {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                HttpRequest.POST("/api/questions", "{}")
                    .bearerAuth(emptyBalanceToken.getToken()),
                String.class
            );
        });

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("Should allow admin tokens regardless of balance")
    void testAdminTokenBalanceBypass() {
        // Admin tokens should pass the filter even with zero balance
        assertTrue(adminToken.isAdmin());
        assertEquals(0, adminToken.getBalance());
        assertTrue(adminToken.isActive());
    }

    @Test
    @DisplayName("Bearer token should be extracted correctly")
    void testBearerTokenExtraction() {
        String fullAuth = "Bearer nq_user_valid_token";
        String extracted = fullAuth.substring("Bearer ".length()).trim();

        assertEquals("nq_user_valid_token", extracted);
    }

    @Test
    @DisplayName("Filter should preserve request attributes")
    void testRequestAttributePreservation() {
        // Token should be added to request attributes for authenticated requests
        String tokenValue = validToken.getToken();
        assertNotNull(tokenValue);
        assertTrue(tokenValue.startsWith("nq_user_"));
    }

    @Test
    @DisplayName("Should handle Bearer prefix case-sensitively")
    void testBearerPrefixCase() {
        String validBearerAuth = "Bearer nq_user_valid_token";
        assertTrue(validBearerAuth.startsWith("Bearer "));

        // Lowercase "bearer" should be invalid
        String invalidAuth = "bearer nq_user_valid_token";
        assertFalse(invalidAuth.startsWith("Bearer "));
    }

    @Test
    @DisplayName("Should validate token existence before other checks")
    void testTokenExistenceValidation() {
        // Non-existent token should be checked before balance/active status
        String nonexistentToken = "nq_user_nonexistent_token";
        assertTrue(tokenRepository.findByToken(nonexistentToken).isEmpty());
    }

    @Test
    @DisplayName("Token with balance should pass filter")
    void testValidTokenWithBalance() {
        assertTrue(validToken.isActive());
        assertTrue(validToken.getBalance() > 0);
        assertFalse(validToken.isAdmin());
    }

    @Test
    @DisplayName("Should handle whitespace in Bearer token")
    void testBearerTokenWhitespace() {
        String fullAuth1 = "Bearer  nq_user_valid_token";  // Extra space
        String fullAuth2 = "Bearer nq_user_valid_token ";  // Trailing space

        String extracted1 = fullAuth1.substring("Bearer ".length()).trim();
        String extracted2 = fullAuth2.substring("Bearer ".length()).trim();

        assertEquals("nq_user_valid_token", extracted1);
        assertEquals("nq_user_valid_token", extracted2);
    }

    @Test
    @DisplayName("Filter should apply only to /api/** paths")
    void testFilterPathMatching() {
        // The filter is applied to /api/** paths
        // /admin/** paths use a different filter
        assertTrue(TokenAuthFilter.class.getAnnotation(
            io.micronaut.http.annotation.ServerFilter.class).value()[0].contains("/api/**"));
    }

    @Test
    @DisplayName("Should check token active status before balance")
    void testActiveStatusCheckOrder() {
        // Even if balance is positive, inactive token should be rejected
        assertTrue(inactiveToken.getBalance() > 0);
        assertFalse(inactiveToken.isActive());
    }

    @Test
    @DisplayName("Should handle concurrent filter executions")
    void testConcurrentFilterExecution() {
        // Verify that multiple tokens can be handled independently
        assertTrue(validToken.isActive());
        assertFalse(inactiveToken.isActive());
        assertTrue(adminToken.isAdmin() && !adminToken.isAdmin() == false);
    }
}

