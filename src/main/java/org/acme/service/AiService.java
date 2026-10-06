package org.acme.service;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.comparison.IsIn;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.acme.service.constants.LogMessages;
import org.acme.service.constants.RagConstants;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ApplicationScoped
public class AiService {

    @Inject
    ChatLanguageModel chatModel;

    @Inject
    EmbeddingModel embeddingModel;

    @Inject
    EmbeddingStore<TextSegment> embeddingStore;

    private static final Logger LOG = Logger.getLogger(AiService.class);

    public String ask(String question, List<UUID> documentIds) {

        /* GUARD : au moins un document requis */
        if (documentIds == null || documentIds.isEmpty()) {
            LOG.warn(LogMessages.NO_DOCUMENT_SELECTED);
            return RagConstants.NO_DOCUMENT_SELECTED;
        }
        /* QUESTION EMBEDDING */
        var embedding = embeddingModel.embed(question).content();
        /* SEARCH REQUEST */
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder().queryEmbedding(embedding).maxResults(RagConstants.SEARCH_MAX_RESULTS).filter(new IsIn(RagConstants.DOCUMENT_ID, documentIds.stream().map(UUID::toString).toList())).build();

        /* VECTOR SEARCH */
        LOG.infof(LogMessages.SEARCH_REQUEST, question, documentIds.stream().map(UUID::toString).collect(Collectors.joining(LogMessages.LIST_SEPARATOR)), RagConstants.SEARCH_MAX_RESULTS);
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
        LOG.infof(LogMessages.MATCHES_RETURNED, result.matches().size(), RagConstants.SEARCH_MAX_RESULTS);
        for (int i = 0; i < result.matches().size(); i++) {
            EmbeddingMatch<TextSegment> match = result.matches().get(i);
            TextSegment segment = match.embedded();
            LOG.infof(LogMessages.MATCH_DETAIL, i + 1, match.score(), segment.metadata().getString(RagConstants.FILE_NAME), segment.metadata().getString(RagConstants.DOCUMENT_ID), segment.metadata().getInteger(RagConstants.CHUNK_INDEX), segment.text());
        }
        List<EmbeddingMatch<TextSegment>> matches = result.matches().stream()
                .toList();
        if (matches.isEmpty()) {
            LOG.info(LogMessages.NO_CONTEXT_FOUND);
            return RagConstants.NO_INFORMATION_FOUND;
        }

        String context = matches.stream().map(match -> match.embedded().text()).collect(Collectors.joining(RagConstants.CONTEXT_CHUNK_SEPARATOR));
        LOG.infof(LogMessages.CONTEXT_SENT_TO_LLM, context);
        String prompt = RagConstants.PROMPT_TEMPLATE.formatted(RagConstants.NO_INFORMATION_FOUND, context, question);
        String response = chatModel.generate(prompt);
        LOG.infof(LogMessages.LLM_RESPONSE, response);
        return response;
    }
}
