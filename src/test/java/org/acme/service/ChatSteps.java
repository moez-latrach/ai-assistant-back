package org.acme.service;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.acme.service.constants.RagConstants;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Step definitions de features/chat.feature.
 * Cucumber crée une nouvelle instance par scénario : chaque scénario a donc ses propres mocks.
 * AiService est testé sans Ollama ni PostgreSQL (dépendances remplacées par des mocks Mockito).
 */
public class ChatSteps {

    private final EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
    @SuppressWarnings("unchecked")
    private final EmbeddingStore<TextSegment> embeddingStore = mock(EmbeddingStore.class);
    private final ChatLanguageModel chatModel = mock(ChatLanguageModel.class);
    private final AiService aiService = new AiService();

    private String question;
    private List<UUID> documentIds;
    private String answer;

    public ChatSteps() {
        // Champs @Inject package-private : accessibles car on est dans le même package.
        aiService.embeddingModel = embeddingModel;
        aiService.embeddingStore = embeddingStore;
        aiService.chatModel = chatModel;

        // Embedding factice par défaut (un "stub" ne compte pas comme un appel pour Mockito).
        when(embeddingModel.embed(anyString()))
                .thenReturn(Response.from(Embedding.from(new float[]{0.1f})));
    }

    /* ---------- GIVEN ---------- */

    @Given("a user question {string}")
    public void aUserQuestion(String question) {
        this.question = question;
    }

    @Given("no document is selected")
    public void noDocumentIsSelected() {
        this.documentIds = List.of();
    }

    @Given("the request contains no document list")
    public void theRequestContainsNoDocumentList() {
        this.documentIds = null;
    }

    @Given("{int} document(s) is/are selected")
    public void documentsAreSelected(int count) {
        this.documentIds = Stream.generate(UUID::randomUUID).limit(count).toList();
    }

    @Given("the vector search returns no passage")
    public void theVectorSearchReturnsNoPassage() {
        when(embeddingStore.search(any()))
                .thenReturn(new EmbeddingSearchResult<>(List.of()));
    }

    /* ---------- WHEN ---------- */

    @When("the user sends the question")
    public void theUserSendsTheQuestion() {
        this.answer = aiService.ask(question, documentIds);
    }

    /* ---------- THEN ---------- */

    @Then("the user is asked to select at least one document")
    public void theUserIsAskedToSelectAtLeastOneDocument() {
        assertEquals(RagConstants.NO_DOCUMENT_SELECTED, answer);
    }

    @Then("the user is told that no relevant information was found")
    public void theUserIsToldThatNoRelevantInformationWasFound() {
        assertEquals(RagConstants.NO_INFORMATION_FOUND, answer);
    }

    @Then("neither the embedding model, the vector store nor the LLM is called")
    public void noExternalComponentIsCalled() {
        verifyNoInteractions(embeddingStore, chatModel);
        verify(embeddingModel, never()).embed(anyString());
    }

    @Then("the LLM is not called")
    public void theLlmIsNotCalled() {
        verify(chatModel, never()).generate(anyString());
    }
}
