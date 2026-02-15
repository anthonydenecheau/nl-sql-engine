package com.yourorg.nlsqlengine.api;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class SavedPromptRepositoryTest {

    @Inject
    SavedPromptRepository repository;

    @Inject
    AgroalDataSource dataSource;

    private long domainId;

    @BeforeEach
    void cleanUp() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM domain_tables");
            stmt.execute("DELETE FROM saved_prompts");
            stmt.execute("DELETE FROM domains");
            stmt.execute("INSERT INTO domains (name, description) VALUES ('TestDomain', 'desc')");
        }
        try (Connection conn = dataSource.getConnection();
             var ps = conn.prepareStatement("SELECT id FROM domains WHERE name = 'TestDomain'");
             var rs = ps.executeQuery()) {
            rs.next();
            domainId = rs.getLong("id");
        }
    }

    @Test
    void saveAndFindByDomain() {
        SavedPrompt prompt = new SavedPrompt(null, domainId, "test question", "SELECT 1", 0, null);
        SavedPrompt saved = repository.save(prompt);

        assertNotNull(saved);
        assertNotNull(saved.id());
        assertEquals("test question", saved.question());
        assertEquals(0, saved.usageCount());

        List<SavedPrompt> found = repository.findByDomain(domainId);
        assertEquals(1, found.size());
        assertEquals("test question", found.get(0).question());
    }

    @Test
    void saveWithNullDomainId() {
        SavedPrompt prompt = new SavedPrompt(null, null, "sans domaine", "SELECT 1", 0, null);
        SavedPrompt saved = repository.save(prompt);

        assertNotNull(saved);
        assertNull(saved.domainId());
    }

    @Test
    void incrementUsage() {
        SavedPrompt saved = repository.save(
                new SavedPrompt(null, domainId, "Q1", "SELECT 1", 0, null));

        repository.incrementUsage(saved.id());
        repository.incrementUsage(saved.id());

        List<SavedPrompt> found = repository.findByDomain(domainId);
        assertEquals(2, found.get(0).usageCount());
    }

    @Test
    void findPopularOrdersByUsageCount() {
        repository.save(new SavedPrompt(null, domainId, "Low", "SELECT 1", 0, null));
        SavedPrompt high = repository.save(new SavedPrompt(null, domainId, "High", "SELECT 2", 0, null));
        for (int i = 0; i < 5; i++) repository.incrementUsage(high.id());

        List<SavedPrompt> popular = repository.findPopular(domainId, 2);
        assertEquals(2, popular.size());
        assertEquals("High", popular.get(0).question());
        assertEquals("Low", popular.get(1).question());
    }

    @Test
    void searchByKeyword() {
        repository.save(new SavedPrompt(null, domainId, "Liste des planètes", "SELECT 1", 0, null));
        repository.save(new SavedPrompt(null, domainId, "Nombre de films", "SELECT 2", 0, null));

        List<SavedPrompt> results = repository.search(null, "planète");
        assertEquals(1, results.size());
        assertEquals("Liste des planètes", results.get(0).question());
    }

    @Test
    void searchByDomainAndKeyword() {
        repository.save(new SavedPrompt(null, domainId, "Films récents", "SELECT 1", 0, null));

        List<SavedPrompt> results = repository.search(domainId, "film");
        assertEquals(1, results.size());

        // Wrong domain
        List<SavedPrompt> empty = repository.search(99999L, "film");
        assertEquals(0, empty.size());
    }

    @Test
    void searchWithoutParamsReturnsAll() {
        repository.save(new SavedPrompt(null, domainId, "Q1", "SELECT 1", 0, null));
        repository.save(new SavedPrompt(null, null, "Q2", "SELECT 2", 0, null));

        List<SavedPrompt> all = repository.search(null, null);
        assertEquals(2, all.size());
    }
}
