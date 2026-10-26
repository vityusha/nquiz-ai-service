package com.lainlab.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lainlab.config.ClientIpResolver;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.micronaut.context.annotation.Value;
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
@ServerFilter({"/api/**", "/admin/**"})
public class RateLimitFilter implements Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final int REQUESTS_PER_MINUTE = 10;
    // Admin API: separate bucket to slow down admin token brute force
    private static final int DEFAULT_ADMIN_REQUESTS_PER_MINUTE = 10;
    private static final String ADMIN_PREFIX = "/admin/";
    private static final int MAX_IPS = 10_000;
    private static final Duration BUCKET_EXPIRY = Duration.ofMinutes(5);

    private final Cache<String, Bucket> buckets;
    private final Cache<String, Bucket> adminBuckets;
    private final ClientIpResolver ipResolver;
    private final int adminRequestsPerMinute;

    public RateLimitFilter(ClientIpResolver ipResolver,
                           @Value("${app.rate-limit.admin-per-minute:" + DEFAULT_ADMIN_REQUESTS_PER_MINUTE + "}") int adminRequestsPerMinute) {
        this.ipResolver = ipResolver;
        this.adminRequestsPerMinute = adminRequestsPerMinute;
        this.buckets = newCache();
        this.adminBuckets = newCache();
    }

    private static Cache<String, Bucket> newCache() {
        return Caffeine.newBuilder()
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
        // Resolved here directly: this filter runs before RealIpFilter sets the attribute
        String ip = ipResolver.resolve(request);
        boolean admin = request.getUri().getPath().startsWith(ADMIN_PREFIX);
        Bucket bucket = admin
                ? adminBuckets.get(ip, k -> createBucket(adminRequestsPerMinute))
                : buckets.get(ip, k -> createBucket(REQUESTS_PER_MINUTE));

        if (!bucket.tryConsume(1)) {
            LOG.warn("Rate limit exceeded for IP: {} on {}", ip, admin ? "admin API" : "API");
            throw new HttpStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded");
        }
    }

    private static Bucket createBucket(int requestsPerMinute) {
        return Bucket4j.builder()
                .addLimit(Bandwidth.simple(requestsPerMinute, Duration.ofMinutes(1)))
                .build();
    }
}
