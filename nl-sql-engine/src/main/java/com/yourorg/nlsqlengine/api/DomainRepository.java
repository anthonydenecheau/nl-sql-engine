package com.yourorg.nlsqlengine.api;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class DomainRepository {

    private static final Logger LOG = Logger.getLogger(DomainRepository.class);

    @Inject
    AgroalDataSource dataSource;

    public void createTablesIfNotExists() {
        try (InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("schema/app-schema.sql")) {
            if (is == null) {
                throw new RuntimeException("app-schema.sql introuvable dans le classpath");
            }
            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                for (String statement : sql.split(";")) {
                    String trimmed = statement.strip();
                    if (!trimmed.isEmpty()) {
                        stmt.execute(trimmed);
                    }
                }
                LOG.info("Tables domains et saved_prompts créées/vérifiées");
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Impossible de créer les tables applicatives", e);
        }
    }

    public List<Domain> findAll() {
        List<Domain> domains = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, name, description FROM domains ORDER BY name")) {
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    domains.add(new Domain(rs.getLong("id"), rs.getString("name"), rs.getString("description")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la lecture des domaines", e);
        }
        return domains;
    }

    public Domain findById(long id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT id, name, description FROM domains WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Domain(rs.getLong("id"), rs.getString("name"), rs.getString("description"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la lecture du domaine", e);
        }
        return null;
    }

    public Domain create(String name, String description) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO domains (name, description) VALUES (?, ?) RETURNING id, name, description")) {
            ps.setString(1, name);
            ps.setString(2, description);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Domain(rs.getLong("id"), rs.getString("name"), rs.getString("description"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la création du domaine", e);
        }
        return null;
    }
}
