package com.lainlab.config;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.MutableHttpResponse;
import io.micronaut.http.annotation.Filter;
import io.micronaut.http.filter.HttpServerFilter;
import io.micronaut.http.filter.ServerFilterChain;
import org.reactivestreams.Publisher;

@Filter("/**") // Перехватываем ВСЕ запросы
public class RealIpFilter implements HttpServerFilter {

    @Override
    public Publisher<MutableHttpResponse<?>> doFilter(HttpRequest<?> request, ServerFilterChain chain) {
        // 1. Ищем реальный IP в заголовке, который прислал Nginx
        String realIp = String.valueOf(request.getHeaders().getFirst("X-Real-IP"));

        // 2. Если X-Real-IP нет (например, при локальном запуске), ищем в X-Forwarded-For
        if (realIp == null || realIp.isEmpty() || realIp.equalsIgnoreCase("unknown")) {
            String xForwardedFor = String.valueOf(request.getHeaders().getFirst("X-Forwarded-For"));
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                // X-Forwarded-For может содержать цепочку IP: "клиент, прокси1, прокси2"
                // Берем самый первый (самый левый) IP - это и есть реальный клиент
                realIp = xForwardedFor.split(",")[0].trim();
            }
        }

        // 3. Если мы нашли реальный IP, кладем его в безопасный атрибут запроса
        if (realIp != null && !realIp.isEmpty() && !realIp.equalsIgnoreCase("unknown")) {
            request.setAttribute("realClientIp", realIp);
        }

        // 4. Отправляем запрос дальше по цепочке (в контроллер)
        return chain.proceed(request);
    }
}
