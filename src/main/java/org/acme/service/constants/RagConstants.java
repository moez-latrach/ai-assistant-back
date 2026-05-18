package org.acme.service.constants;

public final class RagConstants {

    public static final int SEARCH_MAX_RESULTS = 1;
    public static final String NO_INFORMATION_FOUND = """
            Je n'ai trouvé aucune information
            pertinente dans les documents
            sélectionnés.
            """;
    ;
    public static final String DOCUMENT_ID = "documentId";
    public static final String FILE_NAME = "filename";
    public static final String CHUNK_SEPARATOR_REGEX = "\\n\\s*\\n";

    public static final String PROMPT_TEMPLATE = """
            
                   Tu es un assistant documentaire.
            
                                                               Tu es un assistant documentaire.
            
                                                               Tu dois répondre UNIQUEMENT
                                                                à partir du contexte fourni.
            
                                                                 Règles importantes :
            
                                                                   - Réponse courte et directe.
                                                                   - Ne parle jamais du contexte ou des documents.
                                                                    - N'utilise jamais tes connaissances générales.
                                                                      - N'invente aucune information.
                                                                    - Si l'information n'est pas dans le contexte,
                                                                    réponds exactement :
            
                                                                    "Information introuvable dans les documents."
            
                                                                     - Si plusieurs réponses existent,
                                                                       résume-les brièvement.
            
            
                                                               CONTEXTE:
                                                               %s
            
                                                               QUESTION:
                                                               %s
            
            """;

    private RagConstants() {
    }
}
