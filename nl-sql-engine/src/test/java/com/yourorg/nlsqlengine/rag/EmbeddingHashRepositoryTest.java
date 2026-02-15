package com.yourorg.nlsqlengine.rag;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class EmbeddingHashRepositoryTest {

    @Inject
    EmbeddingHashRepository repository;

    @Inject
    AgroalDataSource dataSource;

    @BeforeEach
    void cleanUp() throws Exception {
        repository.createTableIfNotExists();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM embedding_metadata");
        }
    }

    @Test
    void getStoredHashReturnsNullWhenEmpty() {
        assertNull(repository.getStoredHash());
    }

    @Test
    void saveAndRetrieveHash() {
        repository.saveHash("abc123");
        assertEquals("abc123", repository.getStoredHash());
    }

    @Test
    void saveHashOverwritesPreviousValue() {
        repository.saveHash("first");
        repository.saveHash("second");
        assertEquals("second", repository.getStoredHash());
    }
}
