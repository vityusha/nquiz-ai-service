package com.lainlab.service;

import com.lainlab.db.Token;
import com.lainlab.db.TokenRepository;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@DisplayName("PaymentService Tests")
class PaymentServiceTest {

    private static final String WEBHOOK_SECRET = "whsec_test_secret_key_12345";

    @Inject
    PaymentService paymentService;

    @Inject
    TokenRepository tokenRepository;

    private Token testToken;

    @BeforeEach
    void setUp() {
        tokenRepository.deleteAll();

        testToken = new Token();
        testToken.setToken("sk_user_payment_test");
        testToken.setBalance(100);
        testToken.setActive(true);
        testToken.setAdmin(false);
        testToken.setLicenseNo(777);
        testToken.setLicenseOrg("Payment Test Org");
        testToken.setEmail("payment@test.com");
        testToken.setCreatedAt(LocalDateTime.now());
        tokenRepository.save(testToken);
    }

    @Test
    @DisplayName("topUp should increase balance and re-read from DB")
    void testTopUpReReadsFromDb() {
        Token result = paymentService.topUp(testToken, 50);

        assertEquals(150, result.getBalance());
        assertEquals(testToken.getId(), result.getId());

        Token fromDb = tokenRepository.findById(testToken.getId()).orElseThrow();
        assertEquals(150, fromDb.getBalance());
    }

    @Test
    @DisplayName("topUp with zero amount should not change balance")
    void testTopUpZeroAmountIsNoOp() {
        Token result = paymentService.topUp(testToken, 0);

        assertEquals(100, result.getBalance());
        Token fromDb = tokenRepository.findById(testToken.getId()).orElseThrow();
        assertEquals(100, fromDb.getBalance());
    }

    @Test
    @DisplayName("handleWebhook should reject invalid signature")
    void testRejectsInvalidSignature() {
        String body = webhookBody("evt_invalid_sig", "sk_user_payment_test", 50);
        Map<String, String> headers = Map.of("stripe-signature", "t=123,v1=invalidsig");

        assertThrows(SecurityException.class, () ->
            paymentService.handleWebhook(body, headers)
        );

        Token fromDb = tokenRepository.findById(testToken.getId()).orElseThrow();
        assertEquals(100, fromDb.getBalance(), "Balance should not change on invalid signature");
    }

    @Test
    @DisplayName("handleWebhook should accept valid HMAC-SHA256 signature")
    void testAcceptsValidSignature() throws Exception {
        String body = webhookBody("evt_test_001", "sk_user_payment_test", 50);
        Map<String, String> headers = signedHeaders(body);

        paymentService.handleWebhook(body, headers);

        Token fromDb = tokenRepository.findById(testToken.getId()).orElseThrow();
        assertEquals(150, fromDb.getBalance(), "Balance should increase by webhook amount");
    }

    @Test
    @DisplayName("handleWebhook should reject missing stripe-signature header")
    void testRejectsMissingSigHeader() {
        String body = webhookBody("evt_test_002", "sk_user_payment_test", 50);
        Map<String, String> headers = Map.of();

        assertThrows(SecurityException.class, () ->
            paymentService.handleWebhook(body, headers)
        );
    }

    @Test
    @DisplayName("handleWebhook should reject old timestamp (replay protection)")
    void testRejectsOldTimestamp() throws Exception {
        long oldTimestamp = 1000000000;
        String body = webhookBody("evt_test_003", "sk_user_payment_test", 50);

        String payload = oldTimestamp + "." + body;
        String sig = hmacSha256(WEBHOOK_SECRET, payload);
        String sigHeader = "t=" + oldTimestamp + ",v1=" + sig;

        Map<String, String> headers = Map.of("stripe-signature", sigHeader);

        assertThrows(SecurityException.class, () ->
            paymentService.handleWebhook(body, headers)
        );
    }

    @Test
    @DisplayName("handleWebhook should reject missing metadata.token")
    void testRejectsMissingMetadataToken() throws Exception {
        String body = "{\"id\":\"evt_test_004\",\"metadata\":{},\"amount\":50}";
        Map<String, String> headers = signedHeaders(body);

        assertThrows(IllegalArgumentException.class, () ->
            paymentService.handleWebhook(body, headers)
        );

        Token fromDb = tokenRepository.findById(testToken.getId()).orElseThrow();
        assertEquals(100, fromDb.getBalance());
    }

    @Test
    @DisplayName("handleWebhook should reject missing event id")
    void testRejectsMissingEventId() throws Exception {
        String body = "{\"metadata\":{\"token\":\"sk_user_payment_test\"},\"amount\":50}";
        Map<String, String> headers = signedHeaders(body);

        assertThrows(IllegalArgumentException.class, () ->
            paymentService.handleWebhook(body, headers)
        );

        Token fromDb = tokenRepository.findById(testToken.getId()).orElseThrow();
        assertEquals(100, fromDb.getBalance());
    }

    @Test
    @DisplayName("handleWebhook should reject unknown token")
    void testRejectsUnknownToken() throws Exception {
        String body = webhookBody("evt_test_005", "sk_user_does_not_exist", 50);
        Map<String, String> headers = signedHeaders(body);

        assertThrows(IllegalStateException.class, () ->
            paymentService.handleWebhook(body, headers)
        );

        Token fromDb = tokenRepository.findById(testToken.getId()).orElseThrow();
        assertEquals(100, fromDb.getBalance());
    }

    @Test
    @DisplayName("handleWebhook should be idempotent for duplicate event id")
    void testWebhookIdempotentDuplicateEvent() throws Exception {
        String body = webhookBody("evt_test_dup", "sk_user_payment_test", 50);
        Map<String, String> headers = signedHeaders(body);

        paymentService.handleWebhook(body, headers);
        paymentService.handleWebhook(body, headers);

        Token fromDb = tokenRepository.findById(testToken.getId()).orElseThrow();
        assertEquals(150, fromDb.getBalance(), "Duplicate webhook must not double-credit");
    }

    private static String webhookBody(String eventId, String token, int amount) {
        return "{\"id\":\"" + eventId + "\",\"metadata\":{\"token\":\"" + token + "\"},\"amount\":" + amount + "}";
    }

    private static Map<String, String> signedHeaders(String body) throws Exception {
        long timestamp = System.currentTimeMillis() / 1000;
        String payload = timestamp + "." + body;
        String sig = hmacSha256(WEBHOOK_SECRET, payload);
        return Map.of("stripe-signature", "t=" + timestamp + ",v1=" + sig);
    }

    private static String hmacSha256(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }
}
