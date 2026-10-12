# AGENTS.md

## Objet du projet

Ce repository contient le backend d'un assistant documentaire RAG (Retrieval-Augmented Generation). Il ingère des documents PDF ou DOCX, indexe leurs fragments dans PostgreSQL/pgvector, puis répond à une question à partir des fragments retrouvés et d'un LLM : Mistral en local via Ollama (mode `local`, par défaut) ou Azure OpenAI (mode `azure`, documents de démonstration uniquement).

Le projet est actuellement un RAG simple, synchrone et mono-tour. Il n'implémente pas encore les citations, le reranking, la recherche hybride, l'historique de conversation, l'authentification ni des agents capables d'exécuter des actions.

Le code courant est la source de vérité. La documentation sous `documentation/` explique les intentions, mais certaines valeurs peuvent être anciennes (notamment le chunking et le Top K).

## Stack et services externes

- Java 21 et Maven Wrapper.
- Quarkus 3.35.2 : CDI/Arc, REST/Jackson, REST multipart, Hibernate ORM Panache, JDBC PostgreSQL et OpenAPI.
- LangChain4j 0.36.2.
- Ollama sur `http://localhost:11434` :
  - embeddings : `nomic-embed-text` (utilisé dans **les deux modes**) ;
  - chat : `mistral` (mode `local`).
- Azure OpenAI via `langchain4j-azure-open-ai` 0.36.2 : chat uniquement, mode `azure` uniquement.
- PostgreSQL 16 avec pgvector, exposé sur `localhost:5432` par `docker-compose.yml`.
- Modèle d'embedding configuré sur 768 dimensions.
- PDFBox 3.0.2 pour les PDF et Apache POI 5.4.0 pour les DOCX.

Toute la configuration est dans `application.properties` (plus aucune valeur de connexion en dur dans le code) :

