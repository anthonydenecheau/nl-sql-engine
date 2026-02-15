package com.yourorg.nlsqlengine.rag;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@ApplicationScoped
public class EmbeddingHashRepository {

    private static final Logger LOG = Logger.getLogger(EmbeddingHashRepository.class);
    private static final String KEY = "schema_hash";

    @Inject
    AgroalDataSource dataSource;

    public void createTableIfNotExists() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS embedding_metadata (key VARCHAR(255) PRIMARY KEY, value VARCHAR(255))")) {
            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de créer la table embedding_metadata", e);
        }
    }

    public String getStoredHash() {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT value FROM embedding_metadata WHERE key = ?")) {
            ps.setString(1, KEY);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("value") : null;
            }
        } catch (SQLException e) {
            LOG.warn("Impossible de lire le hash stocké, table peut-être inexistante", e);
            return null;
        }
    }

    public void saveHash(String hash) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO embedding_metadata (key, value) VALUES (?, ?) " +
                             "ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value")) {
            ps.setString(1, KEY);
            ps.setString(2, hash);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Impossible de sauvegarder le hash", e);
        }
    }
}
