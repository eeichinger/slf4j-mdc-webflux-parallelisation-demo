package com.example.slf4jmdcdemo;

import net.jadler.Jadler;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.reactive.context.ConfigurableReactiveWebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;

import static net.jadler.Jadler.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Slf4jMdcDemoApplicationTests {

	@LocalServerPort
	int port;

//	@Autowired
//	WebTestClient webTestClient;

	@Autowired
	ConfigurableReactiveWebEnvironment env;

	MapPropertySource testPropertySource = new MapPropertySource(Slf4jMdcDemoApplicationTests.class + ".properties", new HashMap<>());

	@BeforeEach
	void before() {
		initJadler();
		testPropertySource.getSource().put(WebClientEmployeeRepository.PROP_EMPLOYEE_SERVICE_URL, "http://localhost:" + Jadler.port());
		env.getPropertySources().addFirst(testPropertySource);
	}

	@AfterEach
	void after() {
		env.getPropertySources().remove(testPropertySource.getName());
	}

	@Test
	void smoke() {
		WebClient webClient = WebClient.create("http://localhost:" + port);

		onRequest()
//                .havingMethodEqualTo("PUT")
//                .havingPathEqualTo("/AWS/lgt/advisory" + "/investmentPropositions/1234/segments/20202")
//                .havingBody(new JsonMatcher(file("/com/edorasware/prdocm/aws/advisory/investmentPropositions/1234/segments/20202_PUT_request.json")))
				.respond()
				.withStatus(200)
				.withContentType("application/json")
				.withBody("{ \"id\":\"1\", \"name\":\"Employee 1\" }");

		Mono<Employee> employeeMono = webClient.get()
				.uri("/employees/{id}", "1")
				.retrieve()
				.bodyToMono(Employee.class);

		final Employee employee = employeeMono.block();

		MatcherAssert.assertThat(employee.getId(), Matchers.equalTo("1"));
		MatcherAssert.assertThat(employee.getName(), Matchers.equalTo("Employee 1"));
	}
}
