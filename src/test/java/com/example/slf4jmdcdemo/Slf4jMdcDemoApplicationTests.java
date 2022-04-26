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

import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static net.jadler.Jadler.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Slf4jMdcDemoApplicationTests {

    @LocalServerPort
    int port;

    @Autowired
    ConfigurableReactiveWebEnvironment env;

    MapPropertySource testPropertySource = new MapPropertySource(Slf4jMdcDemoApplicationTests.class + ".properties", new HashMap<>());

    @BeforeAll
    static void beforeAll() {
        initJadler();
    }

    @AfterAll
    static void afterAll() {
        closeJadler();
    }

    @BeforeEach
    void before() {
        resetJadler();
        MdcHooks.register_onScheduleHook();

        // override backend url for testing
        testPropertySource.getSource().put(WebClientEmployeeRepository.PROP_EMPLOYEE_SERVICE_URL, "http://localhost:" + Jadler.port());
        env.getPropertySources().addFirst(testPropertySource);
    }

    @AfterEach
    void after() {
        env.getPropertySources().remove(testPropertySource.getName());
        MdcHooks.clear();
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
        WebClient webClient = WebClient.create("http://localhost:" + port);

        stubEmployeeServiceResponse("1", Duration.ofSeconds(3));
        stubEmployeeServiceResponse("2", Duration.ofSeconds(3));
        stubEmployeeServiceResponse("3", Duration.ofSeconds(3));

        List<Employee> employees = new ArrayList<>();

        webClient.get()
                .uri("/employees?ids={ids}", "1,2,3")
                .retrieve()
                .bodyToFlux(Employee.class).toIterable().forEach(employees::add);

        MatcherAssert.assertThat(employees.size(), Matchers.equalTo(3));
    }

    static private void stubEmployeeServiceResponse(String id) {
        stubEmployeeServiceResponse(id, Duration.ofSeconds(0));
    }

    static private void stubEmployeeServiceResponse(String id, Duration delay) {
        onRequest()
                .havingMethodEqualTo("GET")
                .havingPathEqualTo("/employees/" + id)
                .respond()
                .withStatus(200)
                .withDelay(delay.getSeconds(), TimeUnit.SECONDS)
                .withContentType("application/json")
                .withBody("{ \"id\":\"" + id + "\", \"name\":\"Employee " + id + "\" }");
    }
}
