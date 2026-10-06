package org.acme.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.acme.service.constants.RagConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires de AiService, sans Ollama ni PostgreSQL :
 * les dépendances sont remplacées par des mocks Mockito.
 */
class AiServiceTest {

    AiService aiService;
    EmbeddingModel embeddingModel;
    EmbeddingStore<TextSegment> embeddingStore;
    ChatLanguageModel chatModel;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        embeddingModel = mock(EmbeddingModel.class);
        embeddingStore = mock(EmbeddingStore.class);
        chatModel = mock(ChatLanguageModel.class);

        aiService = new AiService();
        aiService.embeddingModel = embeddingModel;
        aiService.embeddingStore = embeddingStore;
        aiService.chatModel = chatModel;
    }

    @Test
    void emptyDocumentList_returnsMessage_withoutCallingOllama() {
        String answer = aiService.ask("Une question ?", List.of());

        assertEquals(RagConstants.NO_DOCUMENT_SELECTED, answer);
        verifyNoInteractions(embeddingModel, embeddingStore, chatModel);
    }

    @Test
    void nullDocumentList_returnsMessage_withoutCallingOllama() {
        String answer = aiService.ask("Une question ?", null);

        assertEquals(RagConstants.NO_DOCUMENT_SELECTED, answer);
        verifyNoInteractions(embeddingModel, embeddingStore, chatModel);
    }

    @Test
    void noMatchFound_returnsNotFound_withoutCallingLlm() {
        when(embeddingModel.embed(anyString()))
                .thenReturn(Response.from(Embedding.from(new float[]{0.1f})));
        when(embeddingStore.search(any()))
                .thenReturn(new EmbeddingSearchResult<>(List.of()));

        String answer = aiService.ask("Une question ?", List.of(UUID.randomUUID()));

        assertEquals(RagConstants.NO_INFORMATION_FOUND, answer);
        verify(chatModel, never()).generate(anyString());
    }
}
