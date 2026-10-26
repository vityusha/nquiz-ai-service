package com.lainlab.config;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.HttpRequest;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the real client IP.
 * Forwarding headers (X-Real-IP / X-Forwarded-For) are honoured only when the TCP peer
 * is a trusted proxy, so a client connecting directly cannot spoof its address.
 */
@Singleton
public class ClientIpResolver {

    private static final Logger LOG = LoggerFactory.getLogger(ClientIpResolver.class);

    // Loopback + private ranges: reverse proxy on the same host or Docker bridge network
    public static final String DEFAULT_TRUSTED_PROXIES =
            "127.0.0.0/8,::1/128,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16,fc00::/7";

    private final List<Cidr> trustedProxies;

    public ClientIpResolver(@Value("${app.trusted-proxies:" + DEFAULT_TRUSTED_PROXIES + "}") String trustedProxies) {
        this.trustedProxies = parse(trustedProxies);
    }

    public String resolve(HttpRequest<?> request) {
        String peer = peerAddress(request);
        if (peer == null) {
            return "unknown";
        }
        if (!isTrusted(peer)) {
            return peer;
        }

        String realIp = request.getHeaders().getFirst("X-Real-IP").map(String::trim).orElse("");
        if (isValidIp(realIp)) {
            return realIp;
        }

        // X-Forwarded-For: "client, proxy1, proxy2" — walk from the right and skip trusted hops,
        // the leftmost entries are client-controlled
        String xff = request.getHeaders().getFirst("X-Forwarded-For").orElse("");
        String[] hops = xff.split(",");
        for (int i = hops.length - 1; i >= 0; i--) {
            String hop = hops[i].trim();
            if (!isValidIp(hop)) {
                break;
            }
            if (!isTrusted(hop)) {
                return hop;
            }
        }
        return peer;
    }

    private static String peerAddress(HttpRequest<?> request) {
        InetSocketAddress remote = request.getRemoteAddress();
        if (remote == null || remote.getAddress() == null) {
            return null;
        }
        return remote.getAddress().getHostAddress();
    }

    boolean isTrusted(String ip) {
        byte[] addr = toBytes(ip);
        if (addr == null) {
            return false;
        }
        for (Cidr cidr : trustedProxies) {
            if (cidr.matches(addr)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidIp(String ip) {
        return ip != null && !ip.isEmpty() && toBytes(ip) != null;
    }

    /** Parses an IP literal without DNS lookups; returns null for anything else. */
    private static byte[] toBytes(String ip) {
        if (ip == null || ip.isEmpty()) {
            return null;
        }
        // Strict pre-checks guarantee InetAddress never falls back to a DNS lookup
        if (ip.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            for (String octet : ip.split("\\.")) {
                if (Integer.parseInt(octet) > 255) {
                    return null;
                }
            }
        } else if (ip.indexOf(':') < 0 || !ip.matches("[0-9a-fA-F:.]+")) {
            return null;
        }
        try {
            return InetAddress.getByName(ip).getAddress();
        } catch (Exception e) {
            return null;
        }
    }

    private static List<Cidr> parse(String spec) {
        List<Cidr> result = new ArrayList<>();
        if (spec == null) {
            return result;
        }
        for (String part : spec.split(",")) {
            String s = part.trim();
            if (s.isEmpty()) {
                continue;
            }
            String[] kv = s.split("/");
            byte[] network = toBytes(kv[0]);
            if (network == null) {
                LOG.warn("Ignoring invalid trusted proxy entry: {}", s);
                continue;
            }
            int prefix = kv.length > 1 ? Integer.parseInt(kv[1]) : network.length * 8;
            result.add(new Cidr(network, prefix));
        }
        return result;
    }

    private record Cidr(byte[] network, int prefix) {
        boolean matches(byte[] addr) {
            if (addr.length != network.length) {
                return false;
            }
            int fullBytes = prefix / 8;
            for (int i = 0; i < fullBytes; i++) {
                if (addr[i] != network[i]) {
                    return false;
                }
            }
            int rem = prefix % 8;
            if (rem == 0) {
                return true;
            }
            int mask = (0xFF << (8 - rem)) & 0xFF;
            return (addr[fullBytes] & mask) == (network[fullBytes] & mask);
        }
    }
}
