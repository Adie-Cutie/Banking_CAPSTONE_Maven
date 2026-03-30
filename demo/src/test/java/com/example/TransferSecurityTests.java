package com.example;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class TransferSecurityTests{

    private String authToken;
    private final String BASE_URL = "http://localhost:3000/api";

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = BASE_URL;
        authToken = given()
            .contentType(ContentType.JSON)
            .body("{ \"email\": \"john@example.com\", \"password\": \"password123\" }")
        .when()
            .post("/auth/login")
        .then()
            .extract().path("token");
    }

    @Test
    public void testBlockSelfTransfer() {
        given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body("{ \"receiverAccountNumber\": \"4141587793\", \"amount\": 10 }") 
        .when()
            .post("/transactions/transfer")
        .then()
            .statusCode(400)
            .body("message", containsString("Cannot transfer to self"));
    }

    @Test
    public void testTransferResponseTime() {
        given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body("{ \"receiverAccountNumber\": \"8840286830\", \"amount\": 1 }")
        .when()
            .post("/transactions/transfer")
        .then()
            .time(lessThan(800L)); 
    }

    @Test
    public void testRejectInvalidAmounts() {
        int[] invalidAmounts = {0, -50};

        for (int amt : invalidAmounts) {
            given()
                .header("Authorization", "Bearer " + authToken)
                .contentType(ContentType.JSON)
                .body("{ \"receiverAccountNumber\": \"8840286830\", \"amount\": " + amt + " }")
            .when()
                .post("/transactions/transfer")
            .then()
                .statusCode(400);
        }
    }

    @Test
    public void testEnforceJsonContentType() {
        given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.XML) // Sending XML instead of JSON
            .body("<transfer><to>8840286830</to><amt>10</amt></transfer>")
        .when()
            .post("/transactions/transfer")
        .then()
            .statusCode(500); 
    }

    @Test
    public void testDecimalPrecision() {
        given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body("{ \"receiverAccountNumber\": \"8840286830\", \"amount\": 10.55 }")
        .when()
            .post("/transactions/transfer")
        .then()
            .statusCode(200)
            .body("newBalance", anyOf(instanceOf(Float.class), instanceOf(Double.class)));
    }

    @Test
    public void testSQLInjectionPayload() {
        given()
            .header("Authorization", "Bearer " + authToken)
            .contentType(ContentType.JSON)
            .body("{ \"receiverAccountNumber\": \"' OR 1=1 --\", \"amount\": 10 }")
        .when()
            .post("/transactions/transfer")
        .then()
            .statusCode(404); 
    }
}
