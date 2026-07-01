package com.lainlab.i18n;

import jakarta.inject.Singleton;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

@Singleton
public class LocalizationService {

    private final Map<String, Properties> bundles = new HashMap<>();

    public LocalizationService() {
        load("en");
        load("ru");
    }

    private void load(String lang) {
        try (InputStream is = getClass().getResourceAsStream("/i18n/messages_" + lang + ".properties")) {
            Properties p = new Properties();
            p.load(new InputStreamReader(is, StandardCharsets.UTF_8)); // UTF-8 is required
            bundles.put(lang, p);
        } catch (Exception e) {
            throw new RuntimeException("Cannot load i18n for lang=" + lang, e);
        }
    }

    public String get(String key, Locale locale) {
        String lang = locale.getLanguage();
        Properties p = bundles.getOrDefault(lang, bundles.get("en"));
        return p.getProperty(key, "??" + key + "??");
    }
}
