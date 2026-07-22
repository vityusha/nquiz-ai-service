package com.lainlab.i18n;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@DisplayName("LocalizationService Tests")
class LocalizationServiceTest {

    @Inject
    LocalizationService localizationService;

    @Test
    @DisplayName("Should return localized string for known language key (en)")
    void shouldReturnEnglishString() {
        String result = localizationService.get("language.ENGLISH", Locale.ENGLISH);
        assertNotNull(result);
        assertFalse(result.startsWith("??"), "Known key should return actual value: " + result);
    }

    @Test
    @DisplayName("Should return localized string for Russian locale")
    void shouldReturnRussianString() {
        String result = localizationService.get("language.ENGLISH", Locale.of("ru"));
        assertNotNull(result);
        assertFalse(result.startsWith("??"), "Known key should return actual value for Russian: " + result);
    }

    @Test
    @DisplayName("Should return fallback for unknown key")
    void shouldReturnFallbackForUnknownKey() {
        String result = localizationService.get("nonexistent.key.that.does.not.exist", Locale.ENGLISH);
        assertNotNull(result);
        assertTrue(result.startsWith("??"), "Unknown key should return ??key?? format: " + result);
        assertTrue(result.endsWith("??"), "Unknown key should end with ??: " + result);
    }

    @Test
    @DisplayName("Should return different values for different locales")
    void shouldReturnDifferentForDifferentLocales() {
        String en = localizationService.get("language.ENGLISH", Locale.ENGLISH);
        String ru = localizationService.get("language.ENGLISH", Locale.of("ru"));
        assertNotNull(en);
        assertNotNull(ru);
        assertNotEquals(en, ru, "English and Russian should have different translations");
    }

    @Test
    @DisplayName("Should handle difficulty A1 key")
    void shouldHandleDifficultyKey() {
        String result = localizationService.get("difficulty.A1", Locale.ENGLISH);
        assertNotNull(result);
        assertFalse(result.startsWith("??"), "Difficulty A1 key should be localized: " + result);
    }

    @Test
    @DisplayName("Should handle type GRAMMAR key")
    void shouldHandleTypeKey() {
        String result = localizationService.get("type.GRAMMAR", Locale.ENGLISH);
        assertNotNull(result);
        assertFalse(result.startsWith("??"), "Type GRAMMAR key should be localized: " + result);
    }

    @Test
    @DisplayName("Should handle mode key")
    void shouldHandleModeKey() {
        String result = localizationService.get("mode.ONE_CORRECT", Locale.ENGLISH);
        assertNotNull(result);
        assertFalse(result.startsWith("??"), "Mode ONE_CORRECT key should be localized: " + result);
    }
}
