package com.lainlab.controller;

import com.lainlab.db.QuestionEntity;
import com.lainlab.db.QuestionRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.*;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.serde.ObjectMapper;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

@Singleton
@Controller("/search")
public class QuestionSearchController {

    @Inject
    private QuestionRepository repository;

    @Inject
    private ObjectMapper objectMapper;

    private final PebbleEngine pebble = new PebbleEngine.Builder().build();

    @Get(produces = MediaType.TEXT_HTML)
    public HttpResponse<String> searchPage(
        @QueryValue(defaultValue = "") String mode,
        @QueryValue(defaultValue = "") String difficulty,
        @QueryValue(defaultValue = "") String type,
        @QueryValue(defaultValue = "") String language,
        @QueryValue(defaultValue = "") String keywords,
        @QueryValue(defaultValue = "0") int page
    ) {
        try {
            var modes = repository.findDistinctModes();
            var difficulties = repository.findDistinctDifficulties();
            var types = repository.findDistinctTypes();
            var languages = repository.findDistinctLanguages();

            String modesJson = objectMapper.writeValueAsString(modes);
            String difficultiesJson = objectMapper.writeValueAsString(difficulties);
            String typesJson = objectMapper.writeValueAsString(types);
            String languagesJson = objectMapper.writeValueAsString(languages);

            List<QuestionEntity> results = List.of();
            long total = 0;
            boolean hasFilters = !mode.isEmpty() || !difficulty.isEmpty() || !type.isEmpty() || !language.isEmpty() || !keywords.isEmpty();

            if (hasFilters) {
                Page<QuestionEntity> pageResult = repository.search(mode, difficulty, type, language, keywords, Pageable.from(page, 20));
                results = pageResult.getContent();
                total = pageResult.getTotalSize();
            }

            String questionsJson = objectMapper.writeValueAsString(results);
            int totalPages = (int) ((total + 19) / 20);

            PebbleTemplate compiled = pebble.getTemplate("templates/search.html.peb");

            Writer writer = new StringWriter();
            compiled.evaluate(writer, Map.ofEntries(
                Map.entry("total", total),
                Map.entry("totalPages", totalPages),
                Map.entry("currentPage", page),
                Map.entry("modesJson", modesJson),
                Map.entry("difficultiesJson", difficultiesJson),
                Map.entry("typesJson", typesJson),
                Map.entry("languagesJson", languagesJson),
                Map.entry("selectedModeJson", jsonString(mode)),
                Map.entry("selectedDifficultyJson", jsonString(difficulty)),
                Map.entry("selectedTypeJson", jsonString(type)),
                Map.entry("selectedLanguageJson", jsonString(language)),
                Map.entry("questionsJson", questionsJson),
                Map.entry("keywordsEscaped", escapeHtmlAttr(keywords))
            ));

            return HttpResponse.ok(writer.toString()).contentType(MediaType.TEXT_HTML);
        } catch (Exception e) {
            throw new HttpStatusException(io.micronaut.http.HttpStatus.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }

    private String jsonString(String s) {
        if (s == null || s.isEmpty()) return "\"\"";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String escapeHtmlAttr(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> { sb.append('&'); sb.append("amp;"); }
                case '<' -> { sb.append('&'); sb.append("lt;"); }
                case '>' -> { sb.append('&'); sb.append("gt;"); }
                case '"' -> { sb.append('&'); sb.append("quot;"); }
                case '\'' -> { sb.append('&'); sb.append("#39;"); }
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
