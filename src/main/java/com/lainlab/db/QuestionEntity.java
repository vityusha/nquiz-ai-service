package com.lainlab.db;

import io.micronaut.data.annotation.*;
import io.micronaut.data.model.*;
import io.micronaut.serde.annotation.Serdeable;
import java.time.LocalDateTime;

@MappedEntity("questions")
@Serdeable
public class QuestionEntity {

    @Id
    @GeneratedValue
    private Long id;

    @MappedProperty("token_id")
    private Long tokenId;

    private String question;

    @MappedProperty("created_at")
    private LocalDateTime createdAt;

    // STORED generated columns (read-only via json_extract, excluded from INSERT via custom save)
    private String mode;
    private String difficulty;
    private String type;
    private String language;
    private String keywords;

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTokenId() { return tokenId; }
    public void setTokenId(Long tokenId) { this.tokenId = tokenId; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public String getMode() { return mode; }
    public String getDifficulty() { return difficulty; }
    public String getType() { return type; }
    public String getLanguage() { return language; }
    public String getKeywords() { return keywords; }
}
