package com.lainlab.config;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("ClientIpResolver Tests")
class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver(ClientIpResolver.DEFAULT_TRUSTED_PROXIES);

    private static HttpRequest<?> request(String peer, String realIp, String xff) {
        MutableHttpRequest<?> req = spy(HttpRequest.GET("/api/test"));
        doReturn(new InetSocketAddress(peer, 12345)).when(req).getRemoteAddress();
        if (realIp != null) {
            req.header("X-Real-IP", realIp);
        }
        if (xff != null) {
            req.header("X-Forwarded-For", xff);
        }
        return req;
    }

    @Test
    @DisplayName("Ignores forwarding headers from untrusted peer")
    void ignoresHeadersFromUntrustedPeer() {
        assertEquals("203.0.113.7", resolver.resolve(request("203.0.113.7", "1.2.3.4", "5.6.7.8")));
    }

    @Test
    @DisplayName("Uses X-Real-IP from trusted proxy")
    void usesRealIpFromTrustedProxy() {
        assertEquals("198.51.100.1", resolver.resolve(request("127.0.0.1", "198.51.100.1", null)));
        assertEquals("198.51.100.1", resolver.resolve(request("172.18.0.1", "198.51.100.1", null)));
    }

    @Test
    @DisplayName("Takes rightmost untrusted hop from X-Forwarded-For")
    void usesRightmostUntrustedXff() {
        // Leftmost entry is attacker-controlled
        assertEquals("198.51.100.2",
                resolver.resolve(request("127.0.0.1", null, "6.6.6.6, 198.51.100.2, 10.0.0.5")));
    }

    @Test
    @DisplayName("Falls back to peer on invalid header values")
    void fallsBackOnGarbage() {
        assertEquals("127.0.0.1", resolver.resolve(request("127.0.0.1", "not-an-ip", "evil.example.com")));
        assertEquals("127.0.0.1", resolver.resolve(request("127.0.0.1", "999.1.1.1", null)));
    }

    @Test
    @DisplayName("CIDR matching")
    void cidrMatching() {
        assertTrue(resolver.isTrusted("172.16.0.1"));
        assertTrue(resolver.isTrusted("172.31.255.255"));
        assertFalse(resolver.isTrusted("172.32.0.1"));
        assertTrue(resolver.isTrusted("::1"));
        assertFalse(resolver.isTrusted("8.8.8.8"));

        ClientIpResolver none = new ClientIpResolver("");
        assertFalse(none.isTrusted("127.0.0.1"));
    }
}
