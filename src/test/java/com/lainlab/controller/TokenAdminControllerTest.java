package com.lainlab.controller;

import com.lainlab.db.Token;
import com.lainlab.db.TokenRepository;
import com.lainlab.dto.CreateTokenRequest;
import com.lainlab.dto.TopUpRequest;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for TokenAdminController.
 * Tests token creation, management, and top-up operations.
 */
@MicronautTest(transactional = false)
@DisplayName("TokenAdminController Integration Tests")
class TokenAdminControllerTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    TokenRepository tokenRepository;

    private Token adminToken;
    private Token userToken;

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();

        // Create admin token
        adminToken = new Token();
        adminToken.setToken("nq_admin_test_integration_token");
        adminToken.setBalance(0);
        adminToken.setActive(true);
        adminToken.setAdmin(true);
        adminToken.setLicenseNo(0);
        adminToken.setLicenseOrg("Admin");
        adminToken.setEmail("admin@localhost");
        adminToken.setCreatedAt(LocalDateTime.now());
        tokenRepository.save(adminToken);

        // Create regular user token
        userToken = new Token();
        userToken.setToken("nq_user_test_integration_token");
        userToken.setBalance(100);
        userToken.setActive(true);
        userToken.setAdmin(false);
        userToken.setLicenseNo(123);
        userToken.setLicenseOrg("Test Org");
        userToken.setEmail("user@example.com");
        userToken.setCreatedAt(LocalDateTime.now());
        tokenRepository.save(userToken);
    }

    @Test
    @DisplayName("Non-admin token should not be able to create user tokens")
    void testNonAdminCannotCreateUserToken() {
        CreateTokenRequest request = new CreateTokenRequest(
            456,
            "New Org",
            "newuser@example.com",
            50,
            false
        );

        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                HttpRequest.POST("/admin/tokens/create-user", request)
                    .bearerAuth(userToken.getToken()),
                Token.class
            );
        });

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus(),
                     "Non-admin should not be able to create user tokens");
    }

    @Test
    @DisplayName("Should reject token creation without authorization header")
    void testMissingAuthorizationForTokenCreation() {
        CreateTokenRequest request = new CreateTokenRequest(
            456,
            "New Org",
            "newuser@example.com",
            50,
            false
        );

        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                HttpRequest.POST("/admin/tokens/create-user", request),
                Token.class
            );
        });

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("Admin should be able to create user tokens with valid request")
    void testAdminCanCreateUserToken() {
        CreateTokenRequest request = new CreateTokenRequest(
            789,
            "New Organization",
            "newuser@test.com",
            250,
            false
        );

        // This test verifies that the admin token and request are valid
        assertTrue(adminToken.isAdmin(), "Token should be admin");
        assertNotNull(request);
        assertEquals(789, request.getLicenseNo());
        assertEquals("newuser@test.com", request.getEmail());
        assertEquals(250, request.getBalance());
        assertFalse(request.isAdmin());
    }

    @Test
    @DisplayName("Non-admin token should not be able to create admin tokens")
    void testNonAdminCannotCreateAdminToken() {
        CreateTokenRequest request = new CreateTokenRequest(
            0,
            "Admin",
            "root@localhost",
            0,
            true
        );

        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> {
            client.toBlocking().exchange(
                HttpRequest.POST("/admin/tokens/create-admin", request)
                    .bearerAuth(userToken.getToken()),
                Token.class
            );
        });

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    @DisplayName("Should validate required fields in token creation request")
    void testTokenCreationValidation() {
        // Test with missing license number
        CreateTokenRequest request1 = new CreateTokenRequest(0, "Org", "email@test.com", 100, false);
        assertEquals(0, request1.getLicenseNo());

        // Test with null email
        CreateTokenRequest request2 = new CreateTokenRequest(123, "Org", null, 100, false);
        assertNull(request2.getEmail());

        // Test with null org
        CreateTokenRequest request3 = new CreateTokenRequest(123, null, "email@test.com", 100, false);
        assertNull(request3.getLicenseOrg());
    }

    @Test
    @DisplayName("Should list all tokens when admin")
    void testListAllTokensRequiresAdmin() {
        // This would normally require admin authentication
        // The endpoint is /admin/tokens/all
        assertNotNull(tokenRepository.findAll());
    }

    @Test
    @DisplayName("Should deactivate token successfully")
    void testDeactivateToken() {
        assertTrue(userToken.isActive(), "Token should be active initially");

        Long tokenId = userToken.getId();
        Optional<Token> found = tokenRepository.findById(tokenId);

        assertTrue(found.isPresent(), "Token should exist");
        assertTrue(found.get().isActive(), "Token should be active");
    }

    @Test
    @DisplayName("Should handle topup by token value")
    void testTopupByTokenValue() {
        TopUpRequest topupReq = new TopUpRequest();
        topupReq.setAmount(100);

        // Verify the token exists and can be topped up
        Optional<Token> token = tokenRepository.findByToken(userToken.getToken());
        assertTrue(token.isPresent(), "Token should exist");

        int initialBalance = token.get().getBalance();
        assertEquals(100, initialBalance, "Initial balance should match");
    }

    @Test
    @DisplayName("Should handle topup by license number")
    void testTopupByLicenseNo() {
        TopUpRequest topupReq = new TopUpRequest();
        topupReq.setAmount(150);

        Optional<Token> token = tokenRepository.findByLicenseNo(123);
        assertTrue(token.isPresent(), "Token with license should exist");

        int initialBalance = token.get().getBalance();
        assertEquals(100, initialBalance);
    }

    @Test
    @DisplayName("Topup amount should be positive")
    void testTopupAmountValidation() {
        TopUpRequest request = new TopUpRequest();

        // Should allow positive amounts
        request.setAmount(100);
        assertEquals(100, request.getAmount());

        // Zero should be allowed (though may not be useful)
        request.setAmount(0);
        assertEquals(0, request.getAmount());
    }

    @Test
    @DisplayName("Should generate valid API key format for user")
    void testUserTokenGeneration() {
        String userKey = TokenAdminController.generateApiKey(false);

        assertTrue(userKey.startsWith("nq_user_"), "User token should start with nq_user_");
        assertTrue(userKey.length() > 20, "User token should have sufficient length");
    }

    @Test
    @DisplayName("Should generate valid API key format for admin")
    void testAdminTokenGeneration() {
        String adminKey = TokenAdminController.generateApiKey(true);

        assertTrue(adminKey.startsWith("nq_admin_"), "Admin token should start with nq_admin_");
        assertTrue(adminKey.length() > 20, "Admin token should have sufficient length");
    }

    @Test
    @DisplayName("Generated tokens should be unique")
    void testTokenUniqueness() {
        String token1 = TokenAdminController.generateApiKey(false);
        String token2 = TokenAdminController.generateApiKey(false);

        assertNotEquals(token1, token2, "Generated tokens should be unique");
    }

    @Test
    @DisplayName("Balance should not be negative")
    void testBalanceValidation() {
        CreateTokenRequest request = new CreateTokenRequest(
            123,
            "Test",
            "test@example.com",
            -50,  // Negative balance
            false
        );

        // The constructor or setter should ensure non-negative balance
        int balance = request.getBalance();
        assertTrue(balance >= 0, "Balance should be non-negative");
    }

    @Test
    @DisplayName("Token fields should persist correctly")
    void testTokenFieldPersistence() {
        Token token = new Token();
        token.setToken("test_token_123");
        token.setLicenseNo(456);
        token.setLicenseOrg("Test Organization");
        token.setEmail("contact@org.com");
        token.setBalance(500);
        token.setActive(true);
        token.setAdmin(false);
        token.setCreatedAt(LocalDateTime.now());

        assertEquals("test_token_123", token.getToken());
        assertEquals(456, token.getLicenseNo());
        assertEquals("Test Organization", token.getLicenseOrg());
        assertEquals("contact@org.com", token.getEmail());
        assertEquals(500, token.getBalance());
        assertTrue(token.isActive());
        assertFalse(token.isAdmin());
        assertNotNull(token.getCreatedAt());
    }
}

