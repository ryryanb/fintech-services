package com.ryanbondoc.fintech.account;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;

class AccountApiTest {

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://localhost:8083";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    @Tag("smoke")
    void accountServiceShouldBeReachable() {

    given()
    .when()
        .get("/actuator/health")
    .then()
        .statusCode(200)
        .body("status", equalTo("UP"));
}
}

