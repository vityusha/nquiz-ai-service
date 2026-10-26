package com.lainlab.controller;

import com.lainlab.db.QuestionRepository;
import com.lainlab.db.Token;
import com.lainlab.db.TokenRepository;
import com.lainlab.dto.CreateTokenRequest;
import com.lainlab.dto.LicenseStats;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller("/admin/tokens")
public class TokenAdminController {

    private static final Logger LOG = LoggerFactory.getLogger(TokenAdminController.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    @Inject
    TokenRepository tokens;

    @Inject
    QuestionRepository questionRepository;

    @Post("/create-user")
    public HttpResponse<?> createToken(@Body CreateTokenRequest req,
                                       HttpRequest<?> httpRequest) {
        LOG.info("Creating new user token for license: {}, org: {}", req.getLicenseNo(), req.getLicenseOrg());

        // Verify admin token
        Token admin = httpRequest.getAttribute("token", Token.class)
                .orElseThrow(() -> new RuntimeException("Missing admin token"));

        if (!admin.isAdmin()) {
            LOG.warn("Unauthorized token creation attempt, token ID: {}", admin.getId());
            return HttpResponse.unauthorized();
        }

        if (req.getLicenseNo() == 0) {
            LOG.warn("Token creation failed: licenseNo is required");
            return HttpResponse.badRequest("licenseNo is required");
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

    @Post("/deactivate/{license_no}")
    public HttpResponse<?> deactivateToken(@PathVariable int license_no) {
        LOG.info("Deactivating token with license number: {}", license_no);

        return tokens.findByLicenseNo(license_no)
                .map(token -> {
                    token.setActive(false);
                    tokens.update(token);
                    LOG.info("Successfully deactivated token, ID: {}, email: {}", token.getId(), token.getEmail());
                    return HttpResponse.ok(token);
                })
                .orElse(HttpResponse.notFound());
    }

    @Post("/activate/{license_no}")
    public HttpResponse<?> activateToken(@PathVariable int license_no) {
        LOG.info("Activating token with license number: {}", license_no);

        return tokens.findByLicenseNo(license_no)
            .map(token -> {
                token.setActive(true);
                tokens.update(token);
                LOG.info("Successfully activated token, ID: {}, email: {}", token.getId(), token.getEmail());
                return HttpResponse.ok(token);
            })
            .orElse(HttpResponse.notFound());
    }

    @Get("/all")
    public HttpResponse<?> listAllTokens(HttpRequest<?> httpRequest) {
        Token token = httpRequest.getAttribute("token", Token.class)
                .orElseThrow(() -> new RuntimeException("Missing admin token"));
        if (!token.isAdmin()) {
            return HttpResponse.unauthorized();
        }
        return HttpResponse.ok(tokens.findAll());
    }

    @Get("/stats")
    public HttpResponse<?> stats(HttpRequest<?> httpRequest) {
        Token token = httpRequest.getAttribute("token", Token.class)
                .orElseThrow(() -> new RuntimeException("Missing admin token"));
        if (!token.isAdmin()) {
            return HttpResponse.unauthorized();
        }

        long totalActiveLicenseNo = tokens.countDistinctActiveLicenseNo();
        long totalAiRequests = tokens.countAiRequests();
        long totalQuestionsStored = questionRepository.count();
        List<LicenseStats> perLicense = tokens.findLicenseStats();

        return HttpResponse.ok(Map.of(
            "totalActiveLicenseNo", totalActiveLicenseNo,
            "totalAiRequests", totalAiRequests,
            "totalQuestionsStored", totalQuestionsStored,
            "licenses", perLicense
        ));
    }

    @Get("/info/{license_no}")
    public HttpResponse<?> info(@PathVariable int license_no) {
        LOG.debug("Token info requested for license: {}", license_no);

        Optional<Token> opt = tokens.findByLicenseNo(license_no);
        if (opt.isEmpty()) {
            LOG.warn("Token info requested with invalid license");
            return HttpResponse.unauthorized();
        }
        Token token = opt.get();

        if (!token.isActive()) {
            LOG.warn("Token info requested for inactive token, license: {}", token.getLicenseNo());
            return HttpResponse.unauthorized();
        }

        LOG.info("Successfully retrieved token info for license: {}, organization: {}", token.getLicenseNo(), token.getLicenseOrg());

        return HttpResponse.ok(new TokenInfoController.TokenInfoResponse(
            token.getToken(),
            token.getLicenseNo(),
            token.getLicenseOrg(),
            token.getEmail(),
            token.getBalance(),
            token.getTotal(),
            token.isActive(),
            token.getCreatedAt()
        ));
    }

    public static String generateApiKey(boolean admin) {
        String prefix = admin ? "sk_admin_" : "sk_user_";
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String random = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return prefix + random;
    }

    @Inject
    PaymentService paymentService;

    // -----------------------------
    // Top Up by hand
    // -----------------------------
    @Post("/{identifier}/topup")
    public HttpResponse<?> topUp(
            @PathVariable String identifier,
            @Body TopUpRequest req,
            HttpRequest<?> httpRequest
    ) {
        Token admin = httpRequest.getAttribute("token", Token.class)
                .orElseThrow(() -> new RuntimeException("Missing admin token"));

        if (!admin.isAdmin()) {
            LOG.warn("Unauthorized topup attempt by token ID: {}", admin.getId());
            return HttpResponse.unauthorized();
        }

        if (req.getAmount() > PaymentService.MAX_TOPUP_AMOUNT) {
            LOG.warn("Topup rejected: amount {} exceeds limit {}", req.getAmount(), PaymentService.MAX_TOPUP_AMOUNT);
            return HttpResponse.badRequest("amount must not exceed " + PaymentService.MAX_TOPUP_AMOUNT);
        }

        Optional<Token> found;
        try {
            int licenseNo = Integer.parseInt(identifier);
            found = tokens.findByLicenseNo(licenseNo);
            LOG.info("Processing topup for license: {}, amount: {}", licenseNo, req.getAmount());
        } catch (NumberFormatException e) {
            found = tokens.findByToken(identifier);
            LOG.info("Processing topup by token value, amount: {}", req.getAmount());
        }
        if (found.isEmpty()) {
            LOG.warn("Topup failed: token not found");
            return HttpResponse.notFound();
        }
        Token token = found.get();

        Token updated = paymentService.topUp(token, req.getAmount());
        LOG.info("Successfully topped up token ID: {}, new balance: {}", token.getId(), updated.getBalance());
        return HttpResponse.ok(updated);
    }

}
