package com.lainlab.config;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import jakarta.inject.Inject;
import org.reactivestreams.Publisher;

@Filter("/**") // Intercept all requests
public class RealIpFilter implements HttpServerFilter {

    @Inject
    ClientIpResolver ipResolver;

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        // Forwarding headers are trusted only from known proxies (see ClientIpResolver)
        request.setAttribute("realClientIp", ipResolver.resolve(request));
        return chain.proceed(request);
    }
}
