package com.yourorg.nlsqlengine.orchestration;

import com.yourorg.nlsqlengine.rag.SchemaEmbeddingService;
import com.yourorg.nlsqlengine.rag.SwapiImporter;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test d'intégration complet de l'orchestrateur.
 * Nécessite : docker compose up -d (PostgreSQL + Ollama avec mistral et e5-mistral)
 * Lancer avec : mvn test -Dtest="NlSqlOrchestratorIntegrationTest" -Dgroups="integration"
 */
@QuarkusTest
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NlSqlOrchestratorIntegrationTest {

    @Inject
    SwapiImporter importer;

    @Inject
    SchemaEmbeddingService embeddingService;

    @Inject
    NlSqlOrchestrator orchestrator;

    @BeforeAll
    void setUp() throws Exception {
        importer.importAll();
        embeddingService.ingestSchema();
    }

    @Test
    void questionAboutCharacters() {
        OrchestratorResult result = orchestrator.process(
                "Quels sont les noms des personnages masculins ?");
        assertNotNull(result.generatedSql(), "Le SQL doit être généré");
        assertTrue(result.generatedSql().toUpperCase().contains("SELECT"),
                "La requête doit être un SELECT");
        if (result.isSuccess()) {
            assertNotNull(result.results());
            assertFalse(result.results().isEmpty(), "Il doit y avoir des résultats");
        }
    }

    @Test
    void questionAboutPlanets() {
        OrchestratorResult result = orchestrator.process(
                "Quelle est la planète avec la plus grande population ?");
        assertNotNull(result.generatedSql());
        assertTrue(result.generatedSql().toUpperCase().contains("PLANET"),
                "La requête doit mentionner la table planets");
    }

    @Test
    void questionAboutFilms() {
        OrchestratorResult result = orchestrator.process(
                "Combien de films ont été réalisés par George Lucas ?");
        assertNotNull(result.generatedSql());
        assertTrue(result.generatedSql().toUpperCase().contains("FILM"),
                "La requête doit mentionner la table films");
    }

    @Test
    void questionWithJoin() {
        OrchestratorResult result = orchestrator.process(
                "Dans quels films apparaît Luke Skywalker ?");
        assertNotNull(result.generatedSql());
        String sqlUpper = result.generatedSql().toUpperCase();
        assertTrue(sqlUpper.contains("JOIN"),
                "La requête devrait contenir une jointure");
    }
}
