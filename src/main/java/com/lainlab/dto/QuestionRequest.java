package com.lainlab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lainlab.model.Mode;
import com.lainlab.model.Difficulty;
import com.lainlab.model.Language;
import com.lainlab.model.QuestionType;
import com.lainlab.model.Provider;
import io.micronaut.serde.annotation.Serdeable;

@Serdeable
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionRequest {
    public static final int MAX_QUESTIONS_COUNT = 10;

    private Provider provider;
    private int count;
    private Mode mode;
    private Language language;
    private Difficulty difficulty;
    private QuestionType type;
    private String keywords;

    // getters/setters
    //
    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public int getCount() {
        return count < 1 ? 1 : count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Language getLanguage() {
        return language;
    }

    public void setLanguage(Language language) {
        this.language = language;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public QuestionType getType() {
        return type;
    }

    public void setType(QuestionType type) {
        this.type = type;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }
}
