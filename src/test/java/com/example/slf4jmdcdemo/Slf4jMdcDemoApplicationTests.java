package com.example.slf4jmdcdemo;

import net.jadler.Jadler;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.reactive.context.ConfigurableReactiveWebEnvironment;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.env.MapPropertySource;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.jadler.Jadler.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Slf4jMdcDemoApplicationTests {

	@LocalServerPort
	int port;

	@Autowired
	ConfigurableReactiveWebEnvironment env;

	MapPropertySource testPropertySource = new MapPropertySource(Slf4jMdcDemoApplicationTests.class + ".properties", new HashMap<>());

	@BeforeEach
	void before() {
		initJadler();
		// override backend url for testing
		testPropertySource.getSource().put(WebClientEmployeeRepository.PROP_EMPLOYEE_SERVICE_URL, "http://localhost:" + Jadler.port());
		env.getPropertySources().addFirst(testPropertySource);
	}

	@AfterEach
	void after() {
		env.getPropertySources().remove(testPropertySource.getName());
		Schedulers.resetOnScheduleHooks();
	}

	@Test
	void fetch_single_employee() {
		WebClient webClient = WebClient.create("http://localhost:" + port);

		stubEmployeeServiceResponse("1");

		Mono<Employee> employeeMono = webClient.get()
				.uri("/employees/{id}", "1")
				.retrieve()
				.bodyToMono(Employee.class);

		final Employee employee = employeeMono.block();

		MatcherAssert.assertThat(employee.getId(), Matchers.equalTo("1"));
		MatcherAssert.assertThat(employee.getName(), Matchers.equalTo("Employee 1"));
	}

	@Test
	void fetch_multiple_employees_parallel() {
		register_onScheduleHook();

		WebClient webClient = WebClient.create("http://localhost:" + port);

		stubEmployeeServiceResponse("1");
		stubEmployeeServiceResponse("2");
		stubEmployeeServiceResponse("3");

		List<Employee> employees = new ArrayList<>();

		webClient.get()
				.uri("/employees?ids={ids}", "1,2,3")
				.retrieve()
				.bodyToFlux(Employee.class).toIterable().forEach(employees::add);

		MatcherAssert.assertThat(employees.size(), Matchers.equalTo(3));
	}

	private void register_onScheduleHook() {
		Schedulers.onScheduleHook("", r->{
			final Logger log = LoggerFactory.getLogger(this.getClass());

			log.info("capture mdc");
			final Map<String, String> capturedMdc = MDC.getCopyOfContextMap();

			return new Runnable() {
				public void run() {

					log.info("apply mdc");
					final Map<String, String> oldMdc = MDC.getCopyOfContextMap();
					setMdcSafe(capturedMdc);
					try {
						r.run();
					} finally {
						log.info("restore mdc");
						setMdcSafe(oldMdc);
					}
				}

				private void setMdcSafe(Map<String, String> capturedMdc) {
					if (capturedMdc != null)
						MDC.setContextMap(capturedMdc);
					else
						MDC.clear();
				}
			};
		});
	}

	static private void stubEmployeeServiceResponse(String id) {
		onRequest()
//                .havingMethodEqualTo("PUT")
                .havingPathEqualTo("/employees/" + id)
//                .havingBody(new JsonMatcher(file("/some/1234/segments/20202_PUT_request.json")))
				.respond()
				.withStatus(200)
				.withContentType("application/json")
				.withBody("{ \"id\":\""+ id +"\", \"name\":\"Employee "+ id +"\" }");
	}
}
