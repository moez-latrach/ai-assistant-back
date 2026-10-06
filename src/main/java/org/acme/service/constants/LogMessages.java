package org.acme.service.constants;

/**
 * Modèles des messages de log du pipeline RAG.
 * Ils sont centralisés ici pour garder AiService lisible.
 * Ne pas supprimer d'information : ces logs servent au diagnostic du Retrieval (voir AGENTS.md).
 */
public final class LogMessages {

    public static final String LIST_SEPARATOR = ", ";

    public static final String NO_DOCUMENT_SELECTED = "Aucun document sélectionné : recherche annulée";

    public static final String SEARCH_REQUEST = """
            =========================================
            QUESTION :
            %s

            DOCUMENT FILTER :
            %s

            MAX RESULTS (Top K demandé) :
            %d

            =========================================
            """;

    public static final String MATCHES_RETURNED = "MATCHES RETURNED : %d / %d demandés";

    public static final String MATCH_DETAIL = """
            -----------------------------------------
            Rank        : %d
            Score       : %.4f
            Document    : %s
            DocumentId  : %s
            ChunkIndex  : %s
            -----------------------------------------
            %s
            -----------------------------------------
            """;

    public static final String NO_CONTEXT_FOUND = "Aucun contexte trouvé";

    public static final String CONTEXT_SENT_TO_LLM = """
            =========================================
            CONTEXT SENT TO LLM

            %s

            =========================================
            """;

    public static final String LLM_RESPONSE = """
            =========================================
            LLM RESPONSE

            %s

            =========================================
            """;

    private LogMessages() {
    }
}
