package com.lainlab.i18n;

import com.lainlab.config.UTF8Control;
import jakarta.inject.Singleton;

import java.util.Locale;
import java.util.ResourceBundle;

@Singleton
public class LocalizationService {

    private static final String BASE_NAME = "i18n.messages";
    private static final UTF8Control CONTROL = new UTF8Control();

    public String get(String key, Locale locale) {
        ResourceBundle bundle = ResourceBundle.getBundle(BASE_NAME, locale, CONTROL);
        return bundle.containsKey(key) ? bundle.getString(key) : "??" + key + "??";
    }
}
