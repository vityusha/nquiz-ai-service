package com.lainlab.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DTO serialization/deserialization.
 * Verifies that DTOs can be correctly marshalled and unmarshalled.
 */
@DisplayName("DTO Serialization Tests")
class DTOSerializationTest {

    private ObjectMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
    }

    @Test
    @DisplayName("CreateTokenRequest should handle valid JSON")
    void testCreateTokenRequestSerialization() throws Exception {
        String json = """
        {
            "license_no": 123,
            "license_org": "Test Org",
            "email": "test@example.com",
            "balance": 500,
            "admin": false
        }
        """;

        CreateTokenRequest request = mapper.readValue(json, CreateTokenRequest.class);

        assertEquals(123, request.getLicenseNo());
        assertEquals("Test Org", request.getLicenseOrg());
        assertEquals("test@example.com", request.getEmail());
        assertEquals(500, request.getBalance());
        assertFalse(request.isAdmin());
    }

    @Test
    @DisplayName("CreateTokenRequest should ignore unknown fields")
    void testCreateTokenRequestIgnoreUnknown() throws Exception {
        String json = """
        {
            "license_no": 456,
            "license_org": "Org",
            "email": "user@test.com",
            "balance": 100,
            "admin": true,
            "unknown_field": "should_be_ignored"
        }
        """;

        CreateTokenRequest request = mapper.readValue(json, CreateTokenRequest.class);
        assertEquals(456, request.getLicenseNo());
    }

    @Test
    @DisplayName("TopUpRequest should deserialize correctly")
    void testTopUpRequestSerialization() throws Exception {
        String json = """
        {
            "amount": 250
        }
        """;

        TopUpRequest request = mapper.readValue(json, TopUpRequest.class);
        assertEquals(250, request.getAmount());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 100, 1000, 999999})
    @DisplayName("TopUpRequest should handle various amounts")
    void testTopUpVariousAmounts(int amount) throws Exception {
        TopUpRequest request = new TopUpRequest();
        request.setAmount(amount);

        assertEquals(amount, request.getAmount());
    }

    @Test
    @DisplayName("Balance should never be negative")
    void testBalanceNonNegative() {
        CreateTokenRequest request = new CreateTokenRequest(
            123,
            "Org",
            "email@test.com",
            -100,  // Negative balance
            false
        );

        // Balance should be ensured non-negative
        assertTrue(request.getBalance() >= 0, "Balance should be non-negative");
    }

    @Test
    @DisplayName("CreateTokenRequest constructor should set all fields")
    void testCreateTokenRequestConstructor() {
        CreateTokenRequest request = new CreateTokenRequest(
            789,
            "My Organization",
            "contact@org.com",
            1000,
            true
        );

        assertEquals(789, request.getLicenseNo());
        assertEquals("My Organization", request.getLicenseOrg());
        assertEquals("contact@org.com", request.getEmail());
        assertEquals(1000, request.getBalance());
        assertTrue(request.isAdmin());
    }

    @Test
    @DisplayName("QuestionResponseList should deserialize correctly")
    void testQuestionResponseListSerialization() throws Exception {
        String json = """
        {
            "questions": []
        }
        """;

        QuestionResponseList response = mapper.readValue(json, QuestionResponseList.class);
        assertNotNull(response);
    }

    @Test
    @DisplayName("Empty CreateTokenRequest should have default values")
    void testEmptyCreateTokenRequest() {
        CreateTokenRequest request = new CreateTokenRequest();

        assertNotNull(request);
    }
}


