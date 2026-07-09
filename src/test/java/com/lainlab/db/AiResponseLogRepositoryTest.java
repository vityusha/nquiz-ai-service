package com.lainlab.db;

import com.lainlab.dto.QuestionRequest;
import com.lainlab.dto.QuestionResponse;
import com.lainlab.dto.QuestionResponseList;
import com.lainlab.model.Difficulty;
import com.lainlab.model.Mode;
import com.lainlab.model.Provider;
import com.lainlab.model.QuestionType;
import com.lainlab.model.Language;
import com.lainlab.model.QuestionType;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@DisplayName("AiResponseLogRepository tests")
class AiResponseLogRepositoryTest {

    @Inject
    TokenRepository tokenRepository;

    @Inject
    AiResponseLogRepository aiResponseLogRepository;

    @BeforeEach
    void setUp() {
        aiResponseLogRepository.deleteAll();
        tokenRepository.deleteAll();
    }

    @Test
    @DisplayName("Should save and read AiResponseLog with converters")
    void shouldSaveAndReadAiResponseLog() {
        Token token = new Token();
        token.setToken("nq_user_for_log_test");
        token.setLicenseNo(500);
        token.setLicenseOrg("LogTestOrg");
        token.setEmail("log@test.local");
        token.setBalance(10);
        token.setActive(true);
        token.setAdmin(false);
        token.setCreatedAt(Instant.now().atOffset(java.time.ZoneOffset.UTC).toLocalDateTime());
        tokenRepository.save(token);

        QuestionRequest request = new QuestionRequest();
        request.setProvider(Provider.DEEPSEEK);
        request.setCount(3);
        request.setMode(Mode.ONE_CORRECT);
        request.setLanguage(Language.ENGLISH);
        request.setDifficulty(Difficulty.B1);
        request.setType(QuestionType.GRAMMAR);
        request.setKeywords("present tense");

        QuestionResponse.Answer a1 = new QuestionResponse.Answer();
        a1.setAnswer("Answer 1");
        a1.setRight(true);

        QuestionResponse q1 = new QuestionResponse();
        q1.setQuestion("What is the correct tense?");
        q1.setDifficulty("B1");
        q1.setType("GRAMMAR");
        q1.setLanguage("ENGLISH");
        q1.setAnswers(List.of(a1));
        q1.setMode(Mode.ONE_CORRECT);

        QuestionResponseList response = new QuestionResponseList();
        response.setQuestions(List.of(q1));

        AiResponseLog log = new AiResponseLog();
        log.setTokenId(token.getId());
        log.setIpAddress("127.0.0.1");
        log.setRequest(request);
        log.setResponse(response);
        log.setCreatedAt(Instant.parse("2020-01-01T00:00:00Z"));

        aiResponseLogRepository.save(log);

        List<AiResponseLog> byToken = aiResponseLogRepository.findByTokenIdOrderByCreatedAtDesc(token.getId());
        assertEquals(1, byToken.size(), "Expected exactly one log for token");

        AiResponseLog loaded = byToken.get(0);
        assertEquals("127.0.0.1", loaded.getIpAddress());
        assertNotNull(loaded.getRequest());
        assertNotNull(loaded.getResponse());

        assertEquals(Provider.DEEPSEEK, loaded.getRequest().getProvider());
        assertEquals(3, loaded.getRequest().getCount());
        assertEquals(Mode.ONE_CORRECT, loaded.getRequest().getMode());
        assertEquals(Language.ENGLISH, loaded.getRequest().getLanguage());
        assertEquals(Difficulty.B1, loaded.getRequest().getDifficulty());
        assertEquals(QuestionType.GRAMMAR, loaded.getRequest().getType());
        assertEquals("present tense", loaded.getRequest().getKeywords());

        assertEquals(1, loaded.getResponse().getQuestions().size());
        QuestionResponse loadedQ = loaded.getResponse().getQuestions().get(0);
        assertEquals("What is the correct tense?", loadedQ.getQuestion());
        assertEquals("Answer 1", loadedQ.getAnswers().get(0).getAnswer());

        List<AiResponseLog> byIp = aiResponseLogRepository.findByIpAddress("127.0.0.1");
        assertEquals(1, byIp.size(), "Expected log to be found by IP");
    }
}

