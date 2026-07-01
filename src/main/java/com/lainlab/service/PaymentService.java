package com.lainlab.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lainlab.db.Token;
import com.lainlab.db.TokenRepository;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class PaymentService {

    @Inject
    TokenRepository tokens;

    private static final Logger LOG = LoggerFactory.getLogger(PaymentService.class);
    private final ObjectMapper mapper = new ObjectMapper();

    // -----------------------------
    // Manual top-up
    // -----------------------------
    public Token topUp(Token token, int amount) {
        token.setBalance(token.getBalance() + amount);
        tokens.update(token);

        LOG.info("Token {} topped up by {} → new balance {}",
                token.getToken(), amount, token.getBalance());

        return token;
    }

    // -----------------------------
    // Automatic top-up via Stripe / YooKassa webhook
    // -----------------------------
    public void handleWebhook(String rawJson) {
        try {
            JsonNode json = mapper.readTree(rawJson);

            String tokenValue = json.path("metadata").path("token").asText();
            int amount = json.path("amount").asInt();

            if (tokenValue == null || tokenValue.isBlank()) {
                LOG.error("Webhook missing token");
                return;
            }

            Token token = tokens.findByToken(tokenValue)
                    .orElseThrow(() -> new RuntimeException("Token not found"));

            topUp(token, amount);

            LOG.info("Webhook top-up: token={}, amount={}", tokenValue, amount);

        } catch (Exception e) {
            LOG.error("Webhook error: {}", e.getMessage());
        }
    }
}
