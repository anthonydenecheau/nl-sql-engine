package com.yourorg.nlsqlengine.orchestration;

import com.yourorg.nlsqlengine.llm.LlmClient;
import com.yourorg.nlsqlengine.rag.ContextRetriever;
import com.yourorg.nlsqlengine.rag.SchemaProvider;
import com.yourorg.nlsqlengine.sql.SqlExecutor;
import com.yourorg.nlsqlengine.sql.SqlValidationResult;
import com.yourorg.nlsqlengine.sql.SqlValidator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

@ApplicationScoped
public class NlSqlOrchestrator {

    private static final Logger LOG = Logger.getLogger(NlSqlOrchestrator.class);
    private static final int MAX_RETRIES = 2;

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

    public OrchestratorResult process(String question) {
        LOG.infof("Question reçue : %s", question);

        // 1. RAG : récupérer le contexte pertinent
        String context = contextRetriever.retrieveRelevantContext(question);
        List<String> businessRules = schemaProvider.getBusinessRules();

        // 2. Générer le SQL avec retries et self-correction
        String sql = null;
        String lastError = null;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            // 3. Appeler le LLM (avec feedback d'erreur si retry)
            sql = llmClient.generateSql(question, context, businessRules, null, lastError);
            LOG.infof("SQL généré (tentative %d) : %s", attempt + 1, sql);

            // 4. Valider le SQL
            SqlValidationResult validation = sqlValidator.validate(sql);
            if (!validation.valid()) {
                lastError = "Validation SQL : " + validation.error();
                LOG.warnf("SQL invalide (tentative %d) : %s", attempt + 1, validation.error());
                continue;
            }
            sql = validation.sql();

            // 5. Exécuter le SQL
            try {
                List<Map<String, Object>> results = sqlExecutor.execute(sql);
                LOG.infof("Exécution réussie : %d lignes", results.size());

                // 6. Générer la réponse en langage naturel
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
}
