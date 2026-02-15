package com.yourorg.nlsqlengine.sql;

import jakarta.enterprise.context.ApplicationScoped;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.schema.Table;

import java.util.List;
import java.util.Set;

@ApplicationScoped
public class SqlValidator {

    private static final Set<String> FORBIDDEN_STATEMENT_TYPES = Set.of(
            "DROP", "DELETE", "UPDATE", "INSERT", "ALTER", "TRUNCATE",
            "CREATE", "GRANT", "REVOKE", "MERGE", "CALL", "EXECUTE"
    );

    private Set<String> allowedTables = Set.of();

    public void setAllowedTables(Set<String> tables) {
        this.allowedTables = tables;
    }

    public SqlValidationResult validate(String sql) {
        if (sql == null || sql.isBlank()) {
            return SqlValidationResult.rejected(sql, "La requête SQL est vide");
        }

        String trimmed = sql.strip();
        // Supprimer le point-virgule final si présent
        if (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1).strip();
        }

        // Vérifier les commandes interdites par mot-clé (avant parsing)
        String firstWord = trimmed.split("\\s+")[0].toUpperCase();
        if (FORBIDDEN_STATEMENT_TYPES.contains(firstWord)) {
            return SqlValidationResult.rejected(trimmed, "Commande interdite : " + firstWord);
        }

        // Parser le SQL
        Statement statement;
        try {
            statement = CCJSqlParserUtil.parse(trimmed);
        } catch (Exception e) {
            return SqlValidationResult.rejected(trimmed, "SQL invalide : " + e.getMessage());
        }

        // Seules les requêtes SELECT sont autorisées
        if (!(statement instanceof Select select)) {
            return SqlValidationResult.rejected(trimmed, "Seules les requêtes SELECT sont autorisées");
        }

        // Vérifier SELECT *
        if (containsSelectAll(select)) {
            return SqlValidationResult.rejected(trimmed, "SELECT * est interdit, spécifiez les colonnes explicitement");
        }

        // Vérifier les tables autorisées
        if (!allowedTables.isEmpty()) {
            String unauthorizedTable = findUnauthorizedTable(select);
            if (unauthorizedTable != null) {
                return SqlValidationResult.rejected(trimmed, "Table non autorisée : " + unauthorizedTable);
            }
        }

        return SqlValidationResult.ok(trimmed);
    }

    private boolean containsSelectAll(Select select) {
        if (select instanceof PlainSelect ps) {
            return hasAllColumns(ps.getSelectItems());
        }
        if (select instanceof SetOperationList sol) {
            return sol.getSelects().stream()
                    .filter(s -> s instanceof PlainSelect)
                    .map(s -> (PlainSelect) s)
                    .anyMatch(ps -> hasAllColumns(ps.getSelectItems()));
        }
        return false;
    }

    private boolean hasAllColumns(List<SelectItem<?>> items) {
        if (items == null) return false;
        return items.stream().anyMatch(item -> item.getExpression() instanceof AllColumns);
    }

    private String findUnauthorizedTable(Select select) {
        if (select instanceof PlainSelect ps) {
            return checkTablesInPlainSelect(ps);
        }
        if (select instanceof SetOperationList sol) {
            for (Select s : sol.getSelects()) {
                if (s instanceof PlainSelect ps) {
                    String result = checkTablesInPlainSelect(ps);
                    if (result != null) return result;
                }
            }
        }
        return null;
    }

    private String checkTablesInPlainSelect(PlainSelect ps) {
        if (ps.getFromItem() instanceof Table table) {
            String tableName = table.getName().toUpperCase();
            if (!allowedTables.contains(tableName)) {
                return table.getName();
            }
        }
        if (ps.getJoins() != null) {
            for (var join : ps.getJoins()) {
                if (join.getRightItem() instanceof Table table) {
                    String tableName = table.getName().toUpperCase();
                    if (!allowedTables.contains(tableName)) {
                        return table.getName();
                    }
                }
            }
        }
        return null;
    }
}
