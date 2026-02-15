package com.yourorg.nlsqlengine.llm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PromptBuilderTest {

    private PromptBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new PromptBuilder();
    }

    @Test
    void systemPromptIsLoaded() {
        String system = builder.getSystemPrompt();
        assertNotNull(system);
        assertFalse(system.isBlank());
        assertTrue(system.contains("SELECT"));
        assertTrue(system.contains("DROP"));
    }

    @Test
    void buildUserPromptWithAllSections() {
        String schema = "CREATE TABLE clients (id INT, name VARCHAR(100));";
        List<String> rules = List.of("Un client actif a le statut = 'ACTIVE'");
        List<Map.Entry<String, String>> examples = List.of(
                Map.entry("Combien de clients ?", "SELECT COUNT(id) AS total FROM clients")
        );

        String prompt = builder.buildUserPrompt("Liste des clients", schema, rules, examples, null);

        assertTrue(prompt.contains("## Schéma de la base de données"));
        assertTrue(prompt.contains("CREATE TABLE clients"));
        assertTrue(prompt.contains("## Règles métier"));
        assertTrue(prompt.contains("statut = 'ACTIVE'"));
        assertTrue(prompt.contains("## Exemples"));
        assertTrue(prompt.contains("Combien de clients ?"));
        assertTrue(prompt.contains("SELECT COUNT(id)"));
        assertTrue(prompt.contains("## Question"));
        assertTrue(prompt.contains("Liste des clients"));
    }

    @Test
    void buildUserPromptWithoutOptionalSections() {
        String prompt = builder.buildUserPrompt("Liste des clients",
                "CREATE TABLE clients (id INT);", null, null, null);

        assertTrue(prompt.contains("## Schéma de la base de données"));
        assertTrue(prompt.contains("## Question"));
        assertFalse(prompt.contains("## Règles métier"));
        assertFalse(prompt.contains("## Exemples"));
    }

    @Test
    void buildUserPromptWithEmptyOptionalSections() {
        String prompt = builder.buildUserPrompt("Test",
                "schema", List.of(), List.of(), null);

        assertFalse(prompt.contains("## Règles métier"));
        assertFalse(prompt.contains("## Exemples"));
        assertFalse(prompt.contains("## Erreur"));
    }

    @Test
    void buildUserPromptWithPreviousError() {
        String prompt = builder.buildUserPrompt("Test", "schema", null, null,
                "relation \"species_planets\" does not exist");

        assertTrue(prompt.contains("## Erreur de la tentative précédente"));
        assertTrue(prompt.contains("species_planets"));
        assertTrue(prompt.contains("Corrige la requête"));
        // L'erreur doit apparaître avant la question
        assertTrue(prompt.indexOf("## Erreur") < prompt.indexOf("## Question"));
    }
}
