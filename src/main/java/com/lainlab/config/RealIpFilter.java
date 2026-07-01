package com.lainlab.config;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.reactivestreams.Publisher;

@Filter("/**") // Intercept all requests
public class RealIpFilter implements HttpServerFilter {

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        // 1. Read the real client IP from the header sent by Nginx
        String realIp = request.getHeaders().getFirst("X-Real-IP").orElse(null);

        // 2. If X-Real-IP is missing (e.g. local run), fall back to X-Forwarded-For
        if (realIp == null || realIp.isEmpty() || realIp.equalsIgnoreCase("unknown")) {
            String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For").orElse(null);
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                // X-Forwarded-For may contain a chain: "client, proxy1, proxy2"
                // Take the first (leftmost) IP — that is the real client
                realIp = xForwardedFor.split(",")[0].trim();
            }
        }

        // 3. If we found a real IP, store it in a request attribute
        if (realIp != null && !realIp.isEmpty() && !realIp.equalsIgnoreCase("unknown")) {
            request.setAttribute("realClientIp", realIp);
        }

        // 4. Continue the filter chain (to the controller)
        return chain.proceed(request);
    }
}
