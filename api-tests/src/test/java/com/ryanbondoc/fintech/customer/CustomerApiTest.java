
package com.ryanbondoc.fintech.customer;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;
import io.restassured.response.Response;

class CustomerApiTest {

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = "http://localhost:8082";
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
@Tag("smoke")
void customerServiceShouldBeHealthy() {

    given()
    .when()
        .get("/actuator/health")
    .then()
        .statusCode(200)
        .body("status", equalTo("UP"));
}

    @Test
    @Tag("regression")
    void shouldCreateAndRetrieveCustomer() {

        String email =
                "cust-001-" + System.currentTimeMillis() + "@example.com";

        String userId = UUID.randomUUID().toString();

        String requestBody = """
        {
            "userId": "%s",
            "firstName": "Ryan",
            "lastName": "Bondoc",
            "email": "%s",
            "status": "ACTIVE"
        }
        """.formatted(userId, email);

        Response createResponse =
                given()
                    .contentType("application/json")
                    .body(requestBody)
                .when()
                    .post("/customers")
                .then()
                    .statusCode(201)
                    .body("id", notNullValue())
                    .body("userId", equalTo(userId))
                    .body("firstName", equalTo("Ryan"))
                    .body("lastName", equalTo("Bondoc"))
                    .body("email", equalTo(email))
                    .body("status", equalTo("ACTIVE"))
                .extract()
                    .response();

        String customerId =
                createResponse.jsonPath().getString("id");

        given()
        .when()
            .get("/customers/{id}", customerId)
        .then()
            .statusCode(200)
            .body("id", equalTo(customerId))
            .body("userId", equalTo(userId))
            .body("firstName", equalTo("Ryan"))
            .body("lastName", equalTo("Bondoc"))
            .body("email", equalTo(email))
            .body("status", equalTo("ACTIVE"));
    }
}

