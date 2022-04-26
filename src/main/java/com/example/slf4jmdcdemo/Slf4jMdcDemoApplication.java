package com.example.slf4jmdcdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.PropertyResolver;

@SpringBootApplication
public class Slf4jMdcDemoApplication {

    @Bean
    WebClientFactory webClientFactory(PropertyResolver env) {
        return new WebClientFactory(env);
    }

    public static void main(String[] args) {
        SpringApplication.run(Slf4jMdcDemoApplication.class, args);
    }
}
