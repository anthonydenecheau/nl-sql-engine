package com.yourorg.nlsqlengine.api;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;

import static org.hamcrest.Matchers.*;

@QuarkusTest
class PromptResourceTest {

    @Inject
    AgroalDataSource dataSource;

    @BeforeEach
    void cleanUp() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM domain_tables");
            stmt.execute("DELETE FROM saved_prompts");
            stmt.execute("DELETE FROM domains");
        }
    }

    @Test
    void postPromptAndRetrieveIt() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"question\": \"Liste des planètes\", \"sqlGenerated\": \"SELECT name FROM planets\"}")
                .when().post("/api/prompts")
                .then()
                .statusCode(201)
                .body("question", is("Liste des planètes"))
                .body("sqlGenerated", is("SELECT name FROM planets"))
                .body("usageCount", is(0));
    }

    @Test
    void postPromptWithEmptyQuestionReturns400() {
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body("{\"question\": \"\"}")
                .when().post("/api/prompts")
                .then()
                .statusCode(400);
    }

    @Test
    void getPopularReturnsOrderedByUsageCount() throws Exception {
        long domainId = insertDomain("Test");
        insertPrompt(domainId, "Question A", "SELECT 1", 10);
        insertPrompt(domainId, "Question B", "SELECT 2", 50);
        insertPrompt(domainId, "Question C", "SELECT 3", 5);

        RestAssured.given()
                .queryParam("domain", domainId)
                .queryParam("limit", 3)
                .when().get("/api/prompts/popular")
                .then()
                .statusCode(200)
                .body("$", hasSize(3))
                .body("[0].question", is("Question B"))
                .body("[1].question", is("Question A"))
                .body("[2].question", is("Question C"));
    }

    @Test
    void getPopularWithLimitReturnsTopN() throws Exception {
        long domainId = insertDomain("Test");
        insertPrompt(domainId, "Q1", "SELECT 1", 30);
        insertPrompt(domainId, "Q2", "SELECT 2", 20);
        insertPrompt(domainId, "Q3", "SELECT 3", 10);

        RestAssured.given()
                .queryParam("domain", domainId)
                .queryParam("limit", 2)
                .when().get("/api/prompts/popular")
                .then()
                .statusCode(200)
                .body("$", hasSize(2));
    }

    @Test
    void getByDomainReturnsOnlyMatchingDomain() throws Exception {
        long domain1 = insertDomain("Ventes");
        long domain2 = insertDomain("RH");
        insertPrompt(domain1, "Ventes Q1", "SELECT 1", 0);
        insertPrompt(domain2, "RH Q1", "SELECT 2", 0);

        RestAssured.given()
                .queryParam("domain", domain1)
                .when().get("/api/prompts")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].question", is("Ventes Q1"));
    }

    @Test
    void searchByKeyword() throws Exception {
        long domainId = insertDomain("Test");
        insertPrompt(domainId, "Liste des planètes", "SELECT name FROM planets", 5);
        insertPrompt(domainId, "Nombre de personnages", "SELECT COUNT(*) FROM people", 3);
        insertPrompt(domainId, "Planètes avec population", "SELECT name FROM planets WHERE population > 0", 1);

        RestAssured.given()
                .queryParam("q", "planète")
                .when().get("/api/prompts/search")
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("question", everyItem(containsStringIgnoringCase("planète")));
    }

    @Test
    void searchByDomainAndKeyword() throws Exception {
        long domain1 = insertDomain("Ventes");
        long domain2 = insertDomain("RH");
        insertPrompt(domain1, "Liste des clients", "SELECT 1", 0);
        insertPrompt(domain2, "Liste des employés", "SELECT 2", 0);

        RestAssured.given()
                .queryParam("domain", domain1)
                .queryParam("q", "liste")
                .when().get("/api/prompts/search")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].question", is("Liste des clients"));
    }

    @Test
    void searchWithoutParamsReturnsAll() throws Exception {
        insertPrompt(null, "Q1", "SELECT 1", 10);
        insertPrompt(null, "Q2", "SELECT 2", 5);

        RestAssured.given()
                .when().get("/api/prompts/search")
                .then()
                .statusCode(200)
                .body("$", hasSize(2));
    }

    @Test
    void deletePrompt() throws Exception {
        insertPrompt(null, "A supprimer", "SELECT 1", 0);

        // Find the id
        int id = RestAssured.given()
                .when().get("/api/prompts/search")
                .then().statusCode(200)
                .extract().path("[0].id");

        RestAssured.given()
                .when().delete("/api/prompts/" + id)
                .then()
                .statusCode(204);

        // Verify it's gone
        RestAssured.given()
                .when().get("/api/prompts/search")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    void deleteNonExistentPromptReturns404() {
        RestAssured.given()
                .when().delete("/api/prompts/99999")
                .then()
                .statusCode(404);
    }

    @Test
    void incrementUsageCount() throws Exception {
        insertPrompt(null, "Test increment", "SELECT 1", 0);

        int id = RestAssured.given()
                .when().get("/api/prompts/search")
                .then().statusCode(200)
                .extract().path("[0].id");

        RestAssured.given()
                .when().put("/api/prompts/" + id + "/increment")
                .then()
                .statusCode(200);

        RestAssured.given()
                .when().put("/api/prompts/" + id + "/increment")
                .then()
                .statusCode(200);

        RestAssured.given()
                .when().get("/api/prompts/search")
                .then()
                .statusCode(200)
                .body("[0].usageCount", is(2));
    }

    @Test
    void getByDomainWithoutParamReturnsEmpty() {
        RestAssured.given()
                .when().get("/api/prompts")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    private long insertDomain(String name) throws Exception {
        try (Connection conn = dataSource.getConnection();
             var ps = conn.prepareStatement(
                     "INSERT INTO domains (name, description) VALUES (?, ?) RETURNING id")) {
            ps.setString(1, name);
            ps.setString(2, name + " desc");
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong("id");
            }
        }
    }

    private void insertPrompt(Long domainId, String question, String sql, int usageCount) throws Exception {
        try (Connection conn = dataSource.getConnection();
             var ps = conn.prepareStatement(
                     "INSERT INTO saved_prompts (domain_id, question, sql_generated, usage_count) VALUES (?, ?, ?, ?)")) {
            if (domainId != null) {
                ps.setLong(1, domainId);
            } else {
                ps.setNull(1, java.sql.Types.BIGINT);
            }
            ps.setString(2, question);
            ps.setString(3, sql);
            ps.setInt(4, usageCount);
            ps.executeUpdate();
        }
    }
}
