package com.lainlab.filter;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@DisplayName("SecurityHeadersFilter Tests")
class SecurityHeadersFilterTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    @DisplayName("Adds security headers to successful responses")
    void addsHeadersOnSuccess() {
        HttpResponse<String> resp = client.toBlocking().exchange(HttpRequest.GET("/health"), String.class);
        assertEquals(HttpStatus.OK, resp.getStatus());
        assertEquals("nosniff", resp.header("X-Content-Type-Options"));
        assertEquals("DENY", resp.header("X-Frame-Options"));
        assertEquals("no-referrer", resp.header("Referrer-Policy"));
        assertEquals(SecurityHeadersFilter.CONTENT_SECURITY_POLICY, resp.header("Content-Security-Policy"));
        assertNotNull(resp.header("Strict-Transport-Security"));
    }

    @Test
    @DisplayName("Adds security headers to error responses")
    void addsHeadersOnError() {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(HttpRequest.GET("/api/token/info"), String.class));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        assertEquals("nosniff", ex.getResponse().header("X-Content-Type-Options"));
    }
}
