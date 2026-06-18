package com.lainlab.filter;

import com.lainlab.db.Token;
import com.lainlab.db.TokenRepository;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.RequestFilter;
import io.micronaut.http.annotation.ServerFilter;
import io.micronaut.http.exceptions.HttpStatusException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton; // КРИТИЧЕСКИ ВАЖНЫЙ ИМПОРТ
import java.util.Optional;

@Singleton // <- ОБЯЗАТЕЛЬНО: без этого Micronaut 4 не создаст бин фильтра!
@ServerFilter("/api/**")
public class TokenAuthFilter {

    @Inject
    TokenRepository tokens;

    @RequestFilter
    public void doFilter(HttpRequest<?> request) {
        String auth = request.getHeaders().get("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new HttpStatusException(HttpStatus.UNAUTHORIZED, "Missing Authorization header!");
        }

        String tokenValue = auth.substring("Bearer ".length()).trim();

        Optional<Token> opt = tokens.findByToken(tokenValue);
        if (opt.isEmpty()) {
            throw new HttpStatusException(HttpStatus.UNAUTHORIZED, "Invalid token!");
        }
        Token token = opt.get();

        if (!token.isActive()) {
            throw new HttpStatusException(HttpStatus.UNAUTHORIZED, "Token is inactive!");
        }

        if (token.isAdmin()) {
            return;
        }

        if (token.getBalance() <= 0) {
            throw new HttpStatusException(HttpStatus.UNAUTHORIZED, "Insufficient balance!");
        }

        request.setAttribute("token", token);
    }
}
