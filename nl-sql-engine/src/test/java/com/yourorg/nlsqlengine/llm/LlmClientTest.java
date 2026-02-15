package com.yourorg.nlsqlengine.llm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LlmClientTest {

    private final LlmClient client = new LlmClient();

    @Test
    void extractSql_plainSql() {
        assertEquals("SELECT id FROM clients",
                client.extractSql("SELECT id FROM clients"));
    }

    @Test
    void extractSql_withMarkdownBlock() {
        String raw = "```sql\nSELECT id FROM clients\n```";
        assertEquals("SELECT id FROM clients", client.extractSql(raw));
    }

    @Test
    void extractSql_withSemicolon() {
        assertEquals("SELECT id FROM clients",
                client.extractSql("SELECT id FROM clients;"));
    }

    @Test
    void extractSql_withMarkdownAndSemicolon() {
        String raw = "```sql\nSELECT id FROM clients;\n```";
        assertEquals("SELECT id FROM clients", client.extractSql(raw));
    }

    @Test
    void extractSql_null() {
        assertNull(client.extractSql(null));
    }

    @Test
    void extractSql_blank() {
        assertEquals("   ", client.extractSql("   "));
    }

    @Test
    void extractSql_withSurroundingWhitespace() {
        assertEquals("SELECT id FROM clients",
                client.extractSql("  SELECT id FROM clients  "));
    }

    @Test
    void extractSql_withTrailingExplanation() {
        String raw = "SELECT s.name FROM starships s WHERE s.name = 'X-wing'\n\n"
                + "Pour obtenir des informations sur l'équipage, il faudrait utiliser la table...";
        assertEquals("SELECT s.name FROM starships s WHERE s.name = 'X-wing'",
                client.extractSql(raw));
    }

    @Test
    void extractSql_withSemicolonAndTrailingText() {
        String raw = "SELECT s.name FROM starships s;\n\nCeci est une explication.";
        assertEquals("SELECT s.name FROM starships s",
                client.extractSql(raw));
    }
}
