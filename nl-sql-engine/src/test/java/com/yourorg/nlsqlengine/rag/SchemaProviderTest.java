package com.yourorg.nlsqlengine.rag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SchemaProviderTest {

    private final SchemaProvider provider = new SchemaProvider();

    @Test
    void schemaDescriptionIsLoaded() {
        String desc = provider.getSchemaDescription();
        assertNotNull(desc);
        assertTrue(desc.contains("planets"));
        assertTrue(desc.contains("people"));
        assertTrue(desc.contains("films"));
        assertTrue(desc.contains("starships"));
        assertTrue(desc.contains("species"));
    }

    @Test
    void businessRulesAreProvided() {
        var rules = provider.getBusinessRules();
        assertNotNull(rules);
        assertFalse(rules.isEmpty());
        assertTrue(rules.stream().anyMatch(r -> r.contains("film_characters")));
    }
}
