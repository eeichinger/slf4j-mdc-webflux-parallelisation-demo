package com.example.slf4jmdcdemo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

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

    private static void setMdcSafe(Map<String, String> capturedMdc) {
        if (capturedMdc != null)
            MDC.setContextMap(capturedMdc);
        else
            MDC.clear();
    }
}
