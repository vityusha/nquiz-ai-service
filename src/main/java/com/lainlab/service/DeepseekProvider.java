package com.lainlab.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lainlab.dto.LLMProvider;
import com.lainlab.dto.LLMRequest;
import com.lainlab.dto.LLMResponse;
import com.lainlab.model.Provider;
import com.lainlab.util.PromptCache;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.core.io.ResourceResolver;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeepseekProvider implements LLMProvider {
    private static final Logger LOG = LoggerFactory.getLogger(DeepseekProvider.class);

    private final HttpClient client;
    private final LlmClientFactory.LlmConfig cfg;
    private final ObjectMapper mapper = new ObjectMapper();

    private final PromptCache promptCache;

    public DeepseekProvider(HttpClient client, LlmClientFactory.LlmConfig cfg, PromptCache promptCache) {
        this.client = client;
        this.cfg = cfg;
        this.promptCache = promptCache;
    }

    @Override
    public Publisher<LLMResponse> generateReactive(LLMRequest request) {

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
                        String content = node.path("choices").get(0)
                                .path("message").path("content").asText();
                        return new LLMResponse(content);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
        );
    }
}
