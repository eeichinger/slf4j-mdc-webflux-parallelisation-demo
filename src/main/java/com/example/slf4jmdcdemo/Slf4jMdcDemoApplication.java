package com.example.slf4jmdcdemo;

import io.netty.channel.nio.NioEventLoopGroup;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.client.reactive.ReactorResourceFactory;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication
public class Slf4jMdcDemoApplication {

	@Bean
	ReactorResourceFactory reactorResourceFactory() {
		final int THREADPOOL_SIZE = 0; // use default size
		ReactorResourceFactory f= new ReactorResourceFactory();
		f.setLoopResources(useNative -> new NioEventLoopGroup(THREADPOOL_SIZE, command -> {
			MdcHooks.scheduler().schedule(command);
		}));
		f.setUseGlobalResources(false);
		return f;
	}

	@Bean
	WebClient webClient(ReactorResourceFactory reactorResourceFactory) {
 		return WebClient.builder()
				.clientConnector(new ReactorClientHttpConnector(reactorResourceFactory, client->client))
				.build();
	}

	public static void main(String[] args) {
		SpringApplication.run(Slf4jMdcDemoApplication.class, args);
	}
}
