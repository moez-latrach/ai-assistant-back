package org.acme.service.constants;

public final class RagConstants {

    public static final int SEARCH_MAX_RESULTS = 10;
    public static final String NO_INFORMATION_FOUND =
            "Je n'ai trouvé aucune information pertinente dans les documents sélectionnés.";
    public static final String NO_DOCUMENT_SELECTED = "Sélectionnez au moins un document avant de poser une question.";

    public static final String DOCUMENT_ID = "documentId";
    public static final String FILE_NAME = "filename";
    public static final String CHUNK_INDEX = "chunkIndex";
    public static final String CONTEXT_CHUNK_SEPARATOR = "\n\n";

    /**
     * Prompt envoyé au LLM. Paramètres, dans l'ordre :
     * 1. message à renvoyer si l'information est absente (NO_INFORMATION_FOUND)
     * 2. contexte (chunks retrouvés)
     * 3. question de l'utilisateur
     */
    public static final String PROMPT_TEMPLATE = """
            Tu es un assistant documentaire.
            Tu dois répondre UNIQUEMENT à partir du contexte fourni.

            Règles importantes :
            - Réponse courte et directe.
            - Ne parle jamais du contexte ou des documents.
            - N'utilise jamais tes connaissances générales.
            - N'invente aucune information.
            - Si l'information n'est pas dans le contexte, réponds exactement :
              "%s"
            - Si plusieurs réponses existent, résume-les brièvement.

            CONTEXTE :
            %s

            QUESTION :
            %s
            """;

    private RagConstants() {
    }

}
