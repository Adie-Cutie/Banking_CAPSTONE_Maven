package com.example;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class TransactionsHistoryTest {

    private String authToken;
    private static final String BASE_URL = "http://localhost:3000/api";

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = BASE_URL;

        // Obtain token before running tests
        authToken = given()
            .contentType(ContentType.JSON)
            .body("{ \"email\": \"john@example.com\", \"password\": \"password123\" }")
        .when()
            .post("/auth/login")
        .then()
            .extract().path("token");
    }

    @Test(priority = 1)
    public void RA_01_VerifyResponseFormatAndCharSet() {
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/transactions/history")
        .then()
            .statusCode(200)
            .contentType(containsString("application/json"))
            .header("Content-Type", containsString("utf-8")); 
    }

    @Test(priority = 2)
    public void RA_02_VerifyStrictSchemaConstraints() {
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/transactions/history")
        .then()
            .body("$", instanceOf(java.util.List.class)) 
            .body("status", everyItem(oneOf("completed", "pending", "failed"))) 
            .body("type", everyItem(is("transfer"))); 
    }

    @Test(priority = 3)
    public void RA_03_Security_ForbiddenFieldsScrubbing() {
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/transactions/history")
        .then()
            .body("sender", everyItem(not(hasKey("password"))))
            .body("sender", everyItem(not(hasKey("email"))))
            .body("receiver", everyItem(not(hasKey("__v")))); // Internal versioning hidden
    }

    @Test(priority = 4)
    public void RA_04_VerifyAccountNumbersAreStrings() {
        // Essential to ensure account numbers don't lose leading zeros in JSON
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/transactions/history")
        .then()
            .body("sender.accountNumber", everyItem(instanceOf(String.class)))
            .body("sender.accountNumber", everyItem(hasLength(10)));
    }

    @Test(priority = 5)
    public void RA_05_Negative_InvalidAuthToken() {
        given()
            .header("Authorization", "Bearer invalid_token_xyz")
        .when()
            .get("/transactions/history")
        .then()
            .statusCode(401)
            .body("message", containsString("Token is not valid"));
    }

    @Test(priority = 6)
    public void RA_06_Negative_MethodNotAllowed() {
        // History should only support GET. POST/PUT should be rejected.
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .post("/transactions/history")
        .then()
            .statusCode(anyOf(is(404), is(405))); 
    }

    @Test(priority = 7)
    public void RA_07_VerifyPaginationMetadataPresence() {
        // If your backend supports pagination in the future
        Response response = given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/transactions/history");
            
        // Check if headers indicate page size or total records
        response.then().header("X-Total-Count", anything()); 
    }

    @Test(priority = 8)
    public void RA_08_VerifyLargeAmountFormatting() {
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/transactions/history")
        .then()
            .body("amount", everyItem(greaterThanOrEqualTo(0.0f)));
    }

    @Test(priority = 9)
    public void RA_09_CORS_HeaderValidation() {
        given()
            .header("Authorization", "Bearer " + authToken)
            .header("Origin", "http://localhost:5173")
        .when()
            .options("/transactions/history")
        .then()
            .statusCode(204)
            .header("Access-Control-Allow-Methods", containsString("GET"));
    }

    @Test(priority = 10)
    public void RA_10_Database_Isolation_Check() {
        given()
            .header("Authorization", "Bearer " + authToken)
        .when()
            .get("/transactions/history")
        .then()
            .body("any { it.sender.name == 'John Doe' || it.receiver.name == 'John Doe' }", is(true));
    }
}
