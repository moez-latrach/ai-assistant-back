package org.acme.config;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.acme.service.constants.LogMessages;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * Affiche au démarrage le mode RAG actif, la base utilisée et les modèles.
 * Objectif : savoir dans quel mode on travaille AVANT d'uploader un document.
 */
@ApplicationScoped
public class StartupReport {

    private static final Logger LOG = Logger.getLogger(StartupReport.class);

    @ConfigProperty(name = "rag.mode")
    String mode;

    @ConfigProperty(name = "quarkus.datasource.jdbc.url")
    String databaseUrl;

    @ConfigProperty(name = "rag.ollama.chat-model")
    String chatModel;

    @ConfigProperty(name = "rag.ollama.embedding-model")
    String embeddingModel;

    void onStart(@Observes StartupEvent event) {
        LOG.infof(LogMessages.STARTUP_CONFIG,
                mode,
                databaseUrl,
                "ollama / " + chatModel,
                "ollama / " + embeddingModel);
    }
}
