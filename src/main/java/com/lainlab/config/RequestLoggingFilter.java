package com.lainlab.config;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

@Filter("/**")
public class RequestLoggingFilter implements HttpServerFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        String path = request.getUri().getPath();
        if ("/health".equals(path)) {
            return chain.proceed(request);
        }

        long start = System.currentTimeMillis();
        String method = request.getMethod().name();
        String uri = request.getUri().toString();
        String ip = request.getAttribute("realClientIp", String.class)
            .orElseGet(() -> request.getRemoteAddress().getAddress().getHostAddress());

        return Mono.from(chain.proceed(request)).doOnNext(response -> {
            long duration = System.currentTimeMillis() - start;
            int status = response.getStatus().getCode();
            LOG.info("{} {} {} {} {}ms", ip, method, uri, status, duration);
        });
    }
}
