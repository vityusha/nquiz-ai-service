package com.lainlab.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lainlab.db.Token;
import com.lainlab.db.TokenRepository;
import io.micronaut.context.annotation.Value;
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
        token.setBalance(token.getBalance() + amount);
        tokens.update(token);

        Token fresh = tokens.findById(token.getId())
                .orElseThrow(() -> new IllegalStateException("Token not found after topUp"));

        LOG.info("Token {} topped up by {} → new balance {}",
                token.getId(), amount, fresh.getBalance());

        return fresh;
    }

    // -----------------------------
    // Automatic top-up via Stripe / YooKassa webhook
    // -----------------------------
    public void handleWebhook(String rawJson, Map<String, String> headers) {
        if (!verifySignature(rawJson, headers)) {
            LOG.warn("Webhook signature verification failed");
            throw new SecurityException("Invalid webhook signature");
        }

        try {
            JsonNode json = mapper.readTree(rawJson);

            String tokenValue = json.path("metadata").path("token").asText();
            int amount = json.path("amount").asInt();
            amount = Math.max(amount, 0);

            if (tokenValue == null || tokenValue.isBlank()) {
                LOG.error("Webhook missing token");
                return;
            }

            Token token = tokens.findByToken(tokenValue)
                    .orElseThrow(() -> new RuntimeException("Token not found"));

            topUp(token, amount);

            LOG.info("Webhook top-up: token={}, amount={}", token.getId(), amount);

        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("Webhook error: {}", e.getMessage());
        }
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
