package com.lainlab.service;

import com.lainlab.model.Provider;
import com.lainlab.util.PromptCache;
import io.micronaut.context.annotation.Factory;
import jakarta.inject.Singleton;

@Factory
public class ProviderFactory {

    @Singleton
    public DeepseekProvider deepseekProvider(LlmClientFactory factory,
                                             PromptCache promptCache) {
        return new DeepseekProvider(
                factory.getClient(Provider.DEEPSEEK),
                factory.getConfig(Provider.DEEPSEEK),
                promptCache
        );
    }

    @Singleton
    public OpenAICompatibleProvider openaiProvider(LlmClientFactory factory,
                                                   PromptCache promptCache) {
        return new OpenAICompatibleProvider(
                factory.getClient(Provider.OPENAI),
                factory.getConfig(Provider.OPENAI),
                promptCache
        );
    }

    @Singleton
    public GeminiProvider geminiProvider(LlmClientFactory factory,
                                         PromptCache promptCache) {
        return new GeminiProvider(
                factory.getClient(Provider.GEMINI),
                factory.getConfig(Provider.GEMINI),
                promptCache
        );
    }
}
