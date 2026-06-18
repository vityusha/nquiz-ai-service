package com.lainlab.controller;

import com.lainlab.dto.QuestionRequest;
import com.lainlab.dto.QuestionResponseList;
import com.lainlab.service.QuestionService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.annotation.*;
import jakarta.inject.Inject;
import reactor.core.publisher.Mono;

@Controller("/api/questions")
public class QuestionController {

    @Inject
    private QuestionService questionService;

    @Post
    public Mono<QuestionResponseList> generate(@Body QuestionRequest req, HttpRequest<?> httpRequest) throws Exception {
        String ip = httpRequest.getRemoteAddress().getAddress().getHostAddress();
        return Mono.from(questionService.generateReactive(req, ip, httpRequest));
    }
}
