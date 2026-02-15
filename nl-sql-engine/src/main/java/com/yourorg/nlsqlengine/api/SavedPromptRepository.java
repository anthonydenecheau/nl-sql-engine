package com.yourorg.nlsqlengine.api;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class SavedPromptRepository {

    @Inject
    AgroalDataSource dataSource;

    public List<SavedPrompt> findByDomain(long domainId) {
        List<SavedPrompt> prompts = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT id, domain_id, question, sql_generated, usage_count, created_at FROM saved_prompts WHERE domain_id = ? ORDER BY created_at DESC")) {
            ps.setLong(1, domainId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    prompts.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la lecture des prompts", e);
        }
        return prompts;
    }

    public List<SavedPrompt> findPopular(Long domainId, int limit) {
        String sql = domainId != null
                ? "SELECT id, domain_id, question, sql_generated, usage_count, created_at FROM saved_prompts WHERE domain_id = ? ORDER BY usage_count DESC LIMIT ?"
                : "SELECT id, domain_id, question, sql_generated, usage_count, created_at FROM saved_prompts ORDER BY usage_count DESC LIMIT ?";
        List<SavedPrompt> prompts = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            if (domainId != null) {
                ps.setLong(idx++, domainId);
            }
            ps.setInt(idx, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    prompts.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la lecture des prompts populaires", e);
        }
        return prompts;
    }

    public SavedPrompt save(SavedPrompt prompt) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO saved_prompts (domain_id, question, sql_generated) VALUES (?, ?, ?) RETURNING id, domain_id, question, sql_generated, usage_count, created_at")) {
            if (prompt.domainId() != null) {
                ps.setLong(1, prompt.domainId());
            } else {
                ps.setNull(1, Types.BIGINT);
            }
            ps.setString(2, prompt.question());
            ps.setString(3, prompt.sqlGenerated());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la sauvegarde du prompt", e);
        }
        return null;
    }

    public List<SavedPrompt> search(Long domainId, String query) {
        boolean hasDomain = domainId != null;
        boolean hasQuery = query != null && !query.isBlank();

        StringBuilder sql = new StringBuilder(
                "SELECT id, domain_id, question, sql_generated, usage_count, created_at FROM saved_prompts WHERE 1=1");
        if (hasDomain) sql.append(" AND domain_id = ?");
        if (hasQuery) sql.append(" AND LOWER(question) LIKE ?");
        sql.append(" ORDER BY usage_count DESC, created_at DESC LIMIT 50");

        List<SavedPrompt> prompts = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int idx = 1;
            if (hasDomain) ps.setLong(idx++, domainId);
            if (hasQuery) ps.setString(idx, "%" + query.toLowerCase() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    prompts.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la recherche de prompts", e);
        }
        return prompts;
    }

    public boolean deleteById(long id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM saved_prompts WHERE id = ?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la suppression du prompt", e);
        }
    }

    public void incrementUsage(long id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE saved_prompts SET usage_count = usage_count + 1 WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'incrémentation du compteur", e);
        }
    }

    private SavedPrompt mapRow(ResultSet rs) throws SQLException {
        return new SavedPrompt(
                rs.getLong("id"),
                rs.getObject("domain_id") != null ? rs.getLong("domain_id") : null,
                rs.getString("question"),
                rs.getString("sql_generated"),
                rs.getInt("usage_count"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
