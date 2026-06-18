package com.lainlab.config;

import io.micronaut.context.MessageSource;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.i18n.ResourceBundleMessageSource;
import jakarta.inject.Singleton;

@Factory
public class MessageSourceFactory {
    @Singleton
    public static MessageSource createMessageSource() {
        return new ResourceBundleMessageSource("i18n.messages");
    }
}
