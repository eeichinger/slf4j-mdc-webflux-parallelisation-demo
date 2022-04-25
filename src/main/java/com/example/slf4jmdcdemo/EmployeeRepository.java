package com.example.slf4jmdcdemo;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EmployeeRepository {
    Mono<Employee> findEmployeeById(String id);

    Flux<Employee> findAllEmployees();
}
