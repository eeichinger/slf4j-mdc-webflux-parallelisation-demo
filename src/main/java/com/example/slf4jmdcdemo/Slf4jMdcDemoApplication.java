package com.example.slf4jmdcdemo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class Slf4jMdcDemoApplication {

	@Bean
	WebClient webClient() {
 		return WebClient.builder()
				.apply(MdcHooks.instrumentWebClient())
				.build();
	}

	public static void main(String[] args) {
		SpringApplication.run(Slf4jMdcDemoApplication.class, args);
	}
}
