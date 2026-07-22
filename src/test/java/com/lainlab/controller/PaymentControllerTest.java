package com.lainlab.controller;

import com.lainlab.db.Token;
import com.lainlab.db.TokenRepository;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@DisplayName("PaymentController Integration Tests")
class PaymentControllerTest {

    private static final String WEBHOOK_SECRET = "whsec_test_secret_key_12345";

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    TokenRepository tokenRepository;

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /webhook/payment should be accessible without admin auth")
    void testWebhookAccessibleWithoutAuth() {
        String body = "{\"metadata\":{\"token\":\"nonexistent\"},\"amount\":50}";

        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () ->
            client.toBlocking().exchange(
                HttpRequest.POST("/webhook/payment", body)
                    .contentType(MediaType.APPLICATION_JSON),
                String.class
            )
        );

        assertNotEquals(HttpStatus.UNAUTHORIZED, ex.getStatus(),
            "Webhook should not require admin auth");
    }

    @Test
    @DisplayName("POST /webhook/payment should process valid signed webhook")
    void testWebhookProcessesValidPayload() throws Exception {
        Token token = new Token();
        token.setToken("sk_user_webhook_test");
        token.setBalance(50);
        token.setActive(true);
        token.setAdmin(false);
        token.setLicenseNo(888);
        token.setLicenseOrg("Webhook Test Org");
        token.setEmail("webhook@test.com");
        token.setCreatedAt(LocalDateTime.now());
        tokenRepository.save(token);

        String body = "{\"metadata\":{\"token\":\"sk_user_webhook_test\"},\"amount\":25}";

        long timestamp = System.currentTimeMillis() / 1000;
        String payload = timestamp + "." + body;
        String sig = hmacSha256(WEBHOOK_SECRET, payload);
        String sigHeader = "t=" + timestamp + ",v1=" + sig;

        assertDoesNotThrow(() ->
            client.toBlocking().exchange(
                HttpRequest.POST("/webhook/payment", body)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Stripe-Signature", sigHeader),
                String.class
            )
        );

        Token fromDb = tokenRepository.findById(token.getId()).orElseThrow();
        assertEquals(75, fromDb.getBalance(), "Balance should increase by webhook amount");
    }

    private static String hmacSha256(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
