package com.yourorg.nlsqlengine.rag;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class StartupEmbeddingSync {

    private static final Logger LOG = Logger.getLogger(StartupEmbeddingSync.class);

    @Inject
    SchemaEmbeddingService embeddingService;

    @ConfigProperty(name = "nlsql.embedding.sync-on-startup", defaultValue = "true")
    boolean syncOnStartup;

    void onStart(@Observes StartupEvent ev) {
        if (!syncOnStartup) {
            LOG.info("Synchronisation des embeddings désactivée au démarrage");
            return;
        }
        LOG.info("Vérification des embeddings au démarrage...");
        embeddingService.ingestIfChanged();
    }
}
