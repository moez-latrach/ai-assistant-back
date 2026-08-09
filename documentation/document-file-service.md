# DocumentFileService

## Objectif

Le `DocumentFileService` est responsable de **l'ingestion des documents**.

Son rôle est de transformer un document (PDF, DOCX...) en une base de connaissances exploitable par le moteur RAG.

> Ce service **ne répond jamais aux questions des utilisateurs**.
>
> Il prépare simplement les données qui seront utilisées plus tard par `AiService`.

---

# Vue d'ensemble

Le pipeline d'ingestion est le suivant :

```text
Upload du fichier
        │
        ▼
Sauvegarde des métadonnées
        │
        ▼
Extraction du texte
        │
        ▼
Découpage en chunks
        │
        ▼
Ajout des métadonnées
        │
        ▼
Création des embeddings
        │
        ▼
Stockage dans la base vectorielle
```

---

# Étape 1 : Upload

```java
public UploadResponse upload(FileUpload file)
```

Cette méthode est le point d'entrée.

Elle reçoit un fichier envoyé par le frontend.

Exemple :

```
sql.docx
```

À ce stade :

- aucun texte n'est extrait ;
- aucun embedding n'existe encore.

---

# Étape 2 : Sauvegarde du document

```java
DocumentFile document = new DocumentFile();

document.setFilename(file.fileName());

document.setUploadedAt(LocalDateTime.now());

documentRepository.persist(document);
```

Cette étape crée une ligne dans la table `documents`.

Exemple :

| id | filename | uploadedAt |
|----|----------|------------|
| 17 | sql.docx | ... |

Cette table contient uniquement les informations du document.

Elle sert notamment à :

- afficher la liste des documents dans Angular ;
- supprimer un document ;
- retrouver le nom du document.

---

# Étape 3 : Extraction du texte

```java
String text = extract(file.fileName(), inputStream);
```

Le document est transformé en texte brut.

### Exemple :

```
PDF
```

↓

```
Kafka est une plateforme...

Producer...

Consumer...
```

Le choix du bon extracteur est réalisé automatiquement grâce à :

```java
extractors.stream()
```

Chaque implémentation de `DocumentExtractor` indique si elle supporte le format du fichier.

Exemple :

- PdfExtractor
- WordExtractor

---

# Étape 4 : Début de l'ingestion

```java
ingest(text, document.getId(), document.getFilename());
```

Cette méthode transforme le texte en données exploitables par le moteur RAG.

---

# Étape 5 : Création d'un Document LangChain4j

```java
Document parsedDocument = Document.from(text);
```

Cette étape encapsule simplement le texte dans un objet `Document`.

Aucune intelligence artificielle n'est utilisée ici.

---

# Étape 6 : Chunking

```java
List<TextSegment> segments = splitter.split(parsedDocument);
```

Le document est découpé en plusieurs morceaux (chunks).

Configuration actuelle :

```java
DocumentSplitters.recursive(250, 50)
```

Cela signifie :

- environ 250 caractères par chunk ;
- 50 caractères d'overlap entre deux chunks.

Exemple :

```
Document complet

↓

Chunk 1

Chunk 2

Chunk 3

...

Chunk N
```

Pourquoi découper ?

Parce qu'un document complet est trop volumineux pour être recherché efficacement.

Le moteur RAG retrouvera uniquement les chunks pertinents.

---

# Étape 7 : Ajout des métadonnées

```java
Metadata metadata = new Metadata();

metadata.put("documentId", documentId.toString());

metadata.put("filename", filename);
```

Chaque chunk reçoit des informations supplémentaires.

Exemple :

```
Chunk

↓

documentId = 17

filename = sql.docx
```

Ces métadonnées permettent ensuite :

- de filtrer les recherches ;
- d'identifier le document d'origine ;
- de gérer la sélection des documents dans l'interface.

---

# Étape 8 : Création des embeddings

```java
embeddingModel.embed(enrichedSegment)
```

C'est la première véritable étape d'intelligence artificielle.

Le modèle transforme un texte en vecteur numérique.

Exemple :

Entrée :

```
Kafka possède un Producer...
```

Sortie :

```
[-0.18,
 0.42,
 0.91,
 ...]
```

Ce vecteur représente le sens du texte.

---

# Étape 9 : Stockage

```java
embeddingStore.add(...)
```

Le système enregistre :

- le vecteur ;
- le texte du chunk ;
- les métadonnées.

Ces données sont stockées dans la base vectorielle (`EmbeddingStore`).

Le document devient alors interrogeable.

---

# Pourquoi deux tables ?

## documents

Contient :

- id
- filename
- uploadedAt

Cette table représente les documents.

---

## embedding_store

Contient :

- embedding
- texte du chunk
- métadonnées

Cette table représente les connaissances extraites des documents.

Un document peut produire plusieurs centaines de chunks.

Séparer les deux tables évite de répéter inutilement le nom du document sur chaque chunk.

---

# Responsabilité du service

Le `DocumentFileService` répond uniquement à la question suivante :

> Comment transformer un document en une base de connaissances interrogeable ?

Il ne réalise :

- aucune recherche ;
- aucune génération de réponse ;
- aucune interaction avec le LLM.

Ces responsabilités appartiennent à `AiService`.

---

# Résumé

Le pipeline complet est :

```
Document

↓

Extraction du texte

↓

Découpage en chunks

↓

Ajout des métadonnées

↓

Création des embeddings

↓

Stockage dans la base vectorielle

↓

Document prêt à être interrogé
```