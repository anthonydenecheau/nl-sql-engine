package com.yourorg.nlsqlengine.sql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SqlValidatorTest {

    private SqlValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SqlValidator();
    }

    // --- SELECT valides ---

    @Test
    void validSelect() {
        var result = validator.validate("SELECT id, name FROM clients");
        assertTrue(result.valid());
        assertNull(result.error());
    }

    @Test
    void validSelectWithJoin() {
        var result = validator.validate(
                "SELECT c.name, o.total FROM clients c JOIN orders o ON c.id = o.client_id");
        assertTrue(result.valid());
    }

    @Test
    void validSelectWithWhere() {
        var result = validator.validate("SELECT id FROM clients WHERE age > 18");
        assertTrue(result.valid());
    }

    @Test
    void validSelectStripsSemicolon() {
        var result = validator.validate("SELECT id FROM clients;");
        assertTrue(result.valid());
        assertFalse(result.sql().endsWith(";"));
    }

    // --- SQL vide / null ---

    @Test
    void nullSql() {
        var result = validator.validate(null);
        assertFalse(result.valid());
    }

    @Test
    void blankSql() {
        var result = validator.validate("   ");
        assertFalse(result.valid());
    }

    // --- Commandes interdites ---

    @ParameterizedTest
    @ValueSource(strings = {
            "DROP TABLE clients",
            "DELETE FROM clients WHERE id = 1",
            "UPDATE clients SET name = 'x' WHERE id = 1",
            "INSERT INTO clients (name) VALUES ('x')",
            "ALTER TABLE clients ADD COLUMN age INT",
            "TRUNCATE TABLE clients",
            "CREATE TABLE foo (id INT)"
    })
    void forbiddenStatements(String sql) {
        var result = validator.validate(sql);
        assertFalse(result.valid());
        assertTrue(result.error().contains("Commande interdite"));
    }

    // --- SELECT * interdit ---

    @Test
    void selectStarRejected() {
        var result = validator.validate("SELECT * FROM clients");
        assertFalse(result.valid());
        assertTrue(result.error().contains("SELECT *"));
    }

    @Test
    void selectStarInUnionRejected() {
        var result = validator.validate(
                "SELECT id FROM clients UNION SELECT * FROM orders");
        assertFalse(result.valid());
        assertTrue(result.error().contains("SELECT *"));
    }

    // --- SQL invalide ---

    @Test
    void invalidSql() {
        var result = validator.validate("COUCOU NIMPORTE QUOI");
        assertFalse(result.valid());
        assertTrue(result.error().contains("SQL invalide"));
    }

    // --- Tables autorisées ---

    @Test
    void unauthorizedTable() {
        validator.setAllowedTables(Set.of("CLIENTS"));
        var result = validator.validate("SELECT id FROM secret_data");
        assertFalse(result.valid());
        assertTrue(result.error().contains("Table non autorisée"));
    }

    @Test
    void authorizedTable() {
        validator.setAllowedTables(Set.of("CLIENTS"));
        var result = validator.validate("SELECT id FROM clients");
        assertTrue(result.valid());
    }

    @Test
    void unauthorizedTableInJoin() {
        validator.setAllowedTables(Set.of("CLIENTS"));
        var result = validator.validate(
                "SELECT c.id FROM clients c JOIN secret s ON c.id = s.id");
        assertFalse(result.valid());
        assertTrue(result.error().contains("Table non autorisée"));
    }
}
