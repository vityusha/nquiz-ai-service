package com.lainlab.exception;

import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Catch-all for unhandled exceptions: logs the details and returns a generic body,
 * so exception messages (internals, upstream LLM errors) never reach the client.
 * More specific handlers (e.g. HttpStatusException) still take precedence.
 */
@Produces
@Singleton
@Requires(classes = {Exception.class, ExceptionHandler.class})
public class GlobalExceptionHandler implements ExceptionHandler<Exception, HttpResponse<?>> {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    public HttpResponse<?> handle(HttpRequest request, Exception exception) {
        LOG.error("Unhandled error on {} {}", request.getMethod(), request.getPath(), exception);
        return HttpResponse.serverError(Map.of("error", "Internal Server Error"));
    }
}
