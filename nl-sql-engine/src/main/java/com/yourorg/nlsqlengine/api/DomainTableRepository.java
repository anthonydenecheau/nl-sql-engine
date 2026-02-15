package com.yourorg.nlsqlengine.api;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class DomainTableRepository {

    @Inject
    AgroalDataSource dataSource;

    public List<String> findTablesByDomain(long domainId) {
        List<String> tables = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT table_name FROM domain_tables WHERE domain_id = ? ORDER BY table_name")) {
            ps.setLong(1, domainId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString("table_name"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la lecture des tables du domaine", e);
        }
        return tables;
    }

    public void associate(long domainId, String tableName) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO domain_tables (domain_id, table_name) VALUES (?, ?) ON CONFLICT DO NOTHING")) {
            ps.setLong(1, domainId);
            ps.setString(2, tableName);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de l'association table/domaine", e);
        }
    }

    public void dissociate(long domainId, String tableName) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "DELETE FROM domain_tables WHERE domain_id = ? AND table_name = ?")) {
            ps.setLong(1, domainId);
            ps.setString(2, tableName);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erreur lors de la dissociation table/domaine", e);
        }
    }
}
