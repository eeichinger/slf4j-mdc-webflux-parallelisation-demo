package com.example.slf4jmdcdemo;

import lombok.NonNull;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.CorePublisher;
import reactor.core.CoreSubscriber;
import reactor.core.publisher.*;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
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
        Schedulers.onScheduleHook("mdc", r -> mdcCapturer(r));
    }

    static Runnable mdcCapturer(Runnable r) {
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

    <T> Consumer<T> wrap(Runnable r) {
        return (T) -> r.run();
    }


    public static Consumer<WebClient.Builder> instrument() {
        return builder -> {
            builder.filters(filters -> {
                filters.add(captureMdcFilter());
            });
        };
    }

    private static ExchangeFilterFunction captureMdcFilter() {
        return (request, next) -> {
            return captureMdc(next.exchange(request));
        };
    }

    static class MdcContextLifter<T> extends BaseSubscriber<T> {
        final CoreSubscriber<T> source;
        final AtomicReference<Map<String, String>> oldMdcReference = new AtomicReference<>();
        final AtomicReference<Map<String, String>> capturedMdcReference = new AtomicReference<>();

        public MdcContextLifter(@NonNull CoreSubscriber<T> source) {
            this.source = source;
        }

        @Override
        protected void hookOnSubscribe(Subscription subscription) {
            capturedMdcReference.set(MDC.getCopyOfContextMap());
            source.onSubscribe(subscription);
            log.info("hookOnSubscribe");
        }

        @Override
        protected void hookOnNext(T value) {
            oldMdcReference.set(MDC.getCopyOfContextMap());
            setMdcSafe(capturedMdcReference.get());
            log.info("hookOnNext");
            source.onNext(value);
        }

        @Override
        protected void hookOnComplete() {
            log.info("hookOnComplete");
            source.onComplete();
        }

        @Override
        protected void hookOnError(Throwable throwable) {
            log.info("hookOnThrowable");
            source.onError(throwable);
        }

        @Override
        protected void hookFinally(SignalType signalType) {
            log.info("hookOnFinally:" + signalType);
//            setMdcSafe(oldMdcReference.get());
        }

        @Override
        public Context currentContext() {
            return source.currentContext();
        }
    }

    static class MonoLogger<I> extends MonoOperator<I, I> {

        /**
         * Build a {@link MonoOperator} wrapper around the passed parent {@link Publisher}
         *
         * @param source the {@link Publisher} to decorate
         */
        public MonoLogger(Mono<? extends I> source) {
            super(source);
        }

        @Override
        public void subscribe(CoreSubscriber<? super I> actual) {
            source.subscribe(actual);
        }


    }

    private static Mono<ClientResponse> captureMdc(Mono<ClientResponse> exchange) {
//        return exchange.publishOn(scheduler());
        log.info("begin exchange");
/*
        return Mono.<ClientResponse, Class>using(
                () -> {
                    log.info("before using");
                    return Void.class;
                }
                , c -> exchange.doFirst(() -> {
                    log.info("do first");
                })
                , c -> {
                    log.info("after using");
                }
        );
*/

/*
        return new Mono<ClientResponse>() {
            @Override
            public void subscribe(CoreSubscriber<? super ClientResponse> actual) {
                exchange.map(r->r).subscribe(new MdcContextLifter<>(actual));
            }
        };
*/
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
