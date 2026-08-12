package com.lainlab.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lainlab.db.*;
import com.lainlab.dto.*;
import com.lainlab.model.Mode;
import com.lainlab.model.Provider;
import com.lainlab.util.JsonFixer;
import com.lainlab.util.JsonValidator;
import com.lainlab.util.PromptBuilder;
import com.lainlab.util.PromptCache;
import com.lainlab.util.PromptLabels;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.exceptions.HttpStatusException;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class QuestionService {
    @Inject
    TokenRepository tokenRepository;

    @Inject
    AiResponseLogRepository aiResponseLogRepository;

    // Logger
    private static final Logger LOG = LoggerFactory.getLogger(QuestionService.class);

    // Mapper
    private final ObjectMapper mapper = new ObjectMapper();

    // Cache
    public final Cache<String, QuestionResponseList> cache =
            Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofHours(1))
                    .maximumSize(1000)
                    .build();
    public final Cache<String, Set<String>> ipHistory =
            Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofHours(1))
                    .maximumSize(10000)
                    .build();
    public final Cache<String, Map<String, Set<String>>> ipQuestions =
            Caffeine.newBuilder()
                    .expireAfterWrite(Duration.ofHours(1))
                    .maximumSize(10000)
                    .build();

    PromptCache promptCache;

    private final DeepseekProvider deepseekProvider;
    private final OpenAICompatibleProvider openaiProvider;
    private final GeminiProvider geminiProvider;

    public QuestionService(PromptCache promptCache,
                           DeepseekProvider deepseekProvider,
                           OpenAICompatibleProvider openaiProvider,
                           GeminiProvider geminiProvider) {
        this.promptCache = promptCache;
        this.deepseekProvider = deepseekProvider;
        this.openaiProvider = openaiProvider;
        this.geminiProvider = geminiProvider;
    }
    public Publisher<QuestionResponseList> generateReactive(QuestionRequest req, String ip,
                                                            HttpRequest<?> httpRequest) throws Exception {
        LOG.info("Generating questions for IP: {}, provider: {}, language: {}, difficulty: {}, type: {}, count: {}",
                 ip, req.getProvider(), req.getLanguage(), req.getDifficulty(), req.getType(), req.getCount());

        // Validate required fields
        if (req.getProvider() == null || req.getMode() == null || req.getLanguage() == null
                || req.getDifficulty() == null || req.getType() == null) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST,
                    "Missing required fields: provider, mode, language, difficulty, type are all required");
        }

        // Check limits
        if (req.getCount() < 1 || req.getCount() > QuestionRequest.MAX_QUESTIONS_COUNT) {
            LOG.error("Invalid question count requested: {}. Must be between 1 and {}", req.getCount(), QuestionRequest.MAX_QUESTIONS_COUNT);
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Invalid question count requested: " + req.getCount());
        }

        // Check cache
        //
        String key = cacheKey(req);

        Set<String> issued = ipHistory.getIfPresent(ip);
        boolean alreadyIssuedToIp = issued != null && issued.contains(key);

        if (!alreadyIssuedToIp) {
            QuestionResponseList cached = cache.getIfPresent(key);
            if (cached != null) {
                LOG.info("Cache HIT for key {} (IP {}), returning {} questions", key, ip, cached.getQuestions().size());

                reserveGenerationCharge(httpRequest, req);

                ipHistory.asMap()
                        .computeIfAbsent(ip, k -> ConcurrentHashMap.newKeySet())
                        .add(key);

                Mode mode = req.getMode();
                for (QuestionResponse q : cached.getQuestions()) {
                    q.setMode(mode);
                }

                saveLogEntry(httpRequest, ip, req, cached);

                return Publishers.just(cached);
            }
            LOG.info("Cache MISS for key {} (IP {}), going to AI", key, ip);
        } else {
            LOG.info("IP {} already received key {} — forcing new AI call", ip, key);
        }

        List<String> prev = getLastQuestions(ip, key, req.getCount());
        LOG.debug("Retrieved {} previous questions for IP: {}", prev.size(), ip);

        var prompt = buildPrompt(req, prev, req.getCount());

        LLMProvider provider = getProvider(req.getProvider());
        LOG.debug("Calling LLM provider: {}", req.getProvider());

        GenerationCharge charge = reserveGenerationCharge(httpRequest, req);

        // ---------- Reactive call with retry (Micronaut 4 + Reactor) ----------
        return Flux.defer(() -> {
            try {
                return Flux.from(provider.generateReactive(new LLMRequest(prompt, 2048)));
            } catch (Exception e) {
                return Flux.error(e);
            }
        })
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                        .filter(ex -> !(ex instanceof IllegalStateException))
                        .onRetryExhaustedThrow((spec, signal) ->
                                new RuntimeException("LLM provider unavailable after retries: " + signal.failure().getMessage(), signal.failure()))
                )
                .doOnError(e -> releaseGenerationCharge(charge))
                .map(llm -> {
                    LOG.debug("LLM Response received: {}", llm.content().substring(0, Math.min(200, llm.content().length())) + (llm.content().length() > 200 ? "..." : ""));

                    String fixed = JsonFixer.fix(llm.content());
                    LOG.debug("Fixed JSON, new length: {} chars", fixed.length());

                    JsonNode jsonNode;
                    try {
                        jsonNode = mapper.readTree(fixed);
                        LOG.debug("Successfully parsed JSON");
                    } catch (JsonProcessingException e) {
                        LOG.error("JSON parsing error: {}, raw content: {}", e.getMessage(), fixed);
                        throw new RuntimeException(e);
                    }

                    Mode mode = req.getMode();
                    List<String> errors = JsonValidator.validateQuestionsArray(jsonNode, mode);
                    if (!errors.isEmpty()) {
                        LOG.error("JSON validation failed: {}", errors);
                        throw new IllegalArgumentException("Invalid LLM JSON: " + errors);
                    }
                    LOG.debug("JSON validation passed");

                    QuestionResponseList response;
                    try {
                        response = mapper.readValue(fixed, QuestionResponseList.class);
                        LOG.debug("Successfully mapped JSON to QuestionResponseList, questions count: {}", response.getQuestions().size());
                    } catch (JsonProcessingException e) {
                        LOG.error("Error mapping response: {}", e.getMessage());
                        throw new RuntimeException(e);
                    }

                    cache.put(key, response);
                    LOG.debug("Cached response for key: {}", key);

                    for (QuestionResponse q : response.getQuestions()) {
                        saveQuestionForIpAndKey(ip, key, q.getQuestion());
                        q.setMode(mode);
                    }
                    LOG.debug("Saved {} questions to history for IP: {}", response.getQuestions().size(), ip);

                    ipHistory.asMap()
                            .computeIfAbsent(ip, k -> ConcurrentHashMap.newKeySet())
                            .add(key);

                    // Save log entry
                    saveLogEntry(httpRequest, ip, req, response);

                    LOG.info("Successfully generated and returned {} questions for IP: {}", response.getQuestions().size(), ip);
                    return response;
                });
    }

    private PromptBuilder.PromptBundle buildPrompt(QuestionRequest req, List<String> previousQuestions, int count) {
        String nonce = UUID.randomUUID().toString();
        LOG.debug("Building prompt with nonce: {}", nonce);

        String variation = PromptLabels.randomVariation(req.getType());

        StringBuilder prevBlock = new StringBuilder();
        if (previousQuestions != null && !previousQuestions.isEmpty()) {
            int start = Math.max(0, previousQuestions.size() - 2);
            for (int i = start; i < previousQuestions.size(); i++) {
                prevBlock.append("- ").append(previousQuestions.get(i)).append("\n");
            }
        } else {
            prevBlock.append("none\n");
        }

        PromptLabels.Entry diff = PromptLabels.forDifficulty(req.getDifficulty());
        PromptLabels.Entry topic = PromptLabels.forQuestionType(req.getType());

        Map<String, Object> ctx = new HashMap<>();
        ctx.put("nonce", nonce);
        ctx.put("count", count);
        ctx.put("lang", req.getLanguage());
        ctx.put("diffTitle", diff.title());
        ctx.put("diffDesc", diff.description());
        ctx.put("typeTitle", topic.title());
        ctx.put("typeDesc", topic.description());
        ctx.put("prev", prevBlock.toString());
        ctx.put("variation", variation);
        ctx.put("keywords",
            req.getKeywords() != null && !req.getKeywords().isBlank()
                ? req.getKeywords()
                : "none — choose topic freely");

        PromptBuilder.PromptBundle bundle = PromptBuilder.build(
                promptCache.system(req.getMode()),
                promptCache.user(),
                ctx,
                req.getMode()
        );

        LOG.debug("Prompt built successfully for mode: {}", req.getMode());

        return bundle;
    }

    private String cacheKey(QuestionRequest req) {
        String key = req.getCount() + "|" +
                req.getMode() + "|" +
                req.getLanguage() + "|" +
                req.getDifficulty() + "|" +
                req.getType() + "|" +
                req.getKeywords();
        LOG.trace("Generated cache key: {}", key);
        return key;
    }

    private void saveQuestionForIpAndKey(String ip, String key, String question) {
        ipQuestions.asMap()
                .computeIfAbsent(ip, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet())
                .add(question);
        LOG.trace("Saved question for IP: {}, key: {}", ip, key);
    }

    private List<String> getLastQuestions(String ip, String key, int limit) {
        Map<String, Set<String>> byKey = ipQuestions.getIfPresent(ip);
        if (byKey == null) return List.of();

        Set<String> questions = byKey.get(key);
        if (questions == null || questions.isEmpty()) return List.of();

        return questions.stream()
                .skip(Math.max(0, questions.size() - limit))
                .toList();
    }

    public LLMProvider getProvider(Provider provider) {
        return switch (provider) {
            case DEEPSEEK -> deepseekProvider;
            case OPENAI, GROQ -> openaiProvider;
            case GEMINI -> geminiProvider;
        };
    }

    private record GenerationCharge(Long tokenId, int count, boolean active) {
        static GenerationCharge none() {
            return new GenerationCharge(null, 0, false);
        }
    }

    /**
     * Atomically reserves balance before an LLM call. Admin tokens are not charged.
     * On generation failure, call {@link #releaseGenerationCharge}.
     */
    private GenerationCharge reserveGenerationCharge(HttpRequest<?> request, QuestionRequest req) {
        if (request == null) {
            return GenerationCharge.none();
        }

        Token token = request.getAttribute("token", Token.class)
                .orElseThrow(() -> new IllegalStateException("Token missing in request"));

        if (token.isAdmin()) {
            LOG.debug("Skipping balance reservation for admin token {}", token.getId());
            return GenerationCharge.none();
        }

        int count = req.getCount();
        int updated = tokenRepository.chargeBalance(token.getId(), count);
        if (updated == 0) {
            LOG.error("Insufficient balance: need {} questions, token ID: {}", count, token.getId());
            throw new HttpStatusException(HttpStatus.PAYMENT_REQUIRED, "Insufficient balance");
        }

        Token fresh = tokenRepository.findById(token.getId())
                .orElseThrow(() -> new IllegalStateException("Token not found after charge"));
        token.setBalance(fresh.getBalance());
        token.setTotal(fresh.getTotal());

        LOG.info("Balance reserved: {} questions, token ID: {}, new balance: {}",
                count, token.getId(), token.getBalance());

        return new GenerationCharge(token.getId(), count, true);
    }

    private void releaseGenerationCharge(GenerationCharge charge) {
        if (!charge.active()) {
            return;
        }

        int refunded = tokenRepository.refundBalance(charge.tokenId(), charge.count());
        if (refunded == 0) {
            LOG.error("Failed to refund {} questions for token ID {}", charge.count(), charge.tokenId());
        } else {
            LOG.info("Balance refunded: {} questions, token ID: {}", charge.count(), charge.tokenId());
        }
    }

    private void saveLogEntry(HttpRequest<?> request, String ip, QuestionRequest req, QuestionResponseList response) {
        if(request == null)
            return;

        Token token = request.getAttribute("token", Token.class)
            .orElseThrow(() -> new IllegalStateException("Token missing in request"));

        AiResponseLog logEntry = new AiResponseLog();
        logEntry.setTokenId(token.getId());
        logEntry.setIpAddress(ip);
        logEntry.setRequest(req);
        logEntry.setResponse(response);
        // Ensure NOT NULL sorting/queries are consistent even if Micronaut inserts NULLs.
        logEntry.setCreatedAt(Instant.now());
        aiResponseLogRepository.save(logEntry);

        LOG.debug("Log entry was successfully created for: {}",token.getId());
    }

    /*
    User questions database
     */
    @Inject
    private QuestionRepository repository;

    /**
     * POST /api/questions/store
     * {
     *   "questions": [
     *     "question": "{...json...}",
     *     ...
     *   ]
     * }
     */
    public HttpResponse<?> saveQuestion(HttpRequest<?> request, @Body QuestionResponseList body, String ip) {
        try {
            LOG.info("Request for storing questions from IP: {}", ip);

            Optional<Token> token = request.getAttribute("token", Token.class);
            if (token.isEmpty()) {
                return HttpResponse.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing token");
            }

            List<QuestionResponse> questions = body.getQuestions();
            if (questions != null) {
                for (QuestionResponse q : questions) {
                    String questionJson = mapper.writeValueAsString(q);
                    LOG.debug("Save new question to DB: {}", questionJson.substring(0, Math.min(200, questionJson.length())) + (questionJson.length() > 200 ? "..." : ""));
                    repository.insertQuestion(token.get().getId(), questionJson);
                }
            }

            return HttpResponse.ok(Map.of(
                "status", "saved",
                "count", questions != null ? questions.size() : 0)
            );
        } catch (Exception e) {
            return HttpResponse.serverError(Map.of(
                "error", e.getMessage()
            ));
        }
    }
}
