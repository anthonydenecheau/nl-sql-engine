package com.yourorg.nlsqlengine.llm;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class LlmClient {

    @Inject
    ChatModel chatModel;

    @Inject
    PromptBuilder promptBuilder;

    public String generateSql(String question, String schema, List<String> businessRules,
                              List<Map.Entry<String, String>> fewShotExamples) {
        String systemPrompt = promptBuilder.getSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(question, schema, businessRules, fewShotExamples);

        List<ChatMessage> messages = List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt)
        );

        ChatResponse response = chatModel.chat(messages);
        return extractSql(response.aiMessage().text());
    }

    public String generateAnswer(String question, String sql, List<Map<String, Object>> results) {
        String systemPrompt = "Tu es un assistant qui répond aux questions en langage naturel. "
                + "On te fournit une question, la requête SQL exécutée et les résultats obtenus. "
                + "Réponds de manière claire et concise en français, en résumant les résultats. "
                + "Ne mentionne pas le SQL dans ta réponse.";

        int maxResultsToShow = Math.min(results.size(), 20);
        List<Map<String, Object>> truncated = results.subList(0, maxResultsToShow);

        String userPrompt = "Question : " + question + "\n\n"
                + "SQL exécuté : " + sql + "\n\n"
                + "Résultats (" + results.size() + " lignes) :\n" + truncated + "\n\n"
                + "Réponds à la question en langage naturel.";

        List<ChatMessage> messages = List.of(
                new SystemMessage(systemPrompt),
                new UserMessage(userPrompt)
        );

        ChatResponse response = chatModel.chat(messages);
        return response.aiMessage().text().strip();
    }

    String extractSql(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            return rawResponse;
        }

        String trimmed = rawResponse.strip();

        // Supprimer les blocs markdown ```sql ... ```
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                trimmed = trimmed.substring(firstNewline + 1, lastFence).strip();
            }
        }

        // Supprimer le point-virgule final et tout texte après la requête SQL
        // Le LLM ajoute parfois des explications après le SQL
        int semicolonIdx = trimmed.indexOf(';');
        if (semicolonIdx >= 0) {
            trimmed = trimmed.substring(0, semicolonIdx).strip();
        }

        // Si la réponse contient du texte après une ligne vide suivant le SQL,
        // ne garder que la partie avant la première ligne vide
        String[] blocks = trimmed.split("\n\\s*\n", 2);
        if (blocks.length > 1 && blocks[0].toUpperCase().contains("SELECT")) {
            trimmed = blocks[0].strip();
        }

        return trimmed;
    }
}
