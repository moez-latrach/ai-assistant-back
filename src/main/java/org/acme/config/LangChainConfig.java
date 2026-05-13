package org.acme.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class LangChainConfig {

    @Produces
    public EmbeddingModel embeddingModel() {

        return OllamaEmbeddingModel.builder()
                .baseUrl(LangChainConstants.OLLAMA_BASE_URL)
                .modelName(LangChainConstants.EMBEDDING_MODEL)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Produces
    public ChatLanguageModel chatModel() {

        return OllamaChatModel.builder()
                .baseUrl(LangChainConstants.OLLAMA_BASE_URL)
                .modelName(LangChainConstants.CHAT_MODEL)
                .build();
    }

    @Produces
    public EmbeddingStore<TextSegment> embeddingStore() {

        return PgVectorEmbeddingStore.builder()
                .host(LangChainConstants.PGVECTOR_HOST)
                .port(LangChainConstants.PGVECTOR_PORT)
                .database(LangChainConstants.PGVECTOR_DATABASE)
                .user(LangChainConstants.PGVECTOR_USER)
                .password(LangChainConstants.PGVECTOR_PASSWORD)
                .table(LangChainConstants.PGVECTOR_TABLE)
                .dimension(LangChainConstants.PGVECTOR_DIMENSION)
                .createTable(true)
                .build();
    }
}
