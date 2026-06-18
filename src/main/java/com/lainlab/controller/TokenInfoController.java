package com.lainlab.controller;

import com.lainlab.db.Token;
import com.lainlab.db.TokenRepository;
import io.micronaut.http.annotation.*;
import io.micronaut.http.*;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Controller("/api/token")
public class TokenInfoController {

    private static final Logger LOG = LoggerFactory.getLogger(TokenInfoController.class);

    @Inject
    TokenRepository tokens;

    @Get("/info")
    public HttpResponse<?> info(@Header("Authorization") String auth) {

        if (auth == null || !auth.startsWith("Bearer ")) {
            LOG.warn("Token info requested without valid Authorization header");
            return HttpResponse.unauthorized();
        }

        String tokenValue = auth.substring("Bearer ".length()).trim();
        LOG.debug("Token info requested for token: {}", tokenValue.substring(0, Math.min(10, tokenValue.length())) + "...");

        Optional<Token> opt = tokens.findByToken(tokenValue);
        if (opt.isEmpty()) {
            LOG.warn("Token info requested with invalid token");
            return HttpResponse.unauthorized();
        }
        Token token = opt.get();

        if (!token.isActive()) {
            LOG.warn("Token info requested for inactive token, ID: {}", token.getId());
            return HttpResponse.unauthorized();
        }

        LOG.info("Successfully retrieved token info for license: {}, organization: {}", token.getLicenseNo(), token.getLicenseOrg());

        return HttpResponse.ok(new TokenInfoResponse(
                token.getToken(),
                token.getLicenseNo(),
                token.getLicenseOrg(),
                token.getEmail(),
                token.getBalance(),
                token.isActive(),
                token.getCreatedAt()
        ));
    }

    @Serdeable
    public record TokenInfoResponse(
            String token,
            int license_no,
            String license_org,
            String owner,
            int balance,
            boolean active,
            java.time.LocalDateTime createdAt
    ) {}
}
