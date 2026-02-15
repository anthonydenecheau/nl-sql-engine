package com.yourorg.nlsqlengine.orchestration;

import com.yourorg.nlsqlengine.api.SavedPrompt;
import com.yourorg.nlsqlengine.api.SavedPromptRepository;
import com.yourorg.nlsqlengine.llm.LlmClient;
import com.yourorg.nlsqlengine.rag.ContextRetriever;
import com.yourorg.nlsqlengine.rag.SchemaProvider;
import com.yourorg.nlsqlengine.sql.SqlExecutor;
import com.yourorg.nlsqlengine.sql.SqlValidationResult;
import com.yourorg.nlsqlengine.sql.SqlValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@ApplicationScoped
public class NlSqlOrchestrator {

    private static final Logger LOG = Logger.getLogger(NlSqlOrchestrator.class);
    private static final int MAX_RETRIES = 2;
    private static final int FEW_SHOT_LIMIT = 3;

    @Inject
    ContextRetriever contextRetriever;

    @Inject
    SchemaProvider schemaProvider;

    @Inject
    LlmClient llmClient;

    @Inject
    SqlValidator sqlValidator;

    @Inject
    SqlExecutor sqlExecutor;

    @Inject
    SavedPromptRepository savedPromptRepository;

    public OrchestratorResult process(String question) {
        return process(question, null);
    }

    public OrchestratorResult process(String question, Long domainId) {
        LOG.infof("Question reçue : %s (domainId=%s)", question, domainId);

        // 1. RAG : récupérer le contexte pertinent
        String context = contextRetriever.retrieveRelevantContext(question, domainId);
        List<String> businessRules = schemaProvider.getBusinessRules();

        // 2. Récupérer les few-shot examples dynamiques depuis les prompts enregistrés
        List<Map.Entry<String, String>> fewShotExamples = loadFewShotExamples(domainId);

        // 3. Générer le SQL avec retries et self-correction
        String sql = null;
        String lastError = null;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            // 4. Appeler le LLM (avec feedback d'erreur si retry)
            sql = llmClient.generateSql(question, context, businessRules, fewShotExamples, lastError);
            LOG.infof("SQL généré (tentative %d) : %s", attempt + 1, sql);

            // 5. Valider le SQL
            SqlValidationResult validation = sqlValidator.validate(sql);
            if (!validation.valid()) {
                lastError = "Validation SQL : " + validation.error();
                LOG.warnf("SQL invalide (tentative %d) : %s", attempt + 1, validation.error());
                continue;
            }
            sql = validation.sql();

            // 6. Exécuter le SQL
            try {
                List<Map<String, Object>> results = sqlExecutor.execute(sql);
                LOG.infof("Exécution réussie : %d lignes", results.size());

                // 7. Générer la réponse en langage naturel
                String answer = llmClient.generateAnswer(question, sql, results);
                LOG.infof("Réponse générée : %s", answer);

                return OrchestratorResult.success(question, sql, results, answer);
            } catch (Exception e) {
                lastError = "Exécution SQL : " + e.getMessage();
                LOG.warnf("Erreur d'exécution (tentative %d) : %s", attempt + 1, e.getMessage());
            }
        }

        return OrchestratorResult.error(question, sql,
                "Échec après " + (MAX_RETRIES + 1) + " tentatives : " + lastError);
    }

    private List<Map.Entry<String, String>> loadFewShotExamples(Long domainId) {
        try {
            List<SavedPrompt> popular = savedPromptRepository.findPopular(domainId, FEW_SHOT_LIMIT);
            return popular.stream()
                    .filter(p -> p.sqlGenerated() != null && !p.sqlGenerated().isBlank())
                    .map(p -> (Map.Entry<String, String>) new AbstractMap.SimpleEntry<>(p.question(), p.sqlGenerated()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.warn("Impossible de charger les few-shot examples depuis les prompts enregistrés", e);
            return List.of();
        }
    }
}
