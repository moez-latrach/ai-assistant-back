package org.acme.config;

public final class LangChainConstants {

    public static final String OLLAMA_BASE_URL = "http://localhost:11434";
    public static final String EMBEDDING_MODEL = "nomic-embed-text";
    public static final String CHAT_MODEL = "mistral";

    public static final String PGVECTOR_HOST = "localhost";
    public static final int PGVECTOR_PORT = 5432;
    public static final String PGVECTOR_DATABASE = "ragdb";
    public static final String PGVECTOR_USER = "userapp";
    public static final String PGVECTOR_PASSWORD = "password";
    public static final String PGVECTOR_TABLE = "embeddings";
    public static final int PGVECTOR_DIMENSION = 768;

    private LangChainConstants() {
    }
}
