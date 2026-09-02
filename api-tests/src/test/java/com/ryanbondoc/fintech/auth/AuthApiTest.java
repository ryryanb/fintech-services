package com.ryanbondoc.fintech.auth;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;
import io.restassured.response.Response;

class AuthApiTest {

    private static final String BASE_URL = "http://localhost:8086";

    private static final String VALID_PASSWORD =
            "SecurePassword123";

    private static final long EXPECTED_EXPIRATION_SECONDS =
            900L;

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = BASE_URL;

        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    // ============================================================
    // AUTH-001 - REGISTER USER
    // ============================================================

    @Test
    @Tag("smoke")
    void authServiceShouldBeHealthy() {

        given()
        .when()
            .get("/actuator/health")
        .then()
            .statusCode(200)
            .body("status", equalTo("UP"));
    }

    @Test
    @Tag("regression")
    void shouldRegisterUser() {

        String email = uniqueEmail();

        String requestBody = """
            {
                "email": "%s",
                "password": "%s",
                "firstName": "Ryan",
                "lastName": "Bondoc"
            }
            """.formatted(
                email,
                VALID_PASSWORD
            );

        given()
            .contentType("application/json")
            .body(requestBody)
        .when()
            .post("/auth/register")
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("email", equalTo(email))
            .body("status", equalTo("ACTIVE"))
            .body("createdAt", notNullValue());
    }

    // ============================================================
    // AUTH-002 - LOGIN
    // ============================================================

    @Test
    @Tag("regression")
    void shouldLoginRegisteredUser() {

        String email = registerTestUser();

        String requestBody = """
            {
                "email": "%s",
                "password": "%s"
            }
            """.formatted(
                email,
                VALID_PASSWORD
            );

        given()
            .contentType("application/json")
            .body(requestBody)
        .when()
            .post("/auth/login")
        .then()
            .statusCode(200)
            .body("accessToken", notNullValue())
            .body("tokenType", equalTo("Bearer"))
            .body("expiresIn", equalTo(900));
    }

    // ============================================================
    // AUTH-003 - JWT ISSUANCE
    // ============================================================

    @Test
    @Tag("regression")
    void shouldIssueValidJwtWithExpectedClaims() {

        String email = registerTestUser();

        String token = loginAndGetToken(email);

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);

        JwtPayload payload = decodeJwtPayload(token);

        assertNotNull(payload.subject());
        assertNotNull(payload.roles());
        assertNotNull(payload.issuedAt());
        assertNotNull(payload.expiration());

        assertEquals("USER", payload.roles());

        long lifetimeSeconds =
                payload.expiration() - payload.issuedAt();

        assertEquals(
                EXPECTED_EXPIRATION_SECONDS,
                lifetimeSeconds
        );

