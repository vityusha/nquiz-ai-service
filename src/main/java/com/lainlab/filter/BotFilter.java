package com.lainlab.filter;

import io.micronaut.core.order.Ordered;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.micronaut.http.exceptions.HttpStatusException;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@ServerFilter("/**")
public class BotFilter implements Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(BotFilter.class);

    private static final String[] BLOCKED_PATTERNS = {
            "vendor/phpunit",
            "eval-stdin.php",
            "phpinfo",
            ".env",
            ".git/",
            ".svn/",
            "wp-admin",
            "wp-login",
            "xmlrpc.php",
            "phpmyadmin",
            "admin/config",
            "/config.php",
            "/config.inc.php",
            "joomla/administrator",
            "/shell",
            "/cmd",
            "/console",
            "/containers",
            "dns-query",
    };

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    @RequestFilter
    public void doFilter(HttpRequest<?> request) {
        String path = request.getUri().getPath().toLowerCase();

        for (String pattern : BLOCKED_PATTERNS) {
            if (path.contains(pattern)) {
                LOG.warn("Blocked scanner request: {} {} from {}",
                        request.getMethod(), request.getUri(),
                        request.getRemoteAddress());
                throw new HttpStatusException(HttpStatus.NOT_FOUND, "Not Found");
            }
        }
    }
}
