package com.yourorg.nlsqlengine.llm;

import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class PromptBuilder {

    private static final String SYSTEM_PROMPT_PATH = "prompts/system-v1.txt";

    private final String systemPrompt;

    public PromptBuilder() {
        this.systemPrompt = loadResource(SYSTEM_PROMPT_PATH);
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public String buildUserPrompt(String question, String schema, List<String> businessRules,
                                  List<Map.Entry<String, String>> fewShotExamples) {
        StringBuilder sb = new StringBuilder();

        // Schéma DB
        sb.append("## Schéma de la base de données\n");
        sb.append(schema);
        sb.append("\n\n");

        // Règles métier
        if (businessRules != null && !businessRules.isEmpty()) {
            sb.append("## Règles métier\n");
            for (String rule : businessRules) {
                sb.append("- ").append(rule).append("\n");
            }
            sb.append("\n");
        }

        // Few-shot examples
        if (fewShotExamples != null && !fewShotExamples.isEmpty()) {
            sb.append("## Exemples\n");
            for (var example : fewShotExamples) {
                sb.append("Question : ").append(example.getKey()).append("\n");
                sb.append("SQL : ").append(example.getValue()).append("\n\n");
            }
        }

        // Question utilisateur
        sb.append("## Question\n");
        sb.append(question);

        return sb.toString();
    }

    private String loadResource(String path) {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalStateException("Prompt template introuvable : " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Erreur de lecture du prompt : " + path, e);
        }
    }
}
