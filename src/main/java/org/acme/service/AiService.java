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
import org.acme.controller.DocumentResource;
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

    private static final Logger LOG = Logger.getLogger(DocumentResource.class);

    public String ask(String question, List<UUID> documentIds) {

        /* QUESTION EMBEDDING */
        var embedding = embeddingModel.embed(question).content();

        /* SEARCH REQUEST */
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .maxResults(RagConstants.SEARCH_MAX_RESULTS)
                .filter(new IsIn(RagConstants.DOCUMENT_ID, documentIds.stream()
                        .map(UUID::toString)
                        .toList())).build();

        /* VECTOR SEARCH */
        LOG.infof("""
                =========================================
                QUESTION :
                %s

                DOCUMENT FILTER :
                %s

                MAX RESULTS (Top K demandé) :
                %d

                =========================================
                """, question,
                documentIds.isEmpty()
                        ? "ALL"
                        : documentIds.stream().map(UUID::toString).collect(Collectors.joining(", ")),
                RagConstants.SEARCH_MAX_RESULTS);
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
        LOG.infof("MATCHES RETURNED : %d / %d demandés",
                result.matches().size(),
                RagConstants.SEARCH_MAX_RESULTS);
        for (int i = 0; i < result.matches().size(); i++) {
            EmbeddingMatch<TextSegment> match = result.matches().get(i);
            TextSegment segment = match.embedded();
            LOG.infof("""
                    -----------------------------------------
                    Rank        : %d
                    Score       : %.4f
                    Document    : %s
                    DocumentId  : %s
                    ChunkIndex  : %s
                    -----------------------------------------
                    %s
                    -----------------------------------------
                    """, i + 1,
                    match.score(),
                    segment.metadata().getString(RagConstants.FILE_NAME),
                    segment.metadata().getString(RagConstants.DOCUMENT_ID),
                    segment.metadata().getInteger(RagConstants.CHUNK_INDEX),
                    segment.text());
        }
        List<EmbeddingMatch<TextSegment>> matches =
                result.matches().stream()
                      //  .filter(match -> match.score() > 0.85)
                        .toList();
        if (matches.isEmpty()) {
            LOG.info("Aucun contexte trouvé");
            return RagConstants.NO_INFORMATION_FOUND;
        }

        String context = matches.stream()
                .map(match -> {
                    return match.embedded().text();
                })
                .collect(Collectors.joining("\n\n"));
        LOG.infof("""
                =========================================
                CONTEXT SENT TO LLM

                %s

                =========================================
                """, context);
       // context = result.matches().getFirst().embedded().text();
      //  LOG.infof("CONTEXT : %s ", context);

        String prompt = RagConstants.PROMPT_TEMPLATE.formatted(context, question);
        String response = chatModel.generate(prompt);
        LOG.infof("""
                =========================================
                LLM RESPONSE

                %s

                =========================================
                """, response);
        return response;
    }
}
