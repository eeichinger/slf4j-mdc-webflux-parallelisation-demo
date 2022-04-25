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

import java.util.function.Function;

@Repository
public class WebClientEmployeeRepository implements EmployeeRepository {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public static final String PROP_EMPLOYEE_SERVICE_URL = "employeeservice.url";

    private final PropertyResolver env;
    private final WebClient webClient = WebClient.builder()
            .apply(MdcHooks.instrument())
            .build();

    @SuppressWarnings("FieldCanBeLocal")
    private final int parallelRails = 2;

    public WebClientEmployeeRepository(@NonNull PropertyResolver env) {
        this.env = env;
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
        final Function<String, Publisher<Employee>> id2employee = id -> findEmployeeById(id).flux();
        return Flux.just(ids)
                .parallel(parallelRails, parallelRails)
                .runOn(MdcHooks.scheduler())
                .flatMap(id2employee)
                .sequential();
    }
}
