package com.lainlab.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lainlab.dto.LLMProvider;
import com.lainlab.dto.LLMRequest;
import com.lainlab.dto.LLMResponse;
import com.lainlab.util.PromptCache;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeminiProvider implements LLMProvider {
    private static final Logger LOG = LoggerFactory.getLogger(GeminiProvider.class);

    private final HttpClient client;
    private final LlmClientFactory.LlmConfig cfg;
    private final ObjectMapper mapper = new ObjectMapper();

    private final PromptCache promptCache;

    public GeminiProvider(HttpClient client, LlmClientFactory.LlmConfig cfg, PromptCache promptCache) {
        this.client = client;
        this.cfg = cfg;
        this.promptCache = promptCache;
    }

    @Override
    public Publisher<LLMResponse> generateReactive(LLMRequest request) {
        if (cfg.apiKey() == null || cfg.apiKey().isBlank()) {
            throw new IllegalStateException("API key not configured for Gemini. Set GEMINI_API_KEY.");
        }

        // Build Gemini JSON
        ObjectNode root = mapper.createObjectNode();
        ArrayNode contents = root.putArray("contents");

        String safe = request.bundle().combined()
                .replace("\r\n", "\n")
                .replace("<|eos|>", "")
                .replace("NONCE:", "NONCE: ");

        ObjectNode content = contents.addObject();
        content.put("role", "user");

        ArrayNode parts = content.putArray("parts");
        ObjectNode part = parts.addObject();
        part.put("text", safe);

        HttpRequest<String> http = HttpRequest.POST(cfg.url(), root.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-goog-api-key", cfg.apiKey());

        LOG.info("LLM Request: {}", root.toString().substring(0, Math.min(200, root.toString().length())) + (root.toString().length() > 200 ? "..." : ""));

        return Publishers.map(
                client.retrieve(http),
                json -> {
                    try {
                        JsonNode node = mapper.readTree(json);
                        return new LLMResponse(extractContent(node));
                    } catch (Exception e) {
                        LOG.error("Failed to parse Gemini response: {}", json, e);
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    private String extractContent(JsonNode node) {
        if (node.has("error")) {
            String msg = node.path("error").path("message").asText("Unknown Gemini error");
            throw new RuntimeException("Gemini API error: " + msg);
        }
        JsonNode candidates = node.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            String blockReason = node.path("promptFeedback").path("blockReason").asText(null);
            if (blockReason != null) {
                throw new RuntimeException("Gemini blocked prompt: " + blockReason);
            }
            throw new RuntimeException("Gemini returned no candidates: " + node);
        }
        String text = candidates.get(0).path("content").path("parts").get(0).path("text").asText("");
        if (text.isEmpty()) {
            throw new RuntimeException("Gemini returned empty text in candidates[0]");
        }
        return text;
    }
}
