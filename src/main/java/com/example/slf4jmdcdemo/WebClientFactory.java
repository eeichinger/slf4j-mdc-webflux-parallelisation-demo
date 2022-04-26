package com.example.slf4jmdcdemo;

import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import lombok.NonNull;
import org.springframework.core.env.PropertyResolver;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.client.reactive.ReactorResourceFactory;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

public class WebClientFactory {

    private final PropertyResolver propertyResolver;

    public WebClientFactory(@NonNull PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
    }

    public WebClient create(String name) {
        int threadpoolSize = propertyResolver.getProperty("backend." + name + ".parallelism", Integer.class, Schedulers.DEFAULT_POOL_SIZE);
        return WebClient.builder()
                .baseUrl("backend." + name + ".url")
                .clientConnector(new ReactorClientHttpConnector(reactorResourceFactory(name, threadpoolSize), (HttpClient client) -> {
                    return client
                            // we can set timeouts directly on HttpClient-level
                            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, propertyResolver.getProperty("backend." + name + ".connectTimeoutMillis", Integer.class, 10000))
                            .responseTimeout(Duration.ofMillis(propertyResolver.getProperty("backend." + name + ".responseTimeoutMillis", Long.class, 30000L)))
                            ;
                }))
                .filter((request, next) -> {
                    return next.exchange(request)
                            .log()
                            // alternatively we can set timeouts using reactor - we shouldn't mix it though
                            .timeout(Duration.ofMillis(propertyResolver.getProperty("backend."+name+".timeoutMillis", Long.class, 30000L)))
                            ;
                }).build();
    }

    private ReactorResourceFactory reactorResourceFactory(String name, int threadpoolSize) {
        ReactorResourceFactory f = new ReactorResourceFactory();
        f.setUseGlobalResources(false);

        // apply ConnectionPool limits
        f.setConnectionProviderSupplier(() -> ConnectionProvider
                .builder(name)
                .maxConnections(ConnectionProvider.DEFAULT_POOL_MAX_CONNECTIONS) // ReactorNetty.POOL_MAX_CONNECTIONS
                .maxIdleTime(Duration.ofMillis(ConnectionProvider.DEFAULT_POOL_MAX_IDLE_TIME)) // ReactorNetty.POOL_MAX_IDLE_TIME
                .maxLifeTime(Duration.ofMillis(ConnectionProvider.DEFAULT_POOL_MAX_LIFE_TIME)) // ReactorNetty.POOL_MAX_LIFE_TIME
                .pendingAcquireTimeout(Duration.ofMillis(ConnectionProvider.DEFAULT_POOL_ACQUIRE_TIMEOUT)) // ReactorNetty.POOL_ACQUIRE_TIMEOUT
                .build());

        // use reactor Scheduler to ensure handling of Schedulers#onScheduleHook()
        f.setLoopResourcesSupplier(() -> useNative -> {
            final Scheduler scheduler = Schedulers.newParallel(name, threadpoolSize);
            return new NioEventLoopGroup(threadpoolSize, command -> {
                scheduler.schedule(command);
            });
        });
        f.afterPropertiesSet();
        return f;
    }
}
