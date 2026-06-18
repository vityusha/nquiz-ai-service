package com.lainlab.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.lainlab.model.Mode;
import io.micronaut.serde.annotation.Serdeable;

import java.util.ArrayList;
import java.util.List;

public class JsonValidator {

    public static List<String> validateQuestionsArray(JsonNode root, Mode mode) {
        List<String> errors = new ArrayList<>();

        // 1. Корневой объект должен содержать "questions"
        if (!root.has("questions")) {
            errors.add("Root JSON must contain field 'questions'");
            return errors;
        }

        JsonNode arr = root.get("questions");

        // 2. "questions" должен быть массивом
        if (!arr.isArray()) {
            errors.add("'questions' must be an array");
            return errors;
        }

        // 3. Массив не должен быть пустым
        if (arr.size() == 0) {
            errors.add("'questions' array must not be empty");
            return errors;
        }

        // 4. Проверяем каждый вопрос
        for (int i = 0; i < arr.size(); i++) {
            JsonNode q = arr.get(i);
            switch (mode) {
                case Mode.ONE_CORRECT, Mode.MULTI_CORRECT -> validateSingleQuestion(q, "questions[" + i + "]", errors);
                case Mode.ORDERING -> validateOrderingQuestion(q, "questions[" + i + "]", errors);
                case Mode.MATCHING -> validateMatchingQuestion(q, "questions[" + i + "]", errors);
            }

        }

        return errors;
    }

    private static void validateSingleQuestion(JsonNode node,
                                               String prefix,
                                               List<String> errors) {

        // question
        if (!node.has("question") || !node.get("question").isTextual()) {
            errors.add(prefix + ": missing or invalid 'question' field");
        }

        // answers
        if (!node.has("answers") || !node.get("answers").isArray()) {
            errors.add(prefix + ": missing or invalid 'answers' array");
            return;
        }

        ArrayNode answers = (ArrayNode) node.get("answers");
        if (answers.size() == 0) {
            errors.add(prefix + ": 'answers' array must not be empty");
            return;
        }

        boolean hasRight = false;

        for (int i = 0; i < answers.size(); i++) {
            JsonNode ans = answers.get(i);

            if (!ans.has("answer") || !ans.get("answer").isTextual()) {
                errors.add(prefix + ".answers[" + i + "]: missing or invalid 'answer'");
            }

            if (!ans.has("right") || !ans.get("right").isBoolean()) {
                errors.add(prefix + ".answers[" + i + "]: missing or invalid 'right'");
            } else if (ans.get("right").asBoolean()) {
                hasRight = true;
            }
        }

        if (!hasRight) {
            errors.add(prefix + ": no answer marked as right=true");
        }
    }

    private static void validateOrderingQuestion(JsonNode node,
                                                 String prefix,
                                                 List<String> errors) {

        // question
        if (!node.has("question") || !node.get("question").isTextual()) {
            errors.add(prefix + ": missing or invalid 'question'");
        }

        // answers
        if (!node.has("answers") || !node.get("answers").isArray()) {
            errors.add(prefix + ": missing or invalid 'answers' array");
            return;
        }

        ArrayNode answers = (ArrayNode) node.get("answers");
        if (answers.size() < 2) {
            errors.add(prefix + ": 'answers' must contain at least 2 items");
            return;
        }

        for (int i = 0; i < answers.size(); i++) {
            JsonNode ans = answers.get(i);

            // must be object
            if (!ans.isObject()) {
                errors.add(prefix + ".answers[" + i + "]: must be an object");
                continue;
            }

            // must contain "answer"
            if (!ans.has("answer") || !ans.get("answer").isTextual()) {
                errors.add(prefix + ".answers[" + i + "]: missing or invalid 'answer'");
            }
        }
    }

    private static void validateMatchingQuestion(JsonNode node,
                                                 String prefix,
                                                 List<String> errors) {

        if (!node.has("question") || !node.get("question").isTextual()) {
            errors.add(prefix + ": missing or invalid 'question'");
        }

        if (!node.has("answers") || !node.get("answers").isArray()) {
            errors.add(prefix + ": missing or invalid 'answers' array");
            return;
        }

        ArrayNode answers = (ArrayNode) node.get("answers");
        if (answers.size() < 2) {
            errors.add(prefix + ": 'answers' must contain at least 2 items");
            return;
        }

        for (int i = 0; i < answers.size(); i++) {
            JsonNode ans = answers.get(i);

            if (!ans.isObject()) {
                errors.add(prefix + ".answers[" + i + "]: must be an object");
                continue;
            }

            if (!ans.has("answer") || !ans.get("answer").isTextual()) {
                errors.add(prefix + ".answers[" + i + "]: missing or invalid 'answer'");
                continue;
            }

            String text = ans.get("answer").asText();
            if (!text.contains(" - ")) {
                errors.add(prefix + ".answers[" + i + "]: must contain ' - ' separator");
            }
        }
    }
}
