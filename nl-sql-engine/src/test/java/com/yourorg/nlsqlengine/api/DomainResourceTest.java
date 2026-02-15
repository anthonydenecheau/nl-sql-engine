package com.yourorg.nlsqlengine.api;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;

import static org.hamcrest.Matchers.*;

@QuarkusTest
class DomainResourceTest {

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
    void getDomainsReturnsEmptyListInitially() {
        RestAssured.given()
                .when().get("/api/domains")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    void getDomainsReturnsInsertedDomains() throws Exception {
        insertDomain("Ventes", "Domaine ventes");
        insertDomain("RH", "Ressources humaines");

        RestAssured.given()
                .when().get("/api/domains")
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("name", hasItems("RH", "Ventes"));
    }

    @Test
    void getTablesForDomainReturnsEmptyInitially() throws Exception {
        long domainId = insertDomain("Stock", "Gestion stock");

        RestAssured.given()
                .when().get("/api/domains/" + domainId + "/tables")
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    void associateAndListTables() throws Exception {
        long domainId = insertDomain("Stock", "Gestion stock");

        RestAssured.given()
                .when().put("/api/domains/" + domainId + "/tables/products")
                .then()
                .statusCode(200);

        RestAssured.given()
                .when().put("/api/domains/" + domainId + "/tables/warehouses")
                .then()
                .statusCode(200);

        RestAssured.given()
                .when().get("/api/domains/" + domainId + "/tables")
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("$", hasItems("products", "warehouses"));
    }

    @Test
    void dissociateTable() throws Exception {
        long domainId = insertDomain("Stock", "Gestion stock");

        RestAssured.given().when().put("/api/domains/" + domainId + "/tables/products");
        RestAssured.given().when().put("/api/domains/" + domainId + "/tables/warehouses");

        RestAssured.given()
                .when().delete("/api/domains/" + domainId + "/tables/products")
                .then()
                .statusCode(204);

        RestAssured.given()
                .when().get("/api/domains/" + domainId + "/tables")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("$", hasItem("warehouses"));
    }

    @Test
    void associateSameTableTwiceIsIdempotent() throws Exception {
        long domainId = insertDomain("Stock", "Gestion stock");

        RestAssured.given().when().put("/api/domains/" + domainId + "/tables/products")
                .then().statusCode(200);
        RestAssured.given().when().put("/api/domains/" + domainId + "/tables/products")
                .then().statusCode(200);

        RestAssured.given()
                .when().get("/api/domains/" + domainId + "/tables")
                .then()
                .statusCode(200)
                .body("$", hasSize(1));
    }

    @Test
    void createDomainViaApi() {
        RestAssured.given()
                .contentType("application/json")
                .body("{\"name\": \"Finance\", \"description\": \"Domaine financier\"}")
                .when().post("/api/domains")
                .then()
                .statusCode(201)
                .body("name", is("Finance"))
                .body("description", is("Domaine financier"))
                .body("id", notNullValue());

        // Verify it appears in list
        RestAssured.given()
                .when().get("/api/domains")
                .then()
                .statusCode(200)
                .body("name", hasItem("Finance"));
    }

    @Test
    void createDomainWithBlankNameReturns400() {
        RestAssured.given()
                .contentType("application/json")
                .body("{\"name\": \"  \", \"description\": \"\"}")
                .when().post("/api/domains")
                .then()
                .statusCode(400);
    }

    private long insertDomain(String name, String description) throws Exception {
        try (Connection conn = dataSource.getConnection();
             var ps = conn.prepareStatement(
                     "INSERT INTO domains (name, description) VALUES (?, ?) RETURNING id")) {
            ps.setString(1, name);
            ps.setString(2, description);
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong("id");
            }
        }
    }
}
