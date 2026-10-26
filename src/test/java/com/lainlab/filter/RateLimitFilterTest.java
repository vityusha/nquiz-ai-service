package com.lainlab.filter;

import com.lainlab.config.ClientIpResolver;
import io.micronaut.core.order.Ordered;
import io.micronaut.http.annotation.ServerFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RateLimitFilter Tests")
class RateLimitFilterTest {

    @Test
    @DisplayName("Should have highest precedence order")
    void shouldHaveHighestPrecedence() {
        RateLimitFilter filter = new RateLimitFilter(new ClientIpResolver(ClientIpResolver.DEFAULT_TRUSTED_PROXIES), 10);
        assertEquals(Ordered.HIGHEST_PRECEDENCE, filter.getOrder());
    }

    @Test
    @DisplayName("Should apply only to /api/** paths")
    void shouldApplyOnlyToApiPaths() {
        assertTrue(RateLimitFilter.class.getAnnotation(ServerFilter.class)
                .value()[0].contains("/api/**"));
    }

    @Test
    @DisplayName("Should also apply to /admin/** paths")
    void shouldApplyToAdminPaths() {
        assertTrue(java.util.Arrays.asList(RateLimitFilter.class.getAnnotation(ServerFilter.class).value())
                .contains("/admin/**"));
    }

    @Test
    @DisplayName("Filter should exist as singleton")
    void shouldExistAsSingleton() {
        assertNotNull(RateLimitFilter.class.getAnnotation(
                jakarta.inject.Singleton.class));
    }

    @Test
    @DisplayName("Should implement Ordered interface")
    void shouldImplementOrdered() {
        assertTrue(Ordered.class.isAssignableFrom(RateLimitFilter.class));
    }
}
