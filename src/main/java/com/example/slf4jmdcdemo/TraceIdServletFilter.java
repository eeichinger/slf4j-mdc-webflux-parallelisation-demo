package com.example.slf4jmdcdemo;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@Order(1)
public class TraceIdServletFilter implements WebFilter {

    final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String field_requestId = "request_id";
    private static final String requestIdPrefix = "req-";

    @Override
    public Mono<Void> filter(ServerWebExchange serverWebExchange,
                             WebFilterChain webFilterChain) {

        final String traceId = beginRequest();
        serverWebExchange.getResponse()
                .getHeaders().add("X-traceid", traceId);

        log.info("creating request");
        return webFilterChain.filter(serverWebExchange)
                .doOnSubscribe(sub->{
                    log.info("begin request");
                })
                .doOnError(Exception.class, ex->{
                    log.error("end request", ex);
                })
                .doFinally(signal->{
                    log.info("end request");
                });
    }

    static String beginRequest() {
        MDC.clear();
        final String requestId = generateRequestId();

        setRequestId(requestId);
        return requestId;
    }

    static void endRequest() {
    }

    private static void setRequestId(@NonNull String requestId) {
        if (isNullOrEmpty(requestId)) {
            throw new IllegalArgumentException("requestId must not be empty");
        }
        MDC.put(field_requestId, requestId);
    }

    /**
     * Generate a UUID for this particular request
     */
    private static String generateRequestId() {
        return requestIdPrefix + UUID.randomUUID().toString();
    }

    private static boolean isNullOrEmpty(String s) {
        return s == null || s.isEmpty();
    }
}
