package com.yourorg.nlsqlengine.rag;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@ApplicationScoped
public class SchemaEmbeddingService {

    private static final Logger LOG = Logger.getLogger(SchemaEmbeddingService.class);

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;

    @Inject
    SchemaProvider schemaProvider;

    @Inject
    AgroalDataSource dataSource;

    @Inject
    EmbeddingHashRepository hashRepository;

    /**
     * Vérifie si le contenu a changé et ré-ingère si nécessaire.
     */
    public void ingestIfChanged() {
        hashRepository.createTableIfNotExists();
        String currentHash = computeContentHash();
        String storedHash = hashRepository.getStoredHash();

        if (currentHash.equals(storedHash)) {
            LOG.info("Embeddings à jour, pas de ré-ingestion");
            return;
        }

        LOG.info("Changement détecté dans le contenu, ré-ingestion des embeddings...");
        purgeEmbeddings();
        ingestSchema();
        hashRepository.saveHash(currentHash);
        LOG.info("Ré-ingestion terminée, hash mis à jour");
    }

    /**
     * Découpe la description du schéma par table et ingère chaque segment dans pgvector.
     */
    public void ingestSchema() {
        String description = schemaProvider.getSchemaDescription();
        List<TextSegment> segments = splitByTable(description);

        // Ajouter les règles métier comme segment
        List<String> rules = schemaProvider.getBusinessRules();
        StringBuilder rulesText = new StringBuilder("Règles métier de la base Star Wars :\n");
        for (String rule : rules) {
            rulesText.append("- ").append(rule).append("\n");
        }
        segments.add(TextSegment.from(rulesText.toString(), Metadata.from("type", "business_rules")));

        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
        embeddingStore.addAll(embeddings, segments);

        LOG.infof("Ingéré %d segments dans pgvector", segments.size());
    }

    /**
     * Purge tous les embeddings existants.
     */
    public void purgeEmbeddings() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM embeddings")) {
            int deleted = ps.executeUpdate();
            LOG.infof("Purgé %d embeddings existants", deleted);
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de purger les embeddings", e);
        }
    }

    String computeContentHash() {
        String description = schemaProvider.getSchemaDescription();
        List<String> rules = schemaProvider.getBusinessRules();

        StringBuilder content = new StringBuilder(description);
        for (String rule : rules) {
            content.append(rule);
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 non disponible", e);
        }
    }

    private List<TextSegment> splitByTable(String description) {
        List<TextSegment> segments = new ArrayList<>();
        String[] sections = description.split("###");

        for (String section : sections) {
            String trimmed = section.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("Base de données") || trimmed.startsWith("##")) {
                continue;
            }
            // Extraire le nom de la table depuis la première ligne
            String[] lines = trimmed.split("\n", 2);
            String tableName = lines[0].strip().toLowerCase();

            Metadata metadata = Metadata.from("type", "table_schema");
            metadata.put("table", tableName);
            metadata.put("domain", "default");
            segments.add(TextSegment.from(trimmed, metadata));
        }

        return segments;
    }
}
