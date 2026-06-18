package com.lainlab.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

@Serdeable
public class QuestionResponseList {
    private List<QuestionResponse> questions;

    public List<QuestionResponse> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionResponse> questions) {
        this.questions = questions;
    }
}
