package com.lainlab.controller;

import com.lainlab.service.PaymentService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Controller("/webhook")
public class PaymentController {

    private static final Logger LOG = LoggerFactory.getLogger(PaymentController.class);

    @Inject
    PaymentService paymentService;

    @Post("/payment")
    public HttpResponse<?> paymentWebhook(@Body String rawJson, io.micronaut.http.HttpHeaders micronautHeaders) {
        try {
            Map<String, String> headers = new HashMap<>();
            micronautHeaders.forEach(entry ->
                headers.put(entry.getKey().toLowerCase(), entry.getValue().isEmpty() ? "" : entry.getValue().get(0))
            );

            paymentService.handleWebhook(rawJson, headers);
            return HttpResponse.ok();
        } catch (SecurityException e) {
            LOG.warn("Webhook rejected: {}", e.getMessage());
            return HttpResponse.status(HttpStatus.FORBIDDEN, "Invalid signature");
        } catch (IllegalArgumentException e) {
            LOG.warn("Webhook rejected (bad payload): {}", e.getMessage());
            return HttpResponse.badRequest(e.getMessage());
        } catch (Exception e) {
            LOG.error("Error processing payment webhook", e);
            return HttpResponse.serverError();
        }
    }
}
