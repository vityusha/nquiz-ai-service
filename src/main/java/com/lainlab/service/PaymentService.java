package com.lainlab.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lainlab.db.ProcessedWebhookEventRepository;
import com.lainlab.db.Token;
import com.lainlab.db.TokenRepository;
import io.micronaut.context.annotation.Value;
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

@Singleton
public class PaymentService {

    @Inject
    TokenRepository tokens;

    @Inject
    ProcessedWebhookEventRepository processedWebhookEvents;

    @Value("${payment.webhook-secret:}")
    private String webhookSecret;

    private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final long MAX_TIMESTAMP_AGE_SECONDS = 300;
    private final ObjectMapper mapper = new ObjectMapper();

    // -----------------------------
    // Manual top-up
    // -----------------------------
    public Token topUp(Token token, int amount) {
        if (amount <= 0) {
            return tokens.findById(token.getId())
                    .orElseThrow(() -> new IllegalStateException("Token not found: " + token.getId()));
        }

        int updated = tokens.addBalance(token.getId(), amount);
        if (updated == 0) {
            throw new IllegalStateException("Token not found: " + token.getId());
        }

        Token fresh = tokens.findById(token.getId())
                .orElseThrow(() -> new IllegalStateException("Token not found after topUp"));

        LOG.info("Token {} topped up by {} → new balance {}",
                token.getId(), amount, fresh.getBalance());

        return fresh;
    }

    // -----------------------------
    // Automatic top-up via Stripe / YooKassa webhook
    // -----------------------------
    @Transactional
    public void handleWebhook(String rawJson, Map<String, String> headers) {
        if (!verifySignature(rawJson, headers)) {
            LOG.warn("Webhook signature verification failed");
            throw new SecurityException("Invalid webhook signature");
        }

        JsonNode json;
        try {
            json = mapper.readTree(rawJson);
        } catch (Exception e) {
            LOG.error("Webhook JSON parse error: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid webhook JSON", e);
        }

        String eventId = extractEventId(json);
        if (eventId == null || eventId.isBlank()) {
            LOG.error("Webhook missing event id");
            throw new IllegalArgumentException("Webhook missing event id (id or event_id)");
        }

        String tokenValue = json.path("metadata").path("token").asText(null);
        int amount = Math.max(json.path("amount").asInt(0), 0);

        if (tokenValue == null || tokenValue.isBlank()) {
            LOG.error("Webhook missing metadata.token");
            throw new IllegalArgumentException("Webhook missing metadata.token");
        }

        Token token = tokens.findByToken(tokenValue)
                .orElseThrow(() -> {
                    LOG.error("Webhook token not found: {}", tokenValue);
                    return new IllegalStateException("Token not found: " + tokenValue);
                });

        int claimed = processedWebhookEvents.tryInsert(eventId, token.getId(), amount);
        if (claimed == 0) {
            LOG.info("Webhook already processed: eventId={}", eventId);
            return;
        }

        topUp(token, amount);

        LOG.info("Webhook top-up: eventId={}, token={}, amount={}", eventId, token.getId(), amount);
    }

    private static String extractEventId(JsonNode json) {
        String id = json.path("id").asText(null);
        if (id != null && !id.isBlank()) {
            return id;
        }
        return json.path("event_id").asText(null);
    }

    private boolean verifySignature(String rawJson, Map<String, String> headers) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            LOG.error("PAYMENT_WEBHOOK_SECRET is not set — rejecting webhook");
            return false;
        }

        String sigHeader = headers.get("stripe-signature");
        if (sigHeader == null || sigHeader.isBlank()) {
            return false;
        }

        try {
            String[] parts = sigHeader.split(",");
            String timestamp = null;
            String expectedSig = null;

            for (String part : parts) {
                String[] kv = part.split("=");
                if (kv.length == 2) {
                    if ("t".equals(kv[0])) {
                        timestamp = kv[1];
                    } else if ("v1".equals(kv[0])) {
                        expectedSig = kv[1];
                    }
                }
            }

            if (timestamp == null || expectedSig == null) {
                return false;
            }

            long timestampSeconds = Long.parseLong(timestamp);
            long age = Instant.now().getEpochSecond() - timestampSeconds;
            if (age < 0 || age > MAX_TIMESTAMP_AGE_SECONDS) {
                LOG.warn("Webhook timestamp too old or in the future: {}s ago", age);
                return false;
            }

            String payload = timestamp + "." + rawJson;
            byte[] computed = hmacSha256(webhookSecret, payload);
            byte[] expected = HexFormat.of().parseHex(expectedSig);

            return MessageDigest.isEqual(computed, expected);

        } catch (Exception e) {
            LOG.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }

    private static byte[] hmacSha256(String secret, String data) throws Exception {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }
}
