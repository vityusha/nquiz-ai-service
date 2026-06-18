package com.lainlab.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.micronaut.core.order.Ordered; // Позволяет управлять порядком вызова
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.micronaut.http.exceptions.HttpStatusException;
import jakarta.inject.Singleton;

import java.time.Duration;

@Singleton
@ServerFilter("/api/**") // Новая аннотация для Micronaut 4
public class RateLimitFilter implements Ordered {

    private final Bucket bucket;

    public RateLimitFilter() {
        this.bucket = Bucket4j.builder()
                .addLimit(Bandwidth.simple(10, Duration.ofMinutes(1))) // 10 запросов в минуту
                .build();
    }

    @Override
    public int getOrder() {
        // Устанавливаем наивысший приоритет.
        // Лимитер отработает самым первым, еще ДО проверки токенов в базе данных.
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @RequestFilter
    public void doFilter(HttpRequest<?> request) { // Тип void исключает любые NPE в Netty
        if (!bucket.tryConsume(1)) {
            // Прерываем запрос и возвращаем 429 ошибку
            throw new HttpStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
        }
        // Если лимит не превышен, метод просто завершается, и запрос легально идет дальше
    }
}
