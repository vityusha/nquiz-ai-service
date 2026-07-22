package com.lainlab.controller;

import com.lainlab.db.QuestionEntity;
import com.lainlab.db.QuestionRepository;
import com.lainlab.dto.QuestionRequest;
import com.lainlab.dto.QuestionResponseList;
import com.lainlab.service.QuestionService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import jakarta.inject.Inject;
import reactor.core.publisher.Mono;

import java.util.Map;

@Controller("/api/questions")
public class QuestionController {
    /*
    AI request
     */
    @Inject
    private QuestionService questionService;

    private String resolveClientIp(HttpRequest<?> request) {
        return request.getAttribute("realClientIp", String.class)
                .orElseGet(() -> {
                    if (request.getRemoteAddress() != null
                            && request.getRemoteAddress().getAddress() != null) {
                        return request.getRemoteAddress().getAddress().getHostAddress();
                    }
                    return "unknown";
                });
    }

    @Post("/generate")
    public Mono<QuestionResponseList> generate(@Body QuestionRequest req, HttpRequest<?> httpRequest) throws Exception {
        String ip = resolveClientIp(httpRequest);

        return Mono.from(questionService.generateReactive(req, ip, httpRequest));
    }

    /*
    User questions database
     */
    @Inject
    private QuestionRepository repository;

    /**
     * POST /api/questions/store
     * {
     *   "questions": [
     *     "question": "{...json...}",
     *     ...
     *   ]
     * }
     */
    @Post("/store")
    public HttpResponse<?> saveQuestion(HttpRequest<?> httpRequest, @Body QuestionResponseList body) {
        String ip = resolveClientIp(httpRequest);

        return questionService.saveQuestion(httpRequest, body, ip);
    }

    /**
     * GET /api/questions/get?mode=MATCHING&difficulty=B1&type=TENSES&language=ENGLISH&keywords=summer
     */
    @Get("/get")
    public HttpResponse<?> getQuestions(
        @QueryValue(defaultValue = "") String mode,
        @QueryValue(defaultValue = "") String difficulty,
        @QueryValue(defaultValue = "") String type,
        @QueryValue(defaultValue = "") String language,
        @QueryValue(defaultValue = "") String keywords,
        Pageable pageable
    ) {
        Page<QuestionEntity> result;

        if (!mode.isEmpty()) {
            result = repository.findByMode(mode, pageable);
        } else if (!difficulty.isEmpty()) {
            result = repository.findByDifficulty(difficulty, pageable);
        } else if (!type.isEmpty()) {
            result = repository.findByType(type, pageable);
        } else if (!language.isEmpty()) {
            result = repository.findByLanguage(language, pageable);
        } else if (!keywords.isEmpty()) {
            result = repository.findByKeywords(keywords, pageable);
        } else {
            return HttpResponse.badRequest(Map.of(
                "error", "No get parameters provided"
            ));
        }

        return HttpResponse.ok(result);
    }
}