        assertTrue(
                payload.expiration() > Instant.now().getEpochSecond()
        );
    }

    // ============================================================
    // DUPLICATE REGISTRATION
    // ============================================================

    @Test
    @Tag("regression")
    void shouldRejectDuplicateRegistration() {

        String email = registerTestUser();

        String requestBody = """
            {
                "email": "%s",
                "password": "%s",
                "firstName": "Ryan",
                "lastName": "Bondoc"
            }
            """.formatted(
                email,
                VALID_PASSWORD
            );

        given()
            .contentType("application/json")
            .body(requestBody)
        .when()
            .post("/auth/register")
        .then()
            .statusCode(409);
    }

    // ============================================================
    // INVALID CREDENTIALS
    // ============================================================

    @Test
    @Tag("regression")
    void shouldRejectInvalidPassword() {

        String email = registerTestUser();

        String requestBody = """
            {
                "email": "%s",
                "password": "WrongPassword123"
            }
            """.formatted(email);

        given()
            .contentType("application/json")
            .body(requestBody)
        .when()
            .post("/auth/login")
        .then()
            .statusCode(401);
    }

    @Test
    @Tag("regression")
    void shouldRejectUnknownEmail() {

        String requestBody = """
            {
                "email": "%s",
                "password": "%s"
            }
            """.formatted(
                uniqueEmail(),
                VALID_PASSWORD
            );

        given()
            .contentType("application/json")
            .body(requestBody)
        .when()
            .post("/auth/login")
        .then()
            .statusCode(401);
    }

    // ============================================================
    // TEST HELPERS
    // ============================================================

    private static String registerTestUser() {

        String email = uniqueEmail();

        String requestBody = """
            {
                "email": "%s",
                "password": "%s",
                "firstName": "Ryan",
                "lastName": "Bondoc"
            }
            """.formatted(
                email,
                VALID_PASSWORD
            );

        given()
            .contentType("application/json")
            .body(requestBody)
        .when()
            .post("/auth/register")
        .then()
            .statusCode(201);

        return email;
    }

    private static String loginAndGetToken(String email) {

        String requestBody = """
            {
                "email": "%s",
                "password": "%s"
            }
            """.formatted(
                email,
                VALID_PASSWORD
            );

        Response response =
                given()
                    .contentType("application/json")
                    .body(requestBody)
                .when()
                    .post("/auth/login")
                .then()
                    .statusCode(200)
                    .body("accessToken", notNullValue())
                    .body("tokenType", equalTo("Bearer"))
                    .body("expiresIn", equalTo(900))
                .extract()
                    .response();

        return response.jsonPath()
                .getString("accessToken");
    }

    private static String uniqueEmail() {

        return "auth-api-"
                + UUID.randomUUID()
                + "@example.com";
    }

    // ============================================================
    // JWT PAYLOAD DECODING
    // ============================================================

    /**
     * Decodes the JWT payload for API-level claim validation.
     *
     * This verifies that the token contains the expected JWT
     * structure and claims. Signature verification belongs in
     * JwtServiceTest, where the RSA public key is available.
     */
    private static JwtPayload decodeJwtPayload(String token) {

        String[] parts = token.split("\\.");

        assertEquals(
                3,
                parts.length,
                "JWT must contain header, payload, and signature"
        );

        String payloadJson =
                new String(
                    Base64.getUrlDecoder().decode(parts[1]),
                    StandardCharsets.UTF_8
                );

        String subject =
                extractJsonString(payloadJson, "sub");

        String roles =
                extractRole(payloadJson);

        long issuedAt =
                extractJsonLong(payloadJson, "iat");

        long expiration =
                extractJsonLong(payloadJson, "exp");

        return new JwtPayload(
                subject,
                roles,
                issuedAt,
                expiration
        );
    }

    private static String extractJsonString(
            String json,
            String field) {

        String marker = "\"" + field + "\":\"";

        int start = json.indexOf(marker);

        assertTrue(
                start >= 0,
                "JWT claim '" + field + "' was not found"
        );

        start += marker.length();

        int end = json.indexOf("\"", start);

        assertTrue(
                end >= 0,
                "JWT claim '" + field + "' is malformed"
        );

        return json.substring(start, end);
    }

    private static long extractJsonLong(
            String json,
            String field) {

        String marker = "\"" + field + "\":";

        int start = json.indexOf(marker);

        assertTrue(
                start >= 0,
                "JWT claim '" + field + "' was not found"
        );

        start += marker.length();

        int end = start;

        while (end < json.length()
                && Character.isDigit(json.charAt(end))) {

            end++;
        }

        assertTrue(
                end > start,
                "JWT claim '" + field + "' is malformed"
        );

        return Long.parseLong(
                json.substring(start, end)
        );
    }

    private static String extractRole(String json) {

        String marker = "\"roles\":[";

        int start = json.indexOf(marker);

        assertTrue(
                start >= 0,
                "JWT claim 'roles' was not found"
        );

        start += marker.length();

        int quoteStart = json.indexOf("\"", start);

        assertTrue(
                quoteStart >= 0,
                "JWT roles claim is malformed"
        );

        quoteStart++;

        int quoteEnd = json.indexOf(
                "\"",
                quoteStart
        );

        assertTrue(
                quoteEnd >= 0,
                "JWT roles claim is malformed"
        );

        return json.substring(
                quoteStart,
                quoteEnd
        );
    }

    private record JwtPayload(
            String subject,
            String roles,
            long issuedAt,
            long expiration
    ) {
    }
}

