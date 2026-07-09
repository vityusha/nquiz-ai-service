package com.lainlab.controller;

import com.lainlab.dto.*;
import com.lainlab.i18n.LocalizationService;
import com.lainlab.model.*;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@Controller("/api/capabilities")
public class CapabilitiesController {

    private static final Logger LOG = LoggerFactory.getLogger(CapabilitiesController.class);

    private final LocalizationService i18n;

    public CapabilitiesController(LocalizationService i18n) {
        this.i18n = i18n;
    }

    @Get
    public CapabilitiesResponse getCapabilities(
            @QueryValue(defaultValue = "en") String lang,
            HttpHeaders headers
    ) {
        LOG.info("Capabilities request received, lang param: {}", lang != null && !lang.isEmpty() ? lang : "not specified");

        Locale locale = resolveLocale(lang, headers);
        LOG.info("Selected locale: {}", locale.toString());

        List<LanguageDescription> languages = Arrays.stream(Language.values())
                .map(t -> new LanguageDescription(
                        t.name(),
                        msg("language." + t.name(), locale),
                        msg("language." + t.name() + ".desc", locale)
                ))
                .toList();
        LOG.debug("Loaded {} languages", languages.size());

        List<QuestionTypeDescription> questionTypes = Arrays.stream(QuestionType.values())
                .map(t -> new QuestionTypeDescription(
                        t.name(),
                        msg("type." + t.name(), locale),
                        msg("type." + t.name() + ".desc", locale)
                ))
                .toList();
        LOG.debug("Loaded {} question types", questionTypes.size());

        List<DifficultyDescription> difficulties = Arrays.stream(Difficulty.values())
                .map(d -> new DifficultyDescription(
                        d.name(),
                        msg("difficulty." + d.name(), locale),
                        msg("difficulty." + d.name() + ".desc", locale)
                ))
                .toList();
        LOG.debug("Loaded {} difficulty levels", difficulties.size());

        List<ModeDescription> modes = Arrays.stream(Mode.values())
                .map(m -> new ModeDescription(
                        m.name(),
                        msg("mode." + m.name(), locale),
                        msg("mode." + m.name() + ".desc", locale)
                ))
                .toList();
        LOG.debug("Loaded {} modes", modes.size());

        CapabilitiesResponse response = new CapabilitiesResponse(
                languages,
                questionTypes,
                difficulties,
                modes,
                Arrays.asList(Provider.values()),
                QuestionRequest.MAX_QUESTIONS_COUNT
        );

        LOG.info("Successfully built capabilities response for locale: {}", locale);
        return response;
    }

    private String msg(String key, Locale locale) {
        String message = i18n.get(key, locale);
        LOG.trace("Localized message [{}] for locale [{}]: {}", key, locale, message);
        return message;
    }


    /**
     * Priority:
     * 1) ?lang=... (any language tag)
     * 2) Accept-Language header (any supported language)
     * 3) fallback = en
     */
    private Locale resolveLocale(String lang, HttpHeaders headers) {

        // 1) Query parameter overrides everything
        if (lang != null && !lang.isBlank()) {
            LOG.debug("Resolving locale from query parameter: {}", lang);
            return Locale.of(lang.toLowerCase());
        }

        // 2) Accept-Language — accept any language tag
        Optional<Locale> headerLocale = headers.findFirst(HttpHeaders.ACCEPT_LANGUAGE)
                .map(headerValue -> {
                    LOG.debug("Accept-Language header found: {}", headerValue);
                    return Locale.LanguageRange.parse(headerValue);
                })
                .flatMap(ranges -> ranges.stream()
                        .map(range -> Locale.forLanguageTag(range.getRange()))
                        .findFirst()
                );

        if (headerLocale.isPresent()) {
            Locale l = headerLocale.get();
            LOG.debug("Resolved locale from Accept-Language header: {}", l.getLanguage());
            return l;
        }

        // 3) Default fallback
        LOG.debug("Using default locale: en");
        return Locale.ENGLISH;
    }
}
