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

        // 2. Générer le SQL avec retries en cas de validation échouée
        String sql = null;
        SqlValidationResult validation = null;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            // 3. Appeler le LLM
            sql = llmClient.generateSql(question, context, businessRules, null);
            LOG.infof("SQL généré (tentative %d) : %s", attempt + 1, sql);

            // 4. Valider le SQL
            validation = sqlValidator.validate(sql);
            if (validation.valid()) {
                break;
            }
            LOG.warnf("SQL invalide (tentative %d) : %s", attempt + 1, validation.error());
        }

        if (!validation.valid()) {
            return OrchestratorResult.error(question, sql,
                    "SQL invalide après " + (MAX_RETRIES + 1) + " tentatives : " + validation.error());
        }

        // 5. Exécuter le SQL
        try {
            List<Map<String, Object>> results = sqlExecutor.execute(validation.sql());
            LOG.infof("Exécution réussie : %d lignes", results.size());

            // 6. Générer la réponse en langage naturel
            String answer = llmClient.generateAnswer(question, validation.sql(), results);
            LOG.infof("Réponse générée : %s", answer);

            return OrchestratorResult.success(question, validation.sql(), results, answer);
        } catch (Exception e) {
            LOG.errorf("Erreur d'exécution SQL : %s", e.getMessage());
            return OrchestratorResult.error(question, validation.sql(),
                    "Erreur d'exécution : " + e.getMessage());
        }
    }
}
