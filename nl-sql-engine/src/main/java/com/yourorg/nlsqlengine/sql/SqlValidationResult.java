package com.yourorg.nlsqlengine.sql;

public record SqlValidationResult(boolean valid, String sql, String error) {

    public static SqlValidationResult ok(String sql) {
        return new SqlValidationResult(true, sql, null);
    }

    public static SqlValidationResult rejected(String sql, String reason) {
        return new SqlValidationResult(false, sql, reason);
    }
}
