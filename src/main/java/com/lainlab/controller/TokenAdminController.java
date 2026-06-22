package com.lainlab.controller;

import com.lainlab.db.Token;
import com.lainlab.db.TokenRepository;
import com.lainlab.dto.CreateTokenRequest;
import com.lainlab.dto.TopUpRequest;
import com.lainlab.service.PaymentService;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Controller("/admin/tokens")
public class TokenAdminController {

    private static final Logger LOG = LoggerFactory.getLogger(TokenAdminController.class);

    @Inject
    TokenRepository tokens;

    @Post("/create-user")
    public HttpResponse<?> createToken(@Body CreateTokenRequest req,
                                       HttpRequest<?> httpRequest) {
        LOG.info("Creating new user token for license: {}, org: {}", req.getLicenseNo(), req.getLicenseOrg());

        // Проверка admin-токена
        Token admin = httpRequest.getAttribute("token", Token.class)
                .orElseThrow(() -> new RuntimeException("Missing admin token"));

        if (!admin.isAdmin()) {
            LOG.warn("Unauthorized token creation attempt, token ID: {}", admin.getId());
            return HttpResponse.unauthorized();
        }

        if (req.getLicenseNo() == 0) {
            LOG.warn("Token creation failed: licenseNo is required");
            return HttpResponse.badRequest("licenseOrg is required");
        }
        if (req.getLicenseOrg() == null || req.getLicenseOrg().trim().isEmpty()) {
            LOG.warn("Token creation failed: licenseOrg is required");
            return HttpResponse.badRequest("licenseOrg is required");
        }
        if (req.getEmail() == null || req.getEmail().trim().isEmpty()) {
            LOG.warn("Token creation failed: email is required");
            return HttpResponse.badRequest("email is required");
        }

        Token token = new Token();
        token.setToken(generateApiKey(false));
        token.setLicenseNo(req.getLicenseNo());
        token.setLicenseOrg(req.getLicenseOrg().trim());
        token.setEmail(req.getEmail().trim());
        token.setBalance(req.getBalance());
        token.setTotal(0);
        token.setActive(true);
        token.setAdmin(req.isAdmin());
        token.setCreatedAt(LocalDateTime.now());

        tokens.save(token);

        LOG.info("Successfully created user token, ID: {}, license: {}, email: {}",
                 token.getId(), token.getLicenseNo(), token.getEmail());

        return HttpResponse.created(token);
    }


    @Post("/create-admin")
    public HttpResponse<?> createAdminToken(@Body CreateTokenRequest req,
                                           HttpRequest<?> httpRequest) {
        LOG.info("Creating new admin token");

        Token admin = httpRequest.getAttribute("token", Token.class)
                .orElseThrow(() -> new RuntimeException("Missing admin token"));

        if (!admin.isAdmin()) {
            LOG.warn("Unauthorized admin token creation attempt, token ID: {}", admin.getId());
            return HttpResponse.unauthorized();
        }

        Token token = new Token();
        token.setAdmin(true);
        token.setToken(generateApiKey(true));
        token.setLicenseNo(0);
        token.setLicenseOrg("NQuiz-AI-service Admin");
        token.setEmail("root@localhost");
        token.setBalance(0);
        token.setTotal(0);
        token.setActive(true);
        token.setCreatedAt(LocalDateTime.now());

        tokens.save(token);

        LOG.info("Successfully created admin token, ID: {}", token.getId());
        return HttpResponse.created(token);
    }

    @Post("/deactivate/{id}")
    public HttpResponse<?> deactivateToken(@PathVariable Long id) {
        LOG.info("Deactivating token with ID: {}", id);

        return tokens.findById(id)
                .map(token -> {
                    token.setActive(false);
                    tokens.update(token);
                    LOG.info("Successfully deactivated token, ID: {}, email: {}", id, token.getEmail());
                    return HttpResponse.ok(token);
                })
                .orElse(HttpResponse.notFound());
    }

    @Get("/all")
    public Iterable<Token> listAllTokens() {
        LOG.debug("Fetching all tokens");
        Iterable<Token> allTokens = tokens.findAll();
        LOG.debug("Retrieved all tokens");
        return allTokens;
    }

    public static String generateApiKey(boolean admin) {
        String prefix = admin ? "nq_admin_" : "nq_user_";
        String random = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(SecureRandom.getSeed(32));
        return prefix + random;
    }

    @Inject
    PaymentService paymentService;

    // -----------------------------
    // 1) Top Up by hand
    // -----------------------------
    @Post("/{tokenValue}/topup")
    public HttpResponse<?> topUp(
            @PathVariable String tokenValue,
            @Body TopUpRequest req,
            HttpRequest<?> httpRequest
    ) {
        LOG.info("Processing topup for token: {}, amount: {}", tokenValue, req.getAmount());

        Token token = tokens.findByToken(tokenValue)
                .orElseThrow(() -> new RuntimeException("Token not found"));

        // Проверка admin-токена (фильтр уже сделал, но продублируем)
        Token admin = httpRequest.getAttribute("token", Token.class)
                .orElseThrow(() -> new RuntimeException("Missing admin token"));

        if (!admin.isAdmin()) {
            LOG.warn("Unauthorized topup attempt by token ID: {}", admin.getId());
            return HttpResponse.unauthorized();
        }

        Token updated = paymentService.topUp(token, req.getAmount());
        LOG.info("Successfully topped up token ID: {}, new balance: {}", token.getId(), updated.getBalance());
        return HttpResponse.ok(updated);
    }

    @Post("/{licenseNo}/topup")
    public HttpResponse<?> topUp(
            @PathVariable int licenseNo,
            @Body TopUpRequest req,
            HttpRequest<?> httpRequest
    ) {
        LOG.info("Processing topup for license: {}, amount: {}", licenseNo, req.getAmount());

        Token token = tokens.findByLicenseNo(licenseNo)
                .orElseThrow(() -> new RuntimeException("Token with specified license No not found"));

        // Проверка admin-токена (фильтр уже сделал, но продублируем)
        Token admin = httpRequest.getAttribute("token", Token.class)
                .orElseThrow(() -> new RuntimeException("Missing admin token"));

        if (!admin.isAdmin()) {
            LOG.warn("Unauthorized topup attempt by token ID: {}", admin.getId());
            return HttpResponse.unauthorized();
        }

        Token updated = paymentService.topUp(token, req.getAmount());
        LOG.info("Successfully topped up license {} (token ID: {}), new balance: {}",
                 licenseNo, token.getId(), updated.getBalance());
        return HttpResponse.ok(updated);
    }

    // -----------------------------------------
    // 2) Top Up by payments service (Stripe/YK)
    // -----------------------------------------
    @Post("/webhook")
    public HttpResponse<?> paymentWebhook(@Body String rawJson) {
        LOG.info("Received payment webhook");
        try {
            paymentService.handleWebhook(rawJson);
            LOG.info("Successfully processed payment webhook");
            return HttpResponse.ok();
        } catch (Exception e) {
            LOG.error("Error processing payment webhook", e);
            throw e;
        }
    }
}