package com.lainlab.dto;

import org.reactivestreams.Publisher;

public interface LLMProvider {
    public Publisher<LLMResponse> generateReactive(LLMRequest request) throws Exception;
}
