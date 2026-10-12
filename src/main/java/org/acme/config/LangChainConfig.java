package org.acme.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.sql.DataSource;
import java.util.Optional;

/**
 * Fabrique les composants LangChain4j à partir de application.properties.
 * Aucune valeur de connexion en dur : tout vient de la configuration (et donc du profil Quarkus actif).
 */
@ApplicationScoped
public class LangChainConfig {

    /** Datasource Quarkus (quarkus.datasource.*) : partagée par Hibernate (table documents) et pgvector (table embeddings). */
    @Inject
    DataSource dataSource;

    @ConfigProperty(name = "rag.ollama.base-url")
    String ollamaBaseUrl;

    @ConfigProperty(name = "rag.ollama.embedding-model")
    String embeddingModelName;

    @ConfigProperty(name = "rag.ollama.chat-model")
    String chatModelName;

    @ConfigProperty(name = "rag.llm.provider")
    String llmProvider;

    // Optional : en mode local ces variables n'existent pas, et ce n'est pas une erreur.
    @ConfigProperty(name = "rag.azure.endpoint")
    Optional<String> azureEndpoint;

    @ConfigProperty(name = "rag.azure.api-key")
    Optional<String> azureApiKey;

    @ConfigProperty(name = "rag.azure.deployment")
    Optional<String> azureDeployment;

    @ConfigProperty(name = "rag.pgvector.table")
    String pgvectorTable;

    @ConfigProperty(name = "rag.pgvector.dimension")
    int pgvectorDimension;

    @Produces
    public EmbeddingModel embeddingModel() {

        return OllamaEmbeddingModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(embeddingModelName)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * LLM de chat selon rag.llm.provider. La combinaison fournisseur / base a déjà été
     * validée au démarrage par LlmSafetyPolicy (voir StartupReport).
     */
    @Produces
    public ChatLanguageModel chatModel() {

        if (LlmSafetyPolicy.PROVIDER_AZURE.equalsIgnoreCase(llmProvider.trim())) {
            return AzureOpenAiChatModel.builder()
                    .endpoint(azureEndpoint.orElseThrow())
                    .apiKey(azureApiKey.orElseThrow())
                    .deploymentName(azureDeployment.orElseThrow())
                    .build();
        }
        return OllamaChatModel.builder()
                .baseUrl(ollamaBaseUrl)
                .modelName(chatModelName)
                .build();
    }

    @Produces
    public EmbeddingStore<TextSegment> embeddingStore() {

        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .table(pgvectorTable)
                .dimension(pgvectorDimension)
                .createTable(true)
                .build();
    }
}
