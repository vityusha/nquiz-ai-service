package com.lainlab.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.lainlab.db.Token;
import com.lainlab.db.TokenRepository;
import com.lainlab.dto.*;
import com.lainlab.model.Mode;
import com.lainlab.model.Provider;
import com.lainlab.util.JsonFixer;
import com.lainlab.util.JsonValidator;
import com.lainlab.util.PromptBuilder;
import com.lainlab.util.PromptCache;
import io.micronaut.core.async.publisher.Publishers;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;

@Singleton
public class QuestionService {
    @Inject
    TokenRepository tokenRepository;

    // Logger
    private static final Logger LOG = LoggerFactory.getLogger(QuestionService.class);
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

                ipHistory.asMap()
                        .computeIfAbsent(ip, k -> new HashSet<>())
                        .add(key);

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

        // ---------- Reactive call (Micronaut 4) ----------
        return Publishers.map(
                provider.generateReactive(new LLMRequest(prompt, 2048)),
                llm -> {
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
                            .computeIfAbsent(ip, k -> new HashSet<>())
                            .add(key);

                    chargeBalance(httpRequest, req);
                    LOG.info("Successfully generated and returned {} questions for IP: {}", response.getQuestions().size(), ip);

                    return response;
                }
        );
    }

    private PromptBuilder.PromptBundle buildPrompt(QuestionRequest req, List<String> previousQuestions, int count) {
        String nonce = UUID.randomUUID().toString();
        LOG.debug("Building prompt with nonce: {}", nonce);

        List<String> variationRules = List.of(
                "different tenses",
                "different structures",
                "different clause types",
                "different vocabulary",
                "different grammar phenomena",
                "different subjects/contexts",
                "different syntax"
        );
        String variation = variationRules.get(new Random().nextInt(variationRules.size()));

        List<String> styles = List.of("formal", "informal", "academic", "business", "narrative");
        String style = styles.get(new Random().nextInt(styles.size()));

        List<String> microTasks = List.of(
                "include an adverb",
                "include an object",
                "include a time expression",
                "make one question longer",
                "use a verb phrase",
                "use a real-life context"
        );
        String microTask = microTasks.get(new Random().nextInt(microTasks.size()));

        StringBuilder prevBlock = new StringBuilder();
        if (previousQuestions != null && !previousQuestions.isEmpty()) {
            for (String q : previousQuestions) {
                prevBlock.append("- ").append(q).append("\n");
            }
        } else {
            prevBlock.append("none\n");
        }

        Map<String, Object> ctx = new HashMap<>();
        ctx.put("nonce", nonce);
        ctx.put("count", count);
        ctx.put("lang", req.getLanguage());
        ctx.put("diff", req.getDifficulty());
        ctx.put("type", req.getType());
        ctx.put("prev", prevBlock.toString());
        ctx.put("variation", variation);
        ctx.put("style", style);
        ctx.put("task", microTask);
        ctx.put("keywords", req.getKeywords());

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
                req.getType();
        LOG.trace("Generated cache key: {}", key);
        return key;
    }

    private void saveQuestionForIpAndKey(String ip, String key, String question) {
        ipQuestions.asMap()
                .computeIfAbsent(ip, k -> new HashMap<>())
                .computeIfAbsent(key, k -> new HashSet<>())
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

    private void chargeBalance(HttpRequest<?> request, QuestionRequest req) {
        if(request == null)
            return;

        Token token = request.getAttribute("token", Token.class)
                .orElseThrow(() -> new IllegalStateException("Token missing in request"));

        int count = req.getCount();
        int currentBalance = token.getBalance();

        LOG.debug("Charging balance: current: {}, required: {}, token ID: {}", currentBalance, count, token.getId());

        if (currentBalance < count) {
            LOG.error("Insufficient balance: need {} questions, have {}, token ID: {}", count, currentBalance, token.getId());
            throw new RuntimeException(
                    "Insufficient balance: need " + count + ", have " + currentBalance
            );
        }

        token.setBalance(currentBalance - count);
        token.setTotal(token.getTotal() + count);
        tokenRepository.update(token);

        LOG.info("Balance charged successfully: {} questions deducted, token ID: {}, new balance: {}, total requested: {}",
                 count, token.getId(), token.getBalance(), token.getTotal());
    }
}
