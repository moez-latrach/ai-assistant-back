package org.acme.config;

import java.util.Set;

/**
 * Règle de confidentialité vérifiée au démarrage :
 * un LLM cloud (Azure OpenAI) n'est autorisé que sur les bases de démonstration listées ici.
 * Liste d'AUTORISATION (et non d'interdiction) : une nouvelle base n'est jamais autorisée par défaut.
 * La liste est volontairement écrite dans le code et non dans application.properties :
 * une règle de sécurité ne doit pas pouvoir être levée par un simple changement de configuration.
 */
public final class LlmSafetyPolicy {

    public static final String PROVIDER_OLLAMA = "ollama";
    public static final String PROVIDER_AZURE = "azure";

    static final Set<String> AZURE_ALLOWED_DATABASES = Set.of("ragdb_demo");

    static final String UNKNOWN_PROVIDER =
            "Fournisseur de LLM inconnu : '%s'. Valeurs possibles : ollama, azure.";
    static final String AZURE_FORBIDDEN_DATABASE =
            "Azure OpenAI ne peut être utilisé qu'avec les bases %s. Base configurée : '%s'. Démarrage refusé.";
    static final String AZURE_MISSING_CREDENTIALS =
            "Mode azure : AZURE_OPENAI_ENDPOINT, AZURE_OPENAI_API_KEY et AZURE_OPENAI_DEPLOYMENT doivent être définies. Démarrage refusé.";

    private LlmSafetyPolicy() {
    }

    /**
     * Vérifie que la combinaison fournisseur / base est autorisée.
     *
     * @throws IllegalStateException si la configuration est refusée (l'application ne démarre pas)
     */
    public static void check(String provider, String jdbcUrl, boolean azureSettingsPresent) {
        String normalizedProvider = provider == null ? "" : provider.trim().toLowerCase();

        if (PROVIDER_OLLAMA.equals(normalizedProvider)) {
            return;
        }
        if (!PROVIDER_AZURE.equals(normalizedProvider)) {
            throw new IllegalStateException(UNKNOWN_PROVIDER.formatted(provider));
        }
        String database = databaseName(jdbcUrl);
        if (!AZURE_ALLOWED_DATABASES.contains(database)) {
            throw new IllegalStateException(AZURE_FORBIDDEN_DATABASE.formatted(AZURE_ALLOWED_DATABASES, database));
        }
        if (!azureSettingsPresent) {
            throw new IllegalStateException(AZURE_MISSING_CREDENTIALS);
        }
    }

    /** Extrait le nom de la base d'une URL JDBC : jdbc:postgresql://host:5432/ragdb_demo?ssl=true → ragdb_demo */
    static String databaseName(String jdbcUrl) {
        if (jdbcUrl == null || !jdbcUrl.contains("/")) {
            return "";
        }
        String afterSlash = jdbcUrl.substring(jdbcUrl.lastIndexOf('/') + 1);
        int query = afterSlash.indexOf('?');
        return query >= 0 ? afterSlash.substring(0, query) : afterSlash;
    }
}
