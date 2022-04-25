package com.example.slf4jmdcdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@SpringBootApplication
public class Slf4jMdcDemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(Slf4jMdcDemoApplication.class, args);
	}
}
