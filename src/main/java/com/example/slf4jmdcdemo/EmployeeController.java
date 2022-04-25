package com.example.slf4jmdcdemo;

import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/employees")
public class EmployeeController {
    final Logger log = LoggerFactory.getLogger(this.getClass());

    private final EmployeeRepository employeeRepository;

    public EmployeeController(@NonNull EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/{id}")
    private Mono<Employee> findEmployeeById(@PathVariable String id) {
        return employeeRepository.findEmployeeById(id)
                .doFinally(signalType -> {
                    log.info("finally employeeRepository.findEmployeeById(id)");
                })
                .doOnSubscribe(subscription -> {
                    log.info("begin employeeRepository.findEmployeeById(id)");
                })
                .doOnSuccess(employee -> {
                    log.info("end employeeRepository.findEmployeeById(id)");
                });
    }

    @GetMapping
    private Flux<Employee> findEmployees(@RequestParam String[] ids) {
        return employeeRepository.findEmployees(ids)
                .doFinally(signalType -> {
                    log.info("finally employeeRepository.findEmployees(ids)");
                })
                .doOnSubscribe(subscription -> {
                    log.info("begin employeeRepository.findEmployees(ids)");
                })
                .doOnComplete(() -> {
                    log.info("end employeeRepository.findEmployees(ids)");
                });
    }
}
