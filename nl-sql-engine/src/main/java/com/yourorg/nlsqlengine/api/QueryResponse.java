package com.yourorg.nlsqlengine.api;

import java.util.List;
import java.util.Map;

public record QueryResponse(
        String question,
        String generatedSql,
        List<Map<String, Object>> results,
        String answer,
        String error
) {
    public static QueryResponse error(String question, String error) {
        return new QueryResponse(question, null, null, null, error);
    }
}
