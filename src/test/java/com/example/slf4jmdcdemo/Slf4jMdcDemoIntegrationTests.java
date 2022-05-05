package com.example.slf4jmdcdemo;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

import static com.example.slf4jmdcdemo.TestUtils.defaultWireMockExtension;
import static com.example.slf4jmdcdemo.TestUtils.stubEmployeeServiceResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Slf4jMdcDemoIntegrationTests {

    @LocalServerPort
    int serverPort;

    @Autowired
    ConfigurableEnvironment env;

    @RegisterExtension
    final static WireMockExtension backendMockServer = defaultWireMockExtension();

    @RegisterExtension
    final TestEnvironmentExtension testEnvironment = TestEnvironmentExtension.with(()->env);

    @BeforeEach
    void before() {
        MdcHooks.register_onScheduleHook();

        // override backend url for testing
        testEnvironment.withProperty(WebClientEmployeeRepository.PROP_EMPLOYEE_SERVICE_URL, backendMockServer.baseUrl());
    }

    @AfterEach
    void after() {
        MdcHooks.clear();
    }

    @Test
    void fetch_single_employee() {
        // arrange
        WebClient webClient = WebClient.create("http://localhost:" + serverPort);

        // act
        stubEmployeeServiceResponse("1");

        Employee employee = webClient.get()
                .uri("/employees/{id}", "1")
                .retrieve()
                .bodyToMono(Employee.class)
                .block();

        // assert
        MatcherAssert.assertThat(employee, Matchers.notNullValue());
        MatcherAssert.assertThat(employee.getId(), Matchers.equalTo("1"));
        MatcherAssert.assertThat(employee.getName(), Matchers.equalTo("Employee 1"));
    }

    @Test
    void fetch_multiple_employees_parallel() {
        final WebClient webClient = WebClient.create("http://localhost:" + serverPort);

        stubEmployeeServiceResponse("1", 3000);
        stubEmployeeServiceResponse("2", 3000);
        stubEmployeeServiceResponse("3", 3000);

        final List<Employee> employees = new ArrayList<>();

        webClient.get()
                .uri("/employees?ids={ids}", "1,2,3")
                .retrieve()
                .bodyToFlux(Employee.class)
                .toIterable().forEach(employees::add);

        MatcherAssert.assertThat(employees.size(), Matchers.equalTo(3));
    }
}
