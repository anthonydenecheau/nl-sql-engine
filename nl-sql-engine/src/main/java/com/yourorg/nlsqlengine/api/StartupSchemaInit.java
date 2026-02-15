package com.yourorg.nlsqlengine.api;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class StartupSchemaInit {

    private static final Logger LOG = Logger.getLogger(StartupSchemaInit.class);

    @Inject
    DomainRepository domainRepository;

    void onStart(@Observes StartupEvent ev) {
        LOG.info("Initialisation des tables applicatives (domains, saved_prompts)...");
        domainRepository.createTablesIfNotExists();
    }
}
