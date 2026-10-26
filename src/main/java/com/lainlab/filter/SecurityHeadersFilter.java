package com.lainlab.filter;

import io.micronaut.http.MutableHttpHeaders;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.ResponseFilter;
import io.micronaut.http.annotation.ServerFilter;
import jakarta.inject.Singleton;

/**
 * Adds security headers to every response, so protection does not depend solely on the reverse proxy.
 * Headers already set (e.g. by a controller) are left untouched.
 */
@Singleton
@ServerFilter("/**")
public class SecurityHeadersFilter {

    // index.html and search page use inline scripts/styles, Google Fonts and js-yaml from jsDelivr
    static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'self'",
            "script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net",
            "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
            "font-src 'self' https://fonts.gstatic.com",
            "img-src 'self' data:",
            "connect-src 'self'",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'");

    @ResponseFilter
    public void addHeaders(MutableHttpResponse<?> response) {
        MutableHttpHeaders headers = response.getHeaders();
        setIfAbsent(headers, "X-Content-Type-Options", "nosniff");
        setIfAbsent(headers, "X-Frame-Options", "DENY");
        setIfAbsent(headers, "Referrer-Policy", "no-referrer");
        setIfAbsent(headers, "Content-Security-Policy", CONTENT_SECURITY_POLICY);
        // Browsers ignore HSTS over plain HTTP, so it is safe to send behind a TLS-terminating proxy
        setIfAbsent(headers, "Strict-Transport-Security", "max-age=31536000");
    }

    private static void setIfAbsent(MutableHttpHeaders headers, String name, String value) {
        if (!headers.contains(name)) {
            headers.add(name, value);
        }
    }
}
