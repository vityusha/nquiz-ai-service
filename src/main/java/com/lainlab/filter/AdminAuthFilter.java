package com.lainlab.filter;

import com.lainlab.db.Token;
import com.lainlab.db.TokenRepository;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.micronaut.http.exceptions.HttpStatusException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Singleton // Required to register the class in the DI context
@ServerFilter("/admin/**")
public class AdminAuthFilter {

    private static final Logger LOG = LoggerFactory.getLogger(AdminAuthFilter.class);

    @Inject
    TokenRepository tokens;

    @RequestFilter
    public void doFilter(HttpRequest<?> request) {
        String auth = request.getHeaders().get("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            LOG.warn("Missing or invalid Authorization header on {}", request.getUri());
            throw new HttpStatusException(HttpStatus.UNAUTHORIZED, "Missing Authorization header!");
        }

        String tokenValue = auth.substring("Bearer ".length()).trim();

        Optional<Token> opt = tokens.findByToken(tokenValue);
        if (opt.isEmpty()) {
            LOG.warn("Invalid admin token attempt on {}", request.getUri());
            throw new HttpStatusException(HttpStatus.UNAUTHORIZED, "Invalid token!");
        }
        Token token = opt.get();

        if (!token.isActive() || !token.isAdmin()) {
            LOG.warn("Token {} (active={}, admin={}) rejected on {}", token.getId(), token.isActive(), token.isAdmin(), request.getUri());
            throw new HttpStatusException(HttpStatus.UNAUTHORIZED, "Token is inactive or not admin token!");
        }

        request.setAttribute("token", token);
    }
}
