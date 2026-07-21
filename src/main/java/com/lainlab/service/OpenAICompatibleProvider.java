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

public class OpenAICompatibleProvider implements LLMProvider {
    private static final Logger LOG = LoggerFactory.getLogger(OpenAICompatibleProvider.class);

    private final HttpClient client;
    private final LlmClientFactory.LlmConfig cfg;
    private final ObjectMapper mapper = new ObjectMapper();

    private final PromptCache promptCache;

    public OpenAICompatibleProvider(HttpClient client, LlmClientFactory.LlmConfig cfg, PromptCache promptCache) {
        this.client = client;
        this.cfg = cfg;
        this.promptCache = promptCache;
    }

    @Override
    public Publisher<LLMResponse> generateReactive(LLMRequest request) {
        if (cfg.apiKey() == null || cfg.apiKey().isBlank()) {
            throw new IllegalStateException("API key not configured for OpenAI/Groq. Set OPENAI_API_KEY or GROQ_API_KEY.");
        }

        ObjectNode root = mapper.createObjectNode();
        root.put("model", cfg.model());

        ArrayNode messages = root.putArray("messages");
        messages.addObject()
                .put("role", "system")
                .put("content", request.bundle().system());
        messages.addObject()
                .put("role", "user")
                .put("content", request.bundle().user());

        root.putObject("response_format").put("type", "json_object");
        root.putObject("thinking").put("type", "disabled");
        root.put("stream", false);

        HttpRequest<String> http = HttpRequest.POST(cfg.url(), root.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .bearerAuth(cfg.apiKey());

        LOG.info("LLM Request: {}", root.toString().substring(0, Math.min(200, root.toString().length())) + (root.toString().length() > 200 ? "..." : ""));

        return Publishers.map(
                client.retrieve(http),
                json -> {
                    try {
                        JsonNode node = mapper.readTree(json);
                        return new LLMResponse(extractContent(node));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }

    private String extractContent(JsonNode node) {
        if (node.has("error")) {
            String msg = node.path("error").path("message").asText("Unknown LLM error");
            throw new RuntimeException("LLM API error: " + msg);
        }
        JsonNode choices = node.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            throw new RuntimeException("LLM returned empty choices: " + node);
        }
        String content = choices.get(0).path("message").path("content").asText("");
        if (content.isEmpty()) {
            throw new RuntimeException("LLM returned empty content in choices[0]");
        }
        return content;
    }
}
