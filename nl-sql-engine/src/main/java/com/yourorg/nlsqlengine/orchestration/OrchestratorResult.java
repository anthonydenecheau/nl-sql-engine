package com.yourorg.nlsqlengine.orchestration;

import java.util.List;
import java.util.Map;

public record OrchestratorResult(
        String question,
        String generatedSql,
        List<Map<String, Object>> results,
        String answer,
        String error
) {
    public static OrchestratorResult success(String question, String sql,
                                             List<Map<String, Object>> results, String answer) {
        return new OrchestratorResult(question, sql, results, answer, null);
    }

    public static OrchestratorResult error(String question, String sql, String error) {
        return new OrchestratorResult(question, sql, null, null, error);
    }

    public boolean isSuccess() {
        return error == null;
    }
}