- `quarkus.datasource.*` : connexion PostgreSQL. **Une seule datasource**, partagée par Hibernate (table `documents`) et par pgvector (table `embeddings`) ;
- `rag.ollama.base-url`, `rag.ollama.embedding-model`, `rag.ollama.chat-model` : modèles Ollama ;
- `rag.pgvector.table`, `rag.pgvector.dimension` : stockage vectoriel (la dimension doit correspondre au modèle d'embedding) ;
- `rag.mode` (`local` / `azure`) et `rag.llm.provider` (`ollama` / `azure`) : mode affiché et fournisseur du LLM de chat ;
- `rag.azure.endpoint`, `rag.azure.api-key`, `rag.azure.deployment` : lus **uniquement** depuis les variables d'environnement `AZURE_OPENAI_ENDPOINT`, `AZURE_OPENAI_API_KEY`, `AZURE_OPENAI_DEPLOYMENT` ;
- CORS et génération du schéma Hibernate.

Les identifiants de développement PostgreSQL sont actuellement `userapp/password`, base `ragdb`. Ne pas les considérer comme une configuration de production.

## Modes d'exécution et confidentialité

Deux modes, sélectionnés par profil Quarkus au démarrage (pas de bascule à chaud) :

| Mode | Lancement | LLM de chat | Base | Documents autorisés |
|---|---|---|---|---|
| `local` (défaut) | `./mvnw quarkus:dev` | Ollama / `mistral` | `ragdb` | tous, y compris confidentiels |
| `azure` | `./mvnw quarkus:dev -Dquarkus.profile=azure` | Azure OpenAI | `ragdb_demo` | **fictifs ou publics uniquement** |

- La base `ragdb_demo` est créée par `docker/init/01-create-demo-db.sql`, exécuté par PostgreSQL uniquement sur un volume vide (`docker compose down -v` puis `up -d` pour le rejouer — efface toutes les données).
- Le profil `azure` ne change que `quarkus.datasource.jdbc.url`, `rag.mode` et `rag.llm.provider` ; grâce à la datasource unique, les deux tables basculent ensemble.
- L'embedding reste `nomic-embed-text` en local dans les deux modes : un seul paramètre varie (le LLM), ce qui permet de comparer Mistral et Azure sur les mêmes chunks.
- `StartupReport` affiche au démarrage le mode, l'URL de la base, le modèle de chat et l'embedding. La clé API n'est jamais affichée.

Verrou de sécurité `LlmSafetyPolicy`, vérifié au démarrage par `StartupReport` (une exception arrête Quarkus) :

- `ollama` est autorisé partout ;
- `azure` n'est autorisé **que** sur les bases de la liste d'autorisation `AZURE_ALLOWED_DATABASES` (`ragdb_demo`), et seulement si endpoint, clé et déploiement sont tous définis ;
- tout fournisseur inconnu est refusé.

Règles à ne jamais enfreindre :

- ne jamais ajouter `ragdb` (ou une base contenant des documents réels) à `AZURE_ALLOWED_DATABASES` ;
- ne jamais déplacer cette liste dans `application.properties` : une règle de sécurité ne doit pas pouvoir être levée par un changement de configuration ;
- ne jamais écrire de clé ou de secret dans le code, `application.properties`, un fichier `.env` ou la configuration IntelliJ du projet. La clé Azure est stockée dans le Trousseau macOS et exportée dans `~/.zshrc` (hors du projet) ;
- ne jamais transformer la liste d'autorisation en liste d'interdiction : une nouvelle base ne doit jamais être autorisée par défaut.

Règle importante : ne jamais redonner à pgvector ses propres paramètres de connexion (`host`, `database`, `user`…). Les deux tables doivent toujours passer par la même datasource ; sinon, un changement de base (par exemple via un profil Quarkus) ne s'appliquerait qu'à une seule des deux tables, sans erreur visible.

## Architecture

```text
API REST
├── DocumentResource ──> DocumentFileService ──> extracteurs / embeddings
│                               ├──> DocumentRepository ──> table documents
│                               └──> EmbeddingStore ──────> table embeddings
└── AiResource ───────> AiService ─────────────> embeddings / recherche / LLM

Ollama      : modèles d'embedding et de chat
PostgreSQL  : métadonnées des documents et vecteurs pgvector
```

### API exposée

- `POST /documents/upload` : upload multipart d'un fichier, puis ingestion synchrone.
- `GET /documents` : liste les documents persistés.
- `POST /chat` : reçoit `{"message": "...", "documentIds": ["..."]}` et renvoie `{"answer": "..."}`.

Le CORS autorise actuellement `http://localhost:4200`.

## Pipeline RAG réel

### Ingestion

```text
Document
    ↓
Extraction PDF ou DOCX
    ↓
Chunking récursif (500 caractères, overlap 100)
    ↓
Métadonnées : documentId, filename, chunkIndex
    ↓
Embedding nomic-embed-text
    ↓
EmbeddingStore / PostgreSQL pgvector
```

`DocumentFileService.upload()` persiste d'abord une entité `DocumentFile` (`id`, `filename`, `uploadedAt`), sélectionne un `DocumentExtractor`, puis appelle `ingest()`.

Le splitter courant est `DocumentSplitters.recursive(500, 100)`. Chaque `TextSegment` enrichi contient :

- `documentId` : UUID converti en chaîne, utilisé par le filtre de recherche ;
- `filename` : nom du fichier source ;
- `chunkIndex` : indice à partir de 0, destiné à l'observabilité et à une éventuelle récupération future des voisins.

`chunkIndex` n'intervient pas dans le calcul de similarité et n'améliore donc pas directement le score vectoriel.

Les extracteurs CDI disponibles sont :

- `PdfExtractor` : fichiers dont le nom se termine par `.pdf`, extraction via PDFBox ;
- `WordExtractor` : fichiers dont le nom se termine par `.docx`, concaténation des paragraphes via Apache POI.

La détection d'extension est actuellement sensible à la casse. Un format sans extracteur provoque `RuntimeException("Format non supporté")`.

### Question et Retrieval

```text
Question + documentIds
    ↓
Garde : aucun document sélectionné ? → NO_DOCUMENT_SELECTED (fin)
    ↓
Embedding nomic-embed-text
    ↓
Vector Search filtrée par documentId
    ↓
Top K chunks (RagConstants.SEARCH_MAX_RESULTS = 10)
    ↓
Concaténation (RagConstants.CONTEXT_CHUNK_SEPARATOR)
    ↓
Contexte injecté dans PROMPT_TEMPLATE
    ↓
LLM Mistral
    ↓
Answer
```

`AiService.ask()` commence par une garde : si `documentIds` est `null` ou vide, il renvoie `RagConstants.NO_DOCUMENT_SELECTED` sans calculer d'embedding, sans recherche et sans appel au LLM (log `WARN`). C'est un choix fonctionnel : on refuse plutôt que de chercher dans tous les documents, en prévision de futurs droits d'accès par document. Le message est renvoyé avec un statut 200 ; le front l'affiche tel quel. Sinon, un filtre `IsIn(documentId, documentIds)` est construit (LangChain4j refuse une liste vide dans `IsIn`, d'où la garde).

Tous les matches renvoyés sont conservés, dans leur ordre de ranking. Aucun seuil de score et aucun reranker ne sont appliqués. Si aucun match n'est retourné, le service renvoie `NO_INFORMATION_FOUND` sans appeler le LLM.

`PROMPT_TEMPLATE` prend trois paramètres, dans l'ordre : le message `NO_INFORMATION_FOUND`, le contexte, la question. Le LLM reçoit donc le même texte de repli que le backend : l'utilisateur voit une seule formulation quand l'information est absente (sous réserve que le modèle la recopie exactement).

## Classes principales

- `LangChainConfig` : producteurs CDI pour `EmbeddingModel`, `ChatLanguageModel` (Ollama ou Azure OpenAI selon `rag.llm.provider` ; réglages Azure en `Optional<String>`, absents en mode local) et `PgVectorEmbeddingStore`. Lit la configuration via `@ConfigProperty` (`rag.*`) et injecte la `DataSource` Quarkus, transmise à pgvector par `PgVectorEmbeddingStore.datasourceBuilder()` (table `embeddings`, créée si nécessaire). Une propriété manquante empêche le démarrage (comportement voulu).
- `LlmSafetyPolicy` : règle pure (fournisseur + URL JDBC + présence des identifiants → autorisé / refusé). Voir « Modes d'exécution et confidentialité ».
- `StartupReport` : au `StartupEvent`, applique `LlmSafetyPolicy` puis affiche la configuration active.
- `DocumentResource` : endpoints d'upload et de listing.
- `AiResource` : endpoint de chat.
- `DocumentFileService` : persistance du document, extraction, chunking, enrichissement, embedding et stockage.
- `DocumentExtractor` : extension SPI des formats supportés.
- `PdfExtractor` / `WordExtractor` : implémentations PDF et DOCX.
- `AiService` : embedding de question, recherche vectorielle, contexte, prompt et génération.
- `DocumentRepository` : `PanacheRepository<DocumentFile>`.
- `DocumentFile` : entité de la table `documents`.
- DTO records : `ChatRequest`, `ChatResponse`, `UploadResponse`, `DocumentResponse`.
- `RagConstants` : tout ce qui influence le comportement du RAG ou ce que lit l'utilisateur/le LLM — Top K (`SEARCH_MAX_RESULTS`), messages utilisateur (`NO_INFORMATION_FOUND`, `NO_DOCUMENT_SELECTED`), clés de métadonnées, séparateur de contexte, `PROMPT_TEMPLATE`.
- `LogMessages` : modèles des messages de log (ingestion et Retrieval). Uniquement destinés au développeur.

## État actuel et diagnostic du Retrieval

Le pipeline de base fonctionne et les logs exposent l'ingestion, les résultats classés, le contexte et la réponse. Le travail en cours porte sur la qualité du Retrieval.

Problème observé : un chunk contenant la bonne information peut être présent dans pgvector mais classé trop bas pour entrer dans le Top K. Augmenter `maxResults` peut alors le faire apparaître. Avant toute correction, localiser précisément la défaillance :

1. **Extraction** : l'information existe-t-elle dans le texte extrait ?
2. **Chunking** : existe-t-elle dans un chunk exploitable, sans séparation destructive ?
3. **Embedding** : la représentation sémantique est-elle adaptée au contenu et à la question ?
4. **Retrieval/ranking** : le chunk existe-t-il mais arrive-t-il trop bas ? Quels sont son rang et son score ?
5. **Génération** : le bon chunk est-il dans le contexte exact, mais la réponse reste-t-elle incorrecte ?

Points connus à garder visibles :

- la documentation sous `documentation/` mentionne encore d'anciennes valeurs (`250/50` et Top 3) ;
- les tests couvrent pour l'instant uniquement la garde « aucun document » et le cas « aucun match » ; le cas nominal (prompt envoyé au LLM) et l'ingestion ne sont pas encore testés.

## Observabilité à préserver

Ne pas supprimer les logs de diagnostic qui permettent de relier ingestion et Retrieval. Ils doivent conserver au minimum :

- à l'ingestion : `chunkIndex`, document, nombre de caractères et texte complet du chunk ;
- à la recherche : question, filtre documentaire, Top K demandé et nombre de matches réellement renvoyés (`MATCHES RETURNED : x / K demandés`) ;
- pour chaque match : rang (à partir de 1), score, document, `documentId`, `chunkIndex` et texte complet ;
- le contexte complet envoyé au LLM ;
- la réponse complète du LLM.

Attention aux données sensibles : ces logs contiennent le texte intégral des documents, des questions et des réponses. Toute évolution vers la production devra définir une politique de niveau, masquage et rétention sans dégrader les diagnostics de développement.

## Règles de contribution pour les agents

1. Lire ce fichier et la documentation pertinente avant toute modification importante.
2. Lire le code réel du pipeline concerné ; en cas d'écart, privilégier le code et signaler la documentation obsolète.
3. Comprendre et décrire l'étape concernée avant de modifier extraction, chunking, embedding, ranking, contexte ou génération.
4. Ne changer qu'un paramètre RAG à la fois pendant une expérimentation et consigner sa valeur avant/après.
5. Préférer des changements petits, réversibles et testables avec une question et un corpus de référence.
6. Ne pas ajouter arbitrairement de seuil de similarité sans jeu d'évaluation : un seuil peut supprimer le chunk correct.
7. Ne pas masquer un problème de Retrieval uniquement en modifiant le prompt du LLM.
8. Préserver les logs de rang, score, document, `chunkIndex`, contenu du chunk, contexte et réponse.
9. Ne pas modifier simultanément le Top K, le modèle d'embedding, le chunking et le prompt : le résultat deviendrait impossible à attribuer.
10. Expliquer toute modification architecturale importante et mettre à jour cette documentation si l'état réel change.
11. Ajouter ou adapter les tests pertinents, puis exécuter les tests existants après toute modification.
12. Ne pas committer des fichiers locaux ou générés (`target/`, `.idea/`, `effective-pom.xml`) sans intention explicite. Seul le dossier `corpus/` peut contenir des documents, et uniquement fictifs ou publics.
13. Ne jamais placer ni lire de document réel ou confidentiel dans le projet ; les documents confidentiels restent hors du dépôt et ne sont utilisés qu'en mode `local`.
14. Ne jamais affaiblir `LlmSafetyPolicy` ni contourner la séparation `ragdb` / `ragdb_demo` (voir « Modes d'exécution et confidentialité »).

## Conventions constatées

- Packages sous `org.acme`, composants métier `@ApplicationScoped` et injection CDI par `@Inject`.
- Endpoints Jakarta REST regroupés dans `controller`, chemins dans `ApiPaths`.
- DTO simples sous forme de records Java ; entités JPA et repositories Panache séparés.
- Constantes RAG et clés de métadonnées centralisées dans `RagConstants` ; messages de log dans `LogMessages`. Règle de tri : si le texte est lu par l'utilisateur ou le LLM → `RagConstants` ; s'il est lu par le développeur dans la console → `LogMessages`. Pas de chaîne de log en dur dans les services.
- Une seule source de vérité par paramètre : jamais de valeur en dur dupliquée entre le code et les logs.
- Nouveau format documentaire : implémenter `DocumentExtractor`, l'annoter comme bean CDI et définir précisément `supports()`.
- Conserver le même modèle et la même dimension entre les embeddings ingérés et ceux des questions. Un changement de modèle/dimension nécessite une stratégie explicite de réindexation.
- Ne jamais interpréter un score isolé comme une mesure absolue de vérité ; comparer les rangs et résultats sur un corpus évalué.

## Démarrage, compilation et tests

Prérequis : Java 21, Docker et Ollama avec les deux modèles configurés.

```bash
# Base PostgreSQL + pgvector
docker compose up -d

# Modèles locaux (si absents)
ollama pull nomic-embed-text
ollama pull mistral

# Mode développement Quarkus — mode local (Mistral, base ragdb)
./mvnw compile quarkus:dev

# Mode azure (Azure OpenAI, base ragdb_demo) — nécessite AZURE_OPENAI_* dans l'environnement
./mvnw quarkus:dev -Dquarkus.profile=azure

# Tests (scénarios BDD Cucumber, sans Ollama ni PostgreSQL)
./mvnw test

# Build JVM complet (tests d'intégration ignorés par défaut)
./mvnw verify

# Packaging
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar

# Build natif optionnel
./mvnw package -Dnative
```

Corpus de test : `corpus/` (documents fictifs ou publics uniquement, voir `corpus/README.md`). Jeu d'évaluation de référence : `corpus/exemplia-teletravail.eval.json` (18 questions, réponses attendues, mots-clés, sections, cas hors document). C'est la seule liste de référence : ne pas la recopier ailleurs.

Pour une expérimentation Retrieval, noter le corpus, la question, les documents filtrés, les valeurs de chunking et de Top K, puis comparer les rangs/scores et le contexte exact. Ne conclure à un problème LLM qu'après avoir vérifié que l'information correcte est bien présente dans le contexte envoyé.

## Tests (BDD)

Les tests sont des scénarios Gherkin exécutables avec Cucumber (dépendances `cucumber-java` et `cucumber-junit-platform-engine`, scope `test`, compatibles JUnit 6 fourni par Quarkus).

- Scénarios : `src/test/resources/features/*.feature` — `chat.feature` (garde « aucun document », cas « aucun match ») et `security.feature` (règle `LlmSafetyPolicy`, 6 scénarios).
- Step definitions : `src/test/java/org/acme/service/ChatSteps.java` et `src/test/java/org/acme/config/SecuritySteps.java` (chaque fichier de steps dans le package de la classe testée).
- Runner : `src/test/java/org/acme/service/RunCucumberTest.java` (glue `org.acme`, donc toutes les steps du projet ; plugin `pretty`).

Principes :

- `AiService` est testé sans Ollama ni PostgreSQL : `EmbeddingModel`, `EmbeddingStore` et `ChatLanguageModel` sont des mocks Mockito, injectés dans les champs `@Inject` package-private (les steps sont dans le même package).
- Cucumber crée une nouvelle instance de `ChatSteps` par scénario : chaque scénario a ses propres mocks ; les données passent d'une étape à l'autre par les champs (`question`, `documentIds`, `answer`).
- Scénarios rédigés en anglais, en vocabulaire métier (pas de noms de classes ni de mocks).
- Un scénario décrit le comportement, pas le texte exact affiché : `Then the user is asked to select at least one document`, et c'est la step qui compare à la constante (`RagConstants.NO_DOCUMENT_SELECTED`).
- Réutiliser les phrases existantes avant d'en créer de nouvelles ; une phrase nouvelle nécessite une méthode dans un fichier de steps. Une même phrase ne doit être définie qu'une seule fois dans tout le glue `org.acme`.
- Les règles de sécurité sont des fonctions pures testées sans mock (`SecuritySteps`).

## Roadmap

Les éléments suivants sont des objectifs, pas des composants déjà implémentés :

1. consolider l'observabilité du Retrieval et construire des cas d'évaluation reproductibles ;
2. améliorer la qualité du Retrieval ;
3. ajouter les citations et passages sources ;
4. ajouter l'historique et les conversations multi-tour ;
5. expérimenter la recherche hybride (vectorielle + lexicale) ;
6. ajouter un reranking mesuré ;
7. améliorer le chunking et préserver davantage la structure documentaire ;
8. ajouter des fonctionnalités entreprise (droits, espaces documentaires, feedback, suivi) ;
9. faire évoluer à terme le RAG vers une brique d'agents IA capables d'utiliser des outils.
