package com.example.slf4jmdcdemo;

import lombok.NonNull;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class WebClientEmployeeRepository implements EmployeeRepository {

    public static final String PROP_EMPLOYEE_SERVICE_URL = "employeeservice.url";

    private final PropertyResolver env;
    private final WebClient webClient = WebClient.create();

    public WebClientEmployeeRepository(@NonNull PropertyResolver env) {
        this.env = env;
    }

    @Override
    public Mono<Employee> findEmployeeById(String id) {
        String baseUri = env.getRequiredProperty(PROP_EMPLOYEE_SERVICE_URL);
        return webClient.get()
                .uri(baseUri + "/employees/{id}", "1")
                .retrieve()
                .bodyToMono(Employee.class);
    }

    @Override
    public Flux<Employee> findAllEmployees() {
        return null;
    }
}
