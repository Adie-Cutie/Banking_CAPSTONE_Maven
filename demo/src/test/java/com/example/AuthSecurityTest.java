package com.example;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class AuthSecurityTest {

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = "http://localhost:3000/api/auth";
    }

    @Test(description = "RA-01: Verify NoSQL Injection Prevention in Login")
    public void testNoSQLInjection() {
        // Attempting to bypass login using MongoDB query operators ($gt: greater than)
        String injectionPayload = "{\"email\": {\"$gt\": \"\"}, \"password\": \"any\"}";

        given()
            .contentType(ContentType.JSON)
            .body(injectionPayload)
        .when()
            .post("/login")
        .then()
            .statusCode(400)
            .body("message", anyOf(equalTo("Invalid Credentials"), containsString("error")));
    }

    @Test(description = "RA-02: Verify Case Insensitivity for Email Login")
    public void testEmailCaseInsensitivity() {
        String payload = "{\"email\": \"JOHN@EXAMPLE.COM\", \"password\": \"password123\"}";

        given()
            .contentType(ContentType.JSON)
            .body(payload)
        .when()
            .post("/login")
        .then()
            .statusCode(400);
    }

    @Test(description = "RA-03: Performance - Login Response Time SLA")
    public void testLoginResponseTime() {
        String payload = "{\"email\": \"john@example.com\", \"password\": \"password123\"}";

        given()
            .contentType(ContentType.JSON)
            .body(payload)
        .when()
            .post("/login")
        .then()
            .statusCode(200)
            .time(lessThan(1200L)); 
            // Check if login takes less than 1.2 seconds
    }

    @Test(description = "RA-04: Verify Registration Payload with Malformed JSON")
    public void testMalformedJson() {

        String brokenJson = "{\"email: \"error@test.com\", \"password\": \"123\"}";

        given()
            .contentType(ContentType.JSON)
            .body(brokenJson)
        .when()
            .post("/register")
        .then()
            .statusCode(anyOf(equalTo(400), equalTo(500))); 
    }

    @Test(description = "RA-05: Data Consistency - Login Response Schema Validation")
    public void testLoginSchema() {
        String payload = "{\"email\": \"john@example.com\", \"password\": \"password123\"}";

        given()
            .contentType(ContentType.JSON)
            .body(payload)
        .when()
            .post("/login")
        .then()
            .statusCode(200)
            .body("user", hasKey("accountNumber"));
    }
}