package com.lainlab.filter;

import io.micronaut.core.order.Ordered;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@DisplayName("BotFilter Tests")
class BotFilterTest {

    @Inject
    @Client("/")
    HttpClient client;

    @ParameterizedTest
    @ValueSource(strings = {
            "/vendor/phpunit/phpunit/src/Util/PHP/eval-stdin.php",
            "/foo/vendor/phpunit/bar",
            "/tests/vendor/phpunit/phpunit/src/Util/PHP/eval-stdin.php",
    })
    @DisplayName("Should block PHPUnit scanner paths")
    void shouldBlockPhpUnitScanner(String path) {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(HttpRequest.GET(path), String.class)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/.env",
            "/config/.env",
            "/assets/.env.local",
            "/.git/config",
            "/.svn/entries",
    })
    @DisplayName("Should block dotfile scanner paths")
    void shouldBlockDotfileScanner(String path) {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(HttpRequest.GET(path), String.class)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/wp-admin/index.php",
            "/wp-login.php",
            "/xmlrpc.php",
            "/phpmyadmin/",
            "/admin/config.php",
            "/config.inc.php",
            "/joomla/administrator/index.php",
            "/shell.php",
            "/cmd.sh",
            "/console",
    })
    @DisplayName("Should block common CMS/vuln scanner paths")
    void shouldBlockCmsScanner(String path) {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(HttpRequest.GET(path), String.class)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/capabilities",
            "/search",
            "/",
            "/health",
    })
    @DisplayName("Should allow unauthenticated paths")
    void shouldAllowUnauthenticatedPaths(String path) {
        assertDoesNotThrow(() ->
                client.toBlocking().exchange(HttpRequest.GET(path), String.class)
        );
    }

    @Test
    @DisplayName("Should be case-insensitive for blocked paths")
    void shouldBeCaseInsensitive() {
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().exchange(
                        HttpRequest.GET("/WP-ADMIN/index.php"), String.class)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    @DisplayName("Filter order should be higher precedence than auth filters")
    void shouldHaveHigherPrecedence() {
        BotFilter filter = new BotFilter();
        assertTrue(filter.getOrder() < 0, "BotFilter should have negative (higher) precedence");
    }

    @Test
    @DisplayName("Should have highest precedence in filter chain")
    void shouldHaveHighestPrecedence() {
        assertTrue(BotFilter.class.getAnnotation(
                io.micronaut.http.annotation.ServerFilter.class).value()[0].contains("/**"));
    }
}
