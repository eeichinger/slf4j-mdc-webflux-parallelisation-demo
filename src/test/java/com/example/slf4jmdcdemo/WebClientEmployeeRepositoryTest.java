package com.example.slf4jmdcdemo;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Random;

import static com.example.slf4jmdcdemo.FluxParallelisationStrategy.IDENTITY_PARALLELISER;
import static com.example.slf4jmdcdemo.TestUtils.defaultWireMockExtension;
import static com.example.slf4jmdcdemo.TestUtils.stubEmployeeServiceResponse;

class WebClientEmployeeRepositoryTest {

    @RegisterExtension
    final static WireMockExtension backendMockServer = defaultWireMockExtension();

    final static Random RANDOM = new Random();

    @Test
    public void foo() {
        // arrange
        final WebClientEmployeeRepository employeeRepository = createWebClientEmployeeRepository(backendMockServer.baseUrl());

        final String EMPLOYEE_ID = String.valueOf(RANDOM.nextInt());

        stubEmployeeServiceResponse(EMPLOYEE_ID, 0);

        // act
        final Employee employee = employeeRepository.findEmployeeById(EMPLOYEE_ID).block();

        // assert
        MatcherAssert.assertThat(employee, Matchers.notNullValue());
        MatcherAssert.assertThat(employee.getId(), Matchers.equalTo(EMPLOYEE_ID));
        MatcherAssert.assertThat(employee.getName(), Matchers.equalTo("Employee "+EMPLOYEE_ID));
    }

    static private WebClientEmployeeRepository createWebClientEmployeeRepository(String url) {
        return new WebClientEmployeeRepository(
                new MockEnvironment().withProperty(WebClientEmployeeRepository.PROP_EMPLOYEE_SERVICE_URL, url)
                , IDENTITY_PARALLELISER()
                , WebClient.builder().build()
        );
    }
}
