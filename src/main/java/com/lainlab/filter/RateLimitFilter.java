package com.lainlab.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.micronaut.core.order.Ordered;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.micronaut.http.exceptions.HttpStatusException;
import jakarta.inject.Singleton;

import java.time.Duration;

@Singleton
@ServerFilter("/api/**")
public class RateLimitFilter implements Ordered {

    private final Bucket bucket;

    public RateLimitFilter() {
        this.bucket = Bucket4j.builder()
                .addLimit(Bandwidth.simple(10, Duration.ofMinutes(1))) // 10 requests per minute
                .build();
    }

    @Override
    public int getOrder() {
        // Run with highest priority so rate limiting happens before token DB checks.
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @RequestFilter
    public void doFilter(HttpRequest<?> request) {
        if (!bucket.tryConsume(1)) {
            // Reject the request with HTTP 429
            throw new HttpStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
        }
        // Under the limit: return normally and let the request continue
    }
}
