package com.yourorg.nlsqlengine.sql;

import io.agroal.api.AgroalDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class SqlExecutor {

    @Inject
    AgroalDataSource dataSource;

    @ConfigProperty(name = "nlsql.executor.max-rows", defaultValue = "100")
    int maxRows;

    @ConfigProperty(name = "nlsql.executor.timeout-seconds", defaultValue = "30")
    int timeoutSeconds;

    public List<Map<String, Object>> execute(String sql) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setReadOnly(true);
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setQueryTimeout(timeoutSeconds);
                stmt.setMaxRows(maxRows);

                try (ResultSet rs = stmt.executeQuery()) {
                    return mapResultSet(rs);
                }
            } finally {
                conn.rollback();
            }
        }
    }

    private List<Map<String, Object>> mapResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();
        List<Map<String, Object>> rows = new ArrayList<>();

        while (rs.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int i = 1; i <= columnCount; i++) {
                row.put(meta.getColumnLabel(i), rs.getObject(i));
            }
            rows.add(row);
        }
        return rows;
    }
}
