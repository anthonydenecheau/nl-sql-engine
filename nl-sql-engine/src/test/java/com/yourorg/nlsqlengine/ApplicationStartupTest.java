package com.yourorg.nlsqlengine;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.anything;

@QuarkusTest
class ApplicationStartupTest {

    @Test
    void applicationStarts() {
        // Vérifie que l'application Quarkus démarre et répond sur HTTP
        RestAssured.given()
                .when().get("/")
                .then()
                .statusCode(anything());
    }
}
