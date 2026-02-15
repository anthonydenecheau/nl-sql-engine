package com.yourorg.nlsqlengine.llm;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test d'intégration avec Ollama.
 * Nécessite : docker compose up -d (Ollama + Mistral disponible)
 * Lancer avec : mvn test -Dtest="OllamaIntegrationTest" -Dgroups="integration"
 */
@Tag("integration")
class OllamaIntegrationTest {

    @Test
    void ollamaIsReachable() {
        // Vérifie que le serveur Ollama répond
        int status = RestAssured.given()
                .baseUri("http://localhost:11434")
                .when().get("/api/tags")
                .then()
                .extract().statusCode();
        assertEquals(200, status);
    }

    @Test
    void mistralModelIsAvailable() {
        // Vérifie que le modèle mistral est chargé
        String body = RestAssured.given()
                .baseUri("http://localhost:11434")
                .when().get("/api/tags")
                .then()
                .statusCode(200)
                .extract().body().asString();
        assertTrue(body.contains("mistral"), "Le modèle mistral doit être disponible");
    }

    @Test
    void mistralRespondsToPrompt() {
        // Envoie un prompt simple et vérifie qu'on obtient une réponse
        String response = RestAssured.given()
                .baseUri("http://localhost:11434")
                .contentType("application/json")
                .body("""
                        {
                          "model": "mistral",
                          "prompt": "Réponds uniquement par le mot: OK",
                          "stream": false
                        }
                        """)
                .when().post("/api/generate")
                .then()
                .statusCode(200)
                .extract().body().asString();
        assertNotNull(response);
        assertFalse(response.isBlank());
    }
}
