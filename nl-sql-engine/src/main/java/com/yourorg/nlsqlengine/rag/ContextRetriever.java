package com.yourorg.nlsqlengine.rag;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class ContextRetriever {

    private static final int MAX_RESULTS = 5;
    private static final double MIN_SCORE = 0.5;

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;

    @Inject
    SchemaProvider schemaProvider;

    /**
     * Recherche les segments de schéma les plus pertinents pour la question posée.
     * Les règles métier sont toujours incluses dans le contexte.
     */
    public String retrieveRelevantContext(String question) {
        Embedding queryEmbedding = embeddingModel.embed(question).content();

        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(MAX_RESULTS)
                .minScore(MIN_SCORE)
                .build();

        List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(request).matches();

        String schemaContext;
        if (matches.isEmpty()) {
            schemaContext = schemaProvider.getSchemaDescription();
        } else {
            schemaContext = matches.stream()
                    .filter(match -> !"business_rules".equals(match.embedded().metadata().getString("type")))
                    .map(match -> match.embedded().text())
                    .collect(Collectors.joining("\n\n"));
        }

        // Toujours ajouter les règles métier au contexte
        StringBuilder rules = new StringBuilder("Règles métier :\n");
        for (String rule : schemaProvider.getBusinessRules()) {
            rules.append("- ").append(rule).append("\n");
        }

        return schemaContext + "\n\n" + rules;
    }
}
