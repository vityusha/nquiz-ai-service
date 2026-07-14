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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

@Singleton
@ServerFilter("/api/**")
public class RateLimitFilter implements Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitFilter.class);

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
            LOG.warn("Rate limit exceeded for {}", request.getUri());
            throw new HttpStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
        }
    }
}
