package com.lainlab;

import com.lainlab.controller.TokenAdminController;
import com.lainlab.service.QuestionService;
import io.micronaut.runtime.Micronaut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Application {
    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        // LOG.info(TokenAdminController.generateApiKey(true));
        Micronaut.run(Application.class, args);
    }
}