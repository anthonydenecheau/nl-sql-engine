package com.yourorg.nlsqlengine.sql;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class SqlExecutorTest {

    @Inject
    AgroalDataSource dataSource;

    @Inject
    SqlExecutor executor;

    @BeforeEach
    void setUp() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS test_clients ("
                    + "id SERIAL PRIMARY KEY, "
                    + "name VARCHAR(100), "
                    + "age INT)");
            stmt.execute("DELETE FROM test_clients");
            stmt.execute("INSERT INTO test_clients (name, age) VALUES ('Alice', 30)");
            stmt.execute("INSERT INTO test_clients (name, age) VALUES ('Bob', 25)");
            stmt.execute("INSERT INTO test_clients (name, age) VALUES ('Charlie', 35)");
        }
    }

    @Test
    void executeValidSelect() throws SQLException {
        List<Map<String, Object>> results = executor.execute(
                "SELECT name, age FROM test_clients ORDER BY name");
        assertEquals(3, results.size());
        assertEquals("Alice", results.get(0).get("name"));
        assertEquals(30, results.get(0).get("age"));
    }

    @Test
    void executeWithWhere() throws SQLException {
        List<Map<String, Object>> results = executor.execute(
                "SELECT name FROM test_clients WHERE age > 28 ORDER BY name");
        assertEquals(2, results.size());
        assertEquals("Alice", results.get(0).get("name"));
        assertEquals("Charlie", results.get(1).get("name"));
    }

    @Test
    void executeCountQuery() throws SQLException {
        List<Map<String, Object>> results = executor.execute(
                "SELECT COUNT(id) AS total FROM test_clients");
        assertEquals(1, results.size());
        assertEquals(3L, results.get(0).get("total"));
    }

    @Test
    void executeInvalidSqlThrows() {
        assertThrows(SQLException.class, () -> executor.execute("SELECT FROM"));
    }

    @Test
    void executeDoesNotMutateData() throws SQLException {
        // L'executor est read-only, un INSERT doit échouer
        assertThrows(SQLException.class, () ->
                executor.execute("INSERT INTO test_clients (name, age) VALUES ('Hacker', 99)"));
    }
}
