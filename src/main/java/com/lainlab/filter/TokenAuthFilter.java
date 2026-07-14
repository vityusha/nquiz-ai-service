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

@Singleton // Required for Micronaut 4 to register the filter bean
@ServerFilter("/api/**")
public class TokenAuthFilter {

    private static final Logger LOG = LoggerFactory.getLogger(TokenAuthFilter.class);

    @Inject
    TokenRepository tokens;

    @RequestFilter
    public void doFilter(HttpRequest<?> request) {
        String path = request.getUri().getPath();
        if ("/api/capabilities".equals(path)) {
            return;
        }
        String auth = request.getHeaders().get("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            LOG.warn("Missing or invalid Authorization header on {}", request.getUri());
            throw new HttpStatusException(HttpStatus.UNAUTHORIZED, "Missing Authorization header!");
        }

        String tokenValue = auth.substring("Bearer ".length()).trim();

        Optional<Token> opt = tokens.findByToken(tokenValue);
        if (opt.isEmpty()) {
            LOG.warn("Invalid token attempt on {}", request.getUri());
            throw new HttpStatusException(HttpStatus.UNAUTHORIZED, "Invalid token!");
        }
        Token token = opt.get();

        if (!token.isActive()) {
            LOG.warn("Inactive token {} used on {}", token.getId(), request.getUri());
            throw new HttpStatusException(HttpStatus.UNAUTHORIZED, "Token is inactive!");
        }

        if (token.isAdmin()) {
            return;
        }

        if (token.getBalance() <= 0) {
            LOG.warn("Token {} has insufficient balance on {}", token.getId(), request.getUri());
            throw new HttpStatusException(HttpStatus.UNAUTHORIZED, "Insufficient balance!");
        }

        request.setAttribute("token", token);
    }
}
