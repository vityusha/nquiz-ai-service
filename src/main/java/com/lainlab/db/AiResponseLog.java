package com.lainlab.db;

import com.lainlab.converter.QuestionRequestConverter;
import com.lainlab.converter.QuestionResponseListConverter;
import com.lainlab.dto.QuestionRequest;
import com.lainlab.dto.QuestionResponseList;
import io.micronaut.data.annotation.*;
import io.micronaut.serde.annotation.Serdeable;
import java.time.Instant;

@Serdeable
@MappedEntity("ai_response_log")
public class AiResponseLog {

    @Id
    @GeneratedValue(GeneratedValue.Type.AUTO)
    private Long id;

    private Long tokenId;
    private String ipAddress;

    @MappedProperty(converter = QuestionRequestConverter.class)
    private QuestionRequest request;

    @MappedProperty(converter = QuestionResponseListConverter.class)
    private QuestionResponseList response;
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTokenId() {
        return tokenId;
    }

    public void setTokenId(Long tokenId) {
        this.tokenId = tokenId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public QuestionRequest getRequest() {
        return request;
    }

    public void setRequest(QuestionRequest request) {
        this.request = request;
    }

    public QuestionResponseList getResponse() {
        return response;
    }

    public void setResponse(QuestionResponseList response) {
        this.response = response;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
