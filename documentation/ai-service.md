# AiService

## Objectif

Le `AiService` est responsable de **la recherche d'informations** dans la base vectorielle et de **la génération de la réponse**.

Contrairement à `DocumentFileService`, ce service ne prépare pas les données.

Son rôle est de répondre à la question suivante :

> Comment retrouver les informations pertinentes dans les documents afin de répondre à une question utilisateur ?

---

# Responsabilité du service

Le `AiService` répond à la question suivante :

> Comment retrouver les bonnes informations afin de répondre à une question utilisateur ?

Il est responsable de :

- transformer la question en embedding ;
- effectuer la recherche vectorielle ;
- filtrer les documents ;
- construire le contexte ;
- créer le prompt ;
- appeler le LLM.

Il ne réalise jamais :

- l'extraction du texte ;
- le découpage des documents ;
- la création des embeddings des documents.

Ces responsabilités appartiennent à `DocumentFileService`.

---

# Vue d'ensemble

Le pipeline est le suivant :

```text
Question utilisateur
        │
        ▼
Embedding de la question
        │
        ▼
Recherche vectorielle
        │
        ▼
Filtrage des documents
        │
        ▼
Récupération des meilleurs chunks
        │
        ▼
Construction du contexte
        │
        ▼
Création du prompt
        │
        ▼
Appel du LLM
        │
        ▼
Réponse finale
```

---

# Étape 1 : Réception de la question

```java
public String ask(String question, List<UUID> documentIds)
```

Le frontend envoie :

- la question de l'utilisateur ;
- les documents sélectionnés.

Exemple :

Question :

```
Comment garantir la cohérence des transactions ?
```

Documents sélectionnés :

```
sql.docx
```

---

# Étape 2 : Embedding de la question

```java
var embedding = embeddingModel.embed(question).content();
```

La question est transformée en vecteur numérique.

Exemple :

Entrée :

```
Comment garantir la cohérence des transactions ?
```

Sortie :

```
[0.18,
-0.52,
0.94,
...]
```

Ce vecteur représente le sens de la question.

Le texte n'est plus utilisé pour la recherche.

---

# Étape 3 : Construction de la recherche

```java
EmbeddingSearchRequest
```

Le système prépare une requête destinée à la base vectorielle.

Configuration actuelle :

```java
.maxResults(3)
```

Le moteur retournera au maximum les **3 chunks les plus proches**.

---

# Étape 4 : Filtrage des documents

```java
.filter(...)
```

La recherche est limitée aux documents sélectionnés par l'utilisateur.

Exemple :

Documents présents :

- kafka.pdf
- sql.docx
- java.pdf

L'utilisateur sélectionne uniquement :

```
sql.docx
```

La recherche ne sera effectuée que dans les embeddings appartenant à ce document.

Les autres documents sont ignorés.

---

# Étape 5 : Recherche vectorielle

```java
EmbeddingSearchResult<TextSegment> result =
        embeddingStore.search(request);
```

La base vectorielle compare :

- le vecteur de la question ;
- les vecteurs de tous les chunks.

Chaque chunk reçoit un score de similarité.

Exemple :

| Chunk | Score |
|--------|-------|
| Transactions SQL | 0.93 |
| Isolation Level | 0.90 |
| Kafka Producer | 0.61 |

Les meilleurs résultats sont retournés.

---

# Comprendre le score de similarité

Lors de la recherche vectorielle, chaque chunk reçoit un **score de similarité**.

Exemple :

| Rang | Score | Chunk |
|------|------:|-------|
| 1 | 0.91 | Chunk A |
| 2 | 0.88 | Chunk B |
| 3 | 0.84 | Chunk C |

Le moteur de recherche retourne les chunks ayant les scores les plus élevés.

⚠️ **Attention : le chunk ayant le meilleur score n'est pas forcément celui qui contient la meilleure réponse.**

Le score mesure uniquement :

- la proximité sémantique entre la question et le chunk.

Il **ne mesure pas** :

- si le chunk contient la réponse complète ;
- si la réponse est plus précise ;
- si la réponse est plus explicite.

Par conséquent, il est tout à fait normal que le LLM construise sa réponse principalement à partir du **deuxième** ou du **troisième** chunk.

Exemple :

```
Résultats du Retrieval

1 → Score : 0.91
Chunk : Introduction aux transactions...

2 → Score : 0.88
Chunk : Les propriétés ACID garantissent la cohérence...

3 → Score : 0.84
Chunk : Exemple d'utilisation...
```

Question :

```
Comment garantir la cohérence des transactions ?
```

Le Retrieval classe le premier chunk comme le plus proche de la question.

Cependant, le LLM peut choisir de répondre principalement à partir du deuxième chunk, car il contient l'information la plus pertinente :

> La cohérence est garantie par les propriétés ACID.

C'est pourquoi un RAG récupère généralement plusieurs chunks (`Top K`) au lieu d'un seul.

Le **Retrieval** répond à la question :

> Quels sont les chunks les plus proches de la question ?

Le **LLM** répond ensuite à la question :

> Parmi ces chunks, lesquels contiennent les informations les plus utiles pour construire une réponse ?

---

# Étape 6 : Filtrage des résultats

Actuellement :

```java
matches = result.matches();
```

Tous les résultats sont conservés.


# Étape 7 : Vérification

```java
if(matches.isEmpty())
```

Si aucun chunk pertinent n'est trouvé :

Le système retourne :

```
Information introuvable dans les documents.
```

Le LLM n'est pas appelé.

---

# Étape 8 : Construction du contexte

```java
String context = ...
```

Les chunks déjà triés par similarité par la base vectorielle sont récupérés, puis leurs textes sont concaténés afin de construire le contexte envoyé au LLM
Exemple :

```
Chunk 1

Transactions...

Chunk 2

Isolation level...
```

↓

```
Transactions...

Isolation level...
```

Ce contexte représente les connaissances qui seront envoyées au LLM.

___

# Étape 9 : Création du prompt

```java
PROMPT_TEMPLATE
```

Le prompt contient :

- les instructions ;
- le contexte ;
- la question utilisateur.

Exemple :

```
Contexte :

Transactions...

Isolation level...

Question :

Comment garantir la cohérence ?
```

Le prompt indique également au LLM :

- de répondre uniquement à partir du contexte ;
- de ne pas utiliser ses connaissances générales ;
- de répondre "Information introuvable" si le contexte ne contient pas la réponse.

---

# Étape 10 : Génération de la réponse

```java
chatModel.generate(prompt)
```

Le LLM lit :

- le contexte ;
- les instructions ;
- la question.

Il génère ensuite une réponse en langage naturel.

---

# Résumé

Le pipeline complet est :

```
Question

↓

Embedding de la question

↓

Recherche vectorielle

↓

Top 3 chunks

↓

Construction du contexte

↓

Prompt

↓

LLM

↓

Réponse
```

---
