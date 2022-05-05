package com.example.slf4jmdcdemo;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.PropertyResolver;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@Configuration
public class AppConfiguration {

    @Bean
    WebClientFactory webClientFactory(PropertyResolver propertyResolver) {
        return new WebClientFactory(propertyResolver);
    }

    @Bean
    @Qualifier(WebClientEmployeeRepository.NAME)
    WebClient employeeWebClient(WebClientFactory webClientFactory) {
        return webClientFactory.create(WebClientEmployeeRepository.NAME);
    }

    @Bean
    @Qualifier(WebClientEmployeeRepository.NAME)
    FluxParallelisationStrategy<String, Employee> employeeFluxParallelisationStrategy() {

        final int parallelRails = 2;
        Scheduler scheduler = Schedulers.newParallel("employeeServiceProcessor", parallelRails);

        return (flux, mapper) -> flux
                .parallel(parallelRails, parallelRails)
                .runOn(scheduler, parallelRails)
                .flatMap(mapper)
                .sequential();
    }
}
