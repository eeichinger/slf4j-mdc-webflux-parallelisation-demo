package com.example.slf4jmdcdemo;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class TestUtils {

    static WireMockExtension defaultWireMockExtension() {
        return WireMockExtension.newInstance()
                .options(wireMockConfig().dynamicPort())
                .configureStaticDsl(true)
                .failOnUnmatchedRequests(true)
                .build();
    }

    final static String EMPLOYEE_JSON_TEMPLATE = "{ \"id\":\"{id}\", \"name\":\"{name}\" }";

    static void stubEmployeeServiceResponse(String id) {
        stubEmployeeServiceResponse(id, 0);
    }

    static void stubEmployeeServiceResponse(String id, int delayMillis) {
        WireMock.stubFor(WireMock.get("/employees/" + id)
                .willReturn(
                        WireMock
                                .ok()
                                .withFixedDelay(delayMillis)
                                .withHeader("content-type", "application/json")
                                .withBody(EMPLOYEE_JSON_TEMPLATE.replace("{id}", id).replace("{name}", "Employee " + id))
                )
        );
    }
}
