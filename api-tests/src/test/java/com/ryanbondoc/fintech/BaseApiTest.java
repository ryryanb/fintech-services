package com.ryanbondoc.fintech;

import org.junit.jupiter.api.BeforeAll;

import io.restassured.RestAssured;

public abstract class BaseApiTest {

    @BeforeAll
    static void configureRestAssured() {

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
}

