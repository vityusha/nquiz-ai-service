package com.lainlab.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
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

    private static final int REQUESTS_PER_MINUTE = 10;
    private static final int MAX_IPS = 10_000;
    private static final Duration BUCKET_EXPIRY = Duration.ofMinutes(5);

    private final Cache<String, Bucket> buckets;

    public RateLimitFilter() {
        this.buckets = Caffeine.newBuilder()
                .maximumSize(MAX_IPS)
                .expireAfterAccess(BUCKET_EXPIRY)
                .build();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @RequestFilter
    public void doFilter(HttpRequest<?> request) {
        String ip = resolveClientIp(request);
        Bucket bucket = buckets.get(ip, this::createBucket);

        if (!bucket.tryConsume(1)) {
            LOG.warn("Rate limit exceeded for IP: {}", ip);
            throw new HttpStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
        }
    }

    private Bucket createBucket(String key) {
        return Bucket4j.builder()
                .addLimit(Bandwidth.simple(REQUESTS_PER_MINUTE, Duration.ofMinutes(1)))
                .build();
    }

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
}
