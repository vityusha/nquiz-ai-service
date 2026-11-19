package com.lainlab.config;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Checks how the bean is configured by Micronaut (not by calling the constructor directly):
 * the IPv6 entries must survive property resolution.
 */
@DisplayName("ClientIpResolver configuration Tests")
class ClientIpResolverContextTest {

    @Nested
    @MicronautTest(transactional = false)
    @DisplayName("Without app.trusted-proxies")
    class Defaults {

        @Inject
        ClientIpResolver resolver;

        @Test
        @DisplayName("Uses all default trusted ranges")
        void usesDefaults() {
            assertTrue(resolver.isTrusted("127.0.0.1"));
            assertTrue(resolver.isTrusted("::1"));
            assertTrue(resolver.isTrusted("10.1.2.3"));
            assertTrue(resolver.isTrusted("172.18.0.1"));
            assertTrue(resolver.isTrusted("192.168.1.10"));
            assertTrue(resolver.isTrusted("fd00::1"));
            assertFalse(resolver.isTrusted("8.8.8.8"));
        }
    }

    @Nested
    @MicronautTest(transactional = false)
    @Property(name = "app.trusted-proxies", value = "203.0.113.0/24,2001:db8::/32")
    @DisplayName("With app.trusted-proxies")
    class Configured {

        @Inject
        ClientIpResolver resolver;

        @Test
        @DisplayName("Uses only the configured ranges, including IPv6")
        void usesConfigured() {
            assertTrue(resolver.isTrusted("203.0.113.9"));
            assertTrue(resolver.isTrusted("2001:db8::5"));
            assertFalse(resolver.isTrusted("127.0.0.1"));
            assertFalse(resolver.isTrusted("10.1.2.3"));
        }
    }
}
