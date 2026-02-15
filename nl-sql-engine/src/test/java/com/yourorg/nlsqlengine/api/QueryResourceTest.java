package com.yourorg.nlsqlengine.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

@QuarkusTest
class QueryResourceTest {

    @Test
    void postValidQuestion_returnsResponse() {
        // L'orchestrateur peut échouer si Ollama n'est pas disponible en test,
        // mais le endpoint doit répondre avec la question dans le body
        RestAssured.given()
                .contentType("application/json")
                .body("{\"question\": \"Liste des clients\"}")
                .when().post("/api/query")
                .then()
                .statusCode(anyOf(is(200), is(500)))
                .body("question", is("Liste des clients"));
    }

    @Test
    void postEmptyQuestion_returns400() {
        RestAssured.given()
                .contentType("application/json")
                .body("{\"question\": \"\"}")
                .when().post("/api/query")
                .then()
                .statusCode(400)
                .body("error", is("La question est obligatoire"));
    }

    @Test
    void postNullQuestion_returns400() {
        RestAssured.given()
                .contentType("application/json")
                .body("{}")
                .when().post("/api/query")
                .then()
                .statusCode(400)
                .body("error", is("La question est obligatoire"));
    }

    @Test
    void postNoBody_returns400() {
        RestAssured.given()
                .contentType("application/json")
                .when().post("/api/query")
                .then()
                .statusCode(anyOf(is(400), is(415)));
    }
}
