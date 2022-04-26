package com.example.slf4jmdcdemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.function.Consumer;

public class MdcHooks {
    private static final Logger log = LoggerFactory.getLogger(MdcHooks.class);

    private static final Scheduler scheduler = Schedulers.parallel();

    public static Scheduler scheduler() {
        return scheduler;
    }

    public static void clear() {
        Schedulers.resetOnScheduleHook("mdc");
    }

    public static void register_onScheduleHook() {
        Schedulers.onScheduleHook("mdc", r -> instrumentWorkerRunnable(r));
    }

    private static Runnable instrumentWorkerRunnable(Runnable r) {
        log.info("capture mdc");
        return new Runnable() {
            final Map<String, String> capturedMdc = MDC.getCopyOfContextMap();

            public void run() {
                final Map<String, String> oldMdc = MDC.getCopyOfContextMap();
                setMdcSafe(capturedMdc);
                log.info("applied mdc");
                try {
                    r.run();
                } finally {
                    log.info("restore mdc");
                    setMdcSafe(oldMdc);
                }
            }
        };
    }

    public static Consumer<WebClient.Builder> instrumentWebClient() {
        return builder -> {
            builder.filters(filters -> {
                filters.add(instrumentExchangeFilter());
            });
        };
    }

    private static ExchangeFilterFunction instrumentExchangeFilter() {
        return (request, next) -> instrumentExchange(next.exchange(request));
    }

    private static Mono<ClientResponse> instrumentExchange(Mono<ClientResponse> exchange) {
        log.info("begin exchange");
//        final AtomicReference<Map<String, String>> oldMdcReference = new AtomicReference<>();
        final Map<String, String> capturedMdc = MDC.getCopyOfContextMap();

        return exchange
                .doOnRequest(l->{
                    log.info("onRequest exchange");
                })
                .doOnSubscribe(subscription -> {
                    log.info("onSubscribe exchange");
                })
                .doOnNext(clientResponse -> {
//                    oldMdcReference.set(MDC.getCopyOfContextMap());
                    setMdcSafe(capturedMdc);
                    log.info("onNext exchange");
                })
                .doOnError(Exception.class, ex -> {
                    log.error("onError exchange", ex);
                })
                .doOnSuccess(clientResponse -> {
                    log.info("onSuccess exchange");
                })
                .doFinally((signalType) -> {
                    log.info("finally exchange");
//                    setMdcSafe(oldMdcReference.get());
                });
    }

    private static void setMdcSafe(Map<String, String> capturedMdc) {
        if (capturedMdc != null)
            MDC.setContextMap(capturedMdc);
        else
            MDC.clear();
    }
}
