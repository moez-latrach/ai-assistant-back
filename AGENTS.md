# AGENTS.md

## Objet du projet

Ce repository contient le backend d'un assistant documentaire RAG (Retrieval-Augmented Generation). Il ingère des documents PDF ou DOCX, indexe leurs fragments dans PostgreSQL/pgvector, puis répond à une question à partir des fragments retrouvés et d'un LLM local servi par Ollama.

Le projet est actuellement un RAG simple, synchrone et mono-tour. Il n'implémente pas encore les citations, le reranking, la recherche hybride, l'historique de conversation, l'authentification ni des agents capables d'exécuter des actions.

Le code courant est la source de vérité. La documentation sous `documentation/` explique les intentions, mais certaines valeurs peuvent être anciennes (notamment le chunking et le Top K).

## Stack et services externes

- Java 21 et Maven Wrapper.
- Quarkus 3.35.2 : CDI/Arc, REST/Jackson, REST multipart, Hibernate ORM Panache, JDBC PostgreSQL et OpenAPI.
- LangChain4j 0.36.2.
- Ollama sur `http://localhost:11434` :
  - embeddings : `nomic-embed-text` ;
  - chat : `mistral`.
- PostgreSQL 16 avec pgvector, exposé sur `localhost:5432` par `docker-compose.yml`.
- Modèle d'embedding configuré sur 768 dimensions.
- PDFBox 3.0.2 pour les PDF et Apache POI 5.4.0 pour les DOCX.

Les paramètres Ollama et pgvector sont codés dans `LangChainConstants`. Les paramètres ORM/CORS sont dans `application.properties`. Les identifiants de développement PostgreSQL sont actuellement `userapp/password`, base `ragdb`. Ne pas les considérer comme une configuration de production.

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
Question
    ↓
Embedding nomic-embed-text
    ↓
Vector Search filtrée par documentId
    ↓
Top K chunks (maxResults = 10 dans le code courant)
    ↓
Concaténation, séparée par deux retours à la ligne
    ↓
Contexte injecté dans PROMPT_TEMPLATE
    ↓
LLM Mistral
    ↓
Answer
```

`AiService.ask()` construit toujours un filtre `IsIn(documentId, documentIds)`. Ne pas supposer qu'une liste vide recherche réellement dans tous les documents sans le vérifier : le log affiche actuellement `ALL`, mais le filtre reste construit avec la liste vide.

Tous les matches renvoyés sont conservés, dans leur ordre de ranking. Aucun seuil de score actif et aucun reranker ne sont appliqués. La ligne de filtre à `0.85` visible dans le code est commentée. Si aucun match n'est retourné, le service renvoie `NO_INFORMATION_FOUND` sans appeler le LLM.

## Classes principales

- `LangChainConfig` : producteurs CDI pour `EmbeddingModel`, `ChatLanguageModel` et `PgVectorEmbeddingStore` (table `embeddings`, créée si nécessaire).
- `LangChainConstants` : configuration locale Ollama/pgvector.
- `DocumentResource` : endpoints d'upload et de listing.
- `AiResource` : endpoint de chat.
- `DocumentFileService` : persistance du document, extraction, chunking, enrichissement, embedding et stockage.
- `DocumentExtractor` : extension SPI des formats supportés.
- `PdfExtractor` / `WordExtractor` : implémentations PDF et DOCX.
- `AiService` : embedding de question, recherche vectorielle, contexte, prompt et génération.
- `DocumentRepository` : `PanacheRepository<DocumentFile>`.
- `DocumentFile` : entité de la table `documents`.
- DTO records : `ChatRequest`, `ChatResponse`, `UploadResponse`, `DocumentResponse`.
- `RagConstants` : clés de métadonnées, texte de repli et prompt. `SEARCH_MAX_RESULTS` et `CHUNK_SEPARATOR_REGEX` ne pilotent pas le flux courant.

## État actuel et diagnostic du Retrieval

Le pipeline de base fonctionne et les logs exposent l'ingestion, les résultats classés, le contexte et la réponse. Le travail en cours porte sur la qualité du Retrieval.

Problème observé : un chunk contenant la bonne information peut être présent dans pgvector mais classé trop bas pour entrer dans le Top K. Augmenter `maxResults` peut alors le faire apparaître. Avant toute correction, localiser précisément la défaillance :

1. **Extraction** : l'information existe-t-elle dans le texte extrait ?
2. **Chunking** : existe-t-elle dans un chunk exploitable, sans séparation destructive ?
3. **Embedding** : la représentation sémantique est-elle adaptée au contenu et à la question ?
4. **Retrieval/ranking** : le chunk existe-t-il mais arrive-t-il trop bas ? Quels sont son rang et son score ?
5. **Génération** : le bon chunk est-il dans le contexte exact, mais la réponse reste-t-elle incorrecte ?

Points connus à garder visibles :

- `AiService` recherche actuellement avec `maxResults(10)`, tandis que le bloc de log annonce encore `20` : c'est une incohérence d'observabilité à corriger séparément et à tester ;
- `RagConstants.SEARCH_MAX_RESULTS` vaut `1`, mais n'est pas utilisé par la requête ;
- la documentation existante mentionne encore d'anciennes valeurs (`250/50` et Top 3) ;
- aucun test automatisé n'est actuellement présent sous `src/test` malgré les dépendances Quarkus JUnit, REST Assured et Mockito ; `./mvnw test` valide donc surtout compilation et démarrage des phases Maven.

## Observabilité à préserver

Ne pas supprimer les logs de diagnostic qui permettent de relier ingestion et Retrieval. Ils doivent conserver au minimum :

- à l'ingestion : `chunkIndex`, document, nombre de caractères et texte complet du chunk ;
- à la recherche : question, filtre documentaire et Top K demandé ;
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
12. Ne pas committer des fichiers locaux ou générés (`target/`, `.idea/`, `effective-pom.xml`, corpus de test) sans intention explicite.

## Conventions constatées

- Packages sous `org.acme`, composants métier `@ApplicationScoped` et injection CDI par `@Inject`.
- Endpoints Jakarta REST regroupés dans `controller`, chemins dans `ApiPaths`.
- DTO simples sous forme de records Java ; entités JPA et repositories Panache séparés.
- Constantes RAG et clés de métadonnées centralisées dans `RagConstants`.
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

# Mode développement Quarkus
./mvnw compile quarkus:dev

# Tests et compilation des tests
./mvnw test

# Build JVM complet (tests d'intégration ignorés par défaut)
./mvnw verify

# Packaging
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar

# Build natif optionnel
./mvnw package -Dnative
```

Pour une expérimentation Retrieval, noter le corpus, la question, les documents filtrés, les valeurs de chunking et de Top K, puis comparer les rangs/scores et le contexte exact. Ne conclure à un problème LLM qu'après avoir vérifié que l'information correcte est bien présente dans le contexte envoyé.

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
