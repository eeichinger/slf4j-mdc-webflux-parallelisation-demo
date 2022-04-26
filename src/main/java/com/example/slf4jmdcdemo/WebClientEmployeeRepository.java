package com.example.slf4jmdcdemo;

import lombok.NonNull;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.function.Function;

@Repository
public class WebClientEmployeeRepository implements EmployeeRepository {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public static final String PROP_EMPLOYEE_SERVICE_URL = "backend.employeeServiceClient.url";
//    public static final String PROP_EMPLOYEE_SERVICE_TIMEOUT = "backend.employeeservice.timeout";

    private final PropertyResolver env;
    private final WebClient webClient;

    @SuppressWarnings("FieldCanBeLocal")
    private final int parallelRails = 2;
    private final Scheduler scheduler;

    public WebClientEmployeeRepository(@NonNull PropertyResolver env, @NonNull WebClientFactory webClientFactory) {
        this.env = env;
        this.scheduler = Schedulers.newParallel("employeeServiceProcessor", parallelRails);
        this.webClient = webClientFactory.create("employeeServiceClient")
/*
                // we could override webClient settings like this:
                .mutate()
                .filter((request, next)->{
                    return next.exchange(request)
                            .timeout(Duration.ofMillis(env.getProperty(PROP_EMPLOYEE_SERVICE_TIMEOUT, Long.class, 10000L)))
                            ;
                })
                .build()
*/
        ;
    }

    @Override
    public Mono<Employee> findEmployeeById(String id) {
        // resolve backend url for testing "at runtime", so we pick up changes at runtime
        String baseUri = env.getRequiredProperty(PROP_EMPLOYEE_SERVICE_URL);

        final String uri = baseUri + "/employees/" + id;
        log.info("submitting request to backend uri=" + uri);
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(Employee.class)
//                .log()
                .doOnNext(employee -> {
                    log.info("received response from backend uri=" + uri);
                })
                .doFinally(signalType -> {
                    log.info("finally response from backend uri=" + uri);
                });
    }

    @Override
    public Flux<Employee> findEmployees(String[] ids) {
        final Flux<String> idsFlux = Flux.just(ids);

        // run either sequential or parallel to compare
//        return run(idsFlux, id -> findEmployeeById(id));
        return runParallel(idsFlux, id -> findEmployeeById(id));
    }

    private Flux<Employee> run(Flux<String> ids, Function<String, Publisher<Employee>> findEmployeeById) {
        return ids
                .flatMap(findEmployeeById);
    }

    private Flux<Employee> runParallel(Flux<String> ids, Function<String, Publisher<Employee>> findEmployeeById) {
        return ids
                // only for demo - parallelisation is actually not needed in our case because all backend calls
                // are performed NIO (and thus on separate threads) anyway
                .parallel(parallelRails, parallelRails)
                .runOn(scheduler)
                .flatMap(findEmployeeById)
                .sequential()
                ;
    }
}
