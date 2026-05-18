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

        LOG.infof("Question reçue : %s", question);

        /* QUESTION EMBEDDING */
        var embedding = embeddingModel.embed(question).content();

        /* SEARCH REQUEST */
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .maxResults(3)
                .filter(new IsIn(RagConstants.DOCUMENT_ID, documentIds.stream()
                        .map(UUID::toString)
                        .toList())).build();

        /* VECTOR SEARCH */
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(request);
        LOG.infof("MATCHES SIZE : %d", result.matches().size());
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
                    LOG.infof("SCORE : %f " , match.score() );
                    LOG.infof( "TEXT : %s " , match.embedded().text());
                    return  match.embedded().text();
                })
                .collect(Collectors.joining("\n\n"));
        LOG.infof("Context récupéré : %s", context);
       // context = result.matches().getFirst().embedded().text();
      //  LOG.infof("CONTEXT : %s ", context);

        String prompt = RagConstants.PROMPT_TEMPLATE.formatted(context, question);
        return chatModel.generate(prompt);
    }
}
