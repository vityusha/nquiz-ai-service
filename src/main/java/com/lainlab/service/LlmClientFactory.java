package com.lainlab.service;

import com.lainlab.model.Provider;
import io.micronaut.context.annotation.Value;
import io.micronaut.http.client.DefaultHttpClientConfiguration;
import io.micronaut.http.client.HttpClient;
import jakarta.inject.Singleton;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class LlmClientFactory {

    @Value("${llm.groq.url}") String groqUrl;
    @Value("${llm.groq.model}") String groqModel;
    @Value("${llm.groq.access-id:}") String groqAccessId;
    @Value("${llm.groq.api-key:}") String groqApiKey;

    @Value("${llm.openai.url}") String openaiUrl;
    @Value("${llm.openai.model}") String openaiModel;
    @Value("${llm.openai.access-id:}") String openaiAccessId;
    @Value("${llm.openai.api-key:}") String openaiApiKey;

    @Value("${llm.gemini.url}") String geminiUrl;
    @Value("${llm.gemini.model}") String geminiModel;
    @Value("${llm.gemini.access-id:}") String geminiAccessId;
    @Value("${llm.gemini.api-key:}") String geminiApiKey;

    @Value("${llm.deepseek.url}") String deepseekUrl;
    @Value("${llm.deepseek.model}") String deepseekModel;
    @Value("${llm.deepseek.access-id:}") String deepseekAccessId;
    @Value("${llm.deepseek.api-key:}") String deepseekApiKey;

    // Clients cache
    private final Map<Provider, HttpClient> clientCache = new ConcurrentHashMap<>();

    public LlmConfig getConfig(Provider provider) {
        return switch (provider) {
            case GROQ -> new LlmConfig(groqUrl, groqModel, groqAccessId, groqApiKey);
            case OPENAI -> new LlmConfig(openaiUrl, openaiModel, openaiAccessId, openaiApiKey);
            case GEMINI -> new LlmConfig(geminiUrl, geminiModel, geminiAccessId, geminiApiKey);
            case DEEPSEEK -> new LlmConfig(deepseekUrl, deepseekModel, deepseekAccessId, deepseekApiKey);
        };
    }

    public HttpClient getClient(Provider provider) {
        return clientCache.computeIfAbsent(provider, this::createHttpClient);
    }

    private HttpClient createHttpClient(Provider provider) {
        LlmConfig config = getConfig(provider);

        DefaultHttpClientConfiguration configuration = new DefaultHttpClientConfiguration();
        configuration.setReadTimeout(Duration.ofSeconds(90));
        configuration.setConnectTimeout(Duration.ofSeconds(10));

        try {
            return HttpClient.create(new URL(config.url()), configuration);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid URL for provider: " + provider, e);
        }
    }

    public record LlmConfig(String url, String model, String accessId, String apiKey) {}
}
