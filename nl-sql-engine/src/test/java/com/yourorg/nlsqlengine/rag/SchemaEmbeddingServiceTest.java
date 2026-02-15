package com.yourorg.nlsqlengine.rag;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class SchemaEmbeddingServiceTest {

    @Test
    void computeContentHashIsDeterministic() throws Exception {
        SchemaEmbeddingService service = createServiceWithProvider();

        String hash1 = service.computeContentHash();
        String hash2 = service.computeContentHash();

        assertNotNull(hash1);
        assertEquals(64, hash1.length(), "SHA-256 hex = 64 chars");
        assertEquals(hash1, hash2, "Le hash doit être déterministe");
    }

    @Test
    void computeContentHashChangesWhenContentChanges() throws Exception {
        SchemaProvider provider1 = new SchemaProvider();
        SchemaEmbeddingService service = new SchemaEmbeddingService();
        setField(service, "schemaProvider", provider1);

        String hashOriginal = service.computeContentHash();

        // Créer un provider avec description modifiée
        SchemaProvider provider2 = new FakeSchemaProvider("description modifiée");
        setField(service, "schemaProvider", provider2);

        String hashModified = service.computeContentHash();

        assertNotEquals(hashOriginal, hashModified,
                "Le hash doit changer quand le contenu change");
    }

    private SchemaEmbeddingService createServiceWithProvider() throws Exception {
        SchemaEmbeddingService service = new SchemaEmbeddingService();
        setField(service, "schemaProvider", new SchemaProvider());
        return service;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    /**
     * SchemaProvider factice pour tester avec un contenu différent.
     */
    static class FakeSchemaProvider extends SchemaProvider {
        private final String description;

        FakeSchemaProvider(String description) {
            super();
            this.description = description;
        }

        @Override
        public String getSchemaDescription() {
            return description;
        }
    }
}
