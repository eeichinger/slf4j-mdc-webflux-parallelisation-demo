package com.example.slf4jmdcdemo;

import lombok.NonNull;
import org.reactivestreams.Publisher;
import org.springframework.core.env.Environment;
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

    public static final String PROP_EMPLOYEE_SERVICE_URL = "employeeservice.url";

    private final PropertyResolver env;
    private final WebClient webClient = WebClient.create();
    @SuppressWarnings("FieldCanBeLocal")
    private final int parallelRails = 2;
    private final Scheduler scheduler = Schedulers.parallel();

    public WebClientEmployeeRepository(@NonNull PropertyResolver env) {
        this.env = env;
    }

    @Override
    public Mono<Employee> findEmployeeById(String id) {
        // resolve backend url for testing "at runtime", so we pick up changes at runtime
        String baseUri = env.getRequiredProperty(PROP_EMPLOYEE_SERVICE_URL);
        return webClient.get()
                .uri(baseUri + "/employees/{id}", "1")
                .retrieve()
                .bodyToMono(Employee.class);
    }

    @Override
    public Flux<Employee> findEmployees(String[] ids) {
        final Function<String, Publisher<Employee>> id2employee = id -> findEmployeeById(id).flux();
        return Flux.just(ids)
                .parallel(parallelRails, parallelRails)
                .runOn(scheduler)
                .flatMap(id2employee)
                .sequential();
    }
}
