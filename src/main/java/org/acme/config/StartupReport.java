package org.acme.config;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.acme.service.constants.LogMessages;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Optional;

/**
 * Au démarrage :
 * 1. vérifie la règle de confidentialité (LlmSafetyPolicy) : si elle échoue, l'application ne démarre pas ;
 * 2. affiche le mode RAG actif, la base utilisée et les modèles.
 * Objectif : savoir dans quel mode on travaille AVANT d'uploader un document.
 */
@ApplicationScoped
public class StartupReport {

    private static final Logger LOG = Logger.getLogger(StartupReport.class);

    @ConfigProperty(name = "rag.mode")
    String mode;

    @ConfigProperty(name = "quarkus.datasource.jdbc.url")
    String databaseUrl;

    @ConfigProperty(name = "rag.llm.provider")
    String llmProvider;

    @ConfigProperty(name = "rag.ollama.chat-model")
    String ollamaChatModel;

    @ConfigProperty(name = "rag.ollama.embedding-model")
    String embeddingModel;

    @ConfigProperty(name = "rag.azure.endpoint")
    Optional<String> azureEndpoint;

    @ConfigProperty(name = "rag.azure.api-key")
    Optional<String> azureApiKey;

    @ConfigProperty(name = "rag.azure.deployment")
    Optional<String> azureDeployment;

    void onStart(@Observes StartupEvent event) {
        boolean azureSettingsPresent = azureEndpoint.isPresent()
                && azureApiKey.isPresent()
                && azureDeployment.isPresent();

        // Lève IllegalStateException si la configuration est interdite : Quarkus arrête alors le démarrage.
        LlmSafetyPolicy.check(llmProvider, databaseUrl, azureSettingsPresent);

        String chatModel = LlmSafetyPolicy.PROVIDER_AZURE.equalsIgnoreCase(llmProvider.trim())
                ? "azure / " + azureDeployment.orElse("?")
                : "ollama / " + ollamaChatModel;

        // La clé API n'est jamais affichée.
        LOG.infof(LogMessages.STARTUP_CONFIG,
                mode,
                databaseUrl,
                chatModel,
                "ollama / " + embeddingModel + " (local)");
    }
}
