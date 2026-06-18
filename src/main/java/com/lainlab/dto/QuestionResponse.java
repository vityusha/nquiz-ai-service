package com.lainlab.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.lainlab.model.Mode;
import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

@Serdeable
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionResponse {
    private Mode mode;
    private String question;
    private String difficulty;
    private String type;
    private String language;
    private List<Answer> answers;

    // getters/setters
    //
    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<Answer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<Answer> answers) {
        this.answers = answers;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @Serdeable
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Answer {
        private String answer;
        private boolean right;

        // getters/setters
        //
        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }

        public boolean isRight() {
            return right;
        }

        public void setRight(boolean right) {
            this.right = right;
        }
    }
}
