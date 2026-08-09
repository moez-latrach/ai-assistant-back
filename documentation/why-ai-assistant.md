# Pourquoi le RAG ?

## Avant les LLM (avant 2022)

Les entreprises disposaient déjà d'assistants internes, mais leur fonctionnement était très différent des solutions actuelles.

### Exemple

**Employé :**

> Comment poser des RTT ?

**Chatbot :**

> Voir la procédure RH.

### Comment fonctionnaient-ils ?

Ces assistants reposaient principalement sur :

- des règles (`if/else`) ;
- des FAQ ;
- des arbres de décision ;
- des moteurs de recherche par mots-clés (par exemple Elasticsearch).

### Les limites

Si l'utilisateur écrivait :

> Comment prendre des congés ?

Le chatbot pouvait répondre correctement.

En revanche, avec une formulation différente :

> Comment poser mes vacances ?

Le chatbot pouvait ne pas trouver de réponse.

Pourquoi ?

Parce qu'il ne comprenait pas le sens de la question. Il recherchait principalement des **mots-clés**.

---

# Arrivée des LLM (2022-2023)

Avec l'arrivée de modèles comme **ChatGPT**, **Claude** ou **Gemini**, une nouvelle génération d'assistants est apparue.

Les LLM comprennent le langage naturel.

### Exemple

**Utilisateur :**

> Explique Kafka comme à un débutant.

Le LLM est capable de produire une explication claire, même si la formulation est différente de celles rencontrées pendant son entraînement.

## Mais un problème apparaît

Les LLM ne connaissent pas les informations internes d'une entreprise.

### Exemple

**Utilisateur :**

> Quel est le processus de validation des congés chez ACME ?

Le modèle peut :

- répondre correctement par hasard ;
- inventer une réponse plausible mais fausse.

Ce phénomène est appelé une **hallucination**.

---

# Arrivée du RAG

Le **Retrieval-Augmented Generation (RAG)** ajoute une étape avant l'appel au LLM.

### Sans RAG

```text
Question
    │
    ▼
   LLM
    │
    ▼
Réponse
```

Le modèle répond uniquement avec les connaissances acquises lors de son entraînement.

### Avec RAG

```text
Question
    │
    ▼
Recherche dans les documents
    │
    ▼
Contexte
    │
    ▼
   LLM
    │
    ▼
Réponse
```

Le LLM ne répond plus uniquement à partir de sa mémoire.

Il construit sa réponse en s'appuyant sur les documents de l'entreprise.

---

# Pourquoi est-ce une révolution ?

Imaginons une entreprise possédant :

- 10 000 procédures PDF ;
- 5 000 documents Word ;
- une documentation Confluence ;
- des contrats ;
- une documentation technique.

## Avant

Un collaborateur devait :

- rechercher les documents ;
- ouvrir plusieurs fichiers ;
- parcourir les différentes procédures ;
- trouver lui-même la réponse.

## Aujourd'hui

L'utilisateur pose simplement une question :

> Comment déployer un microservice Quarkus ?

Le système RAG :

1. recherche les documents pertinents ;
2. sélectionne les meilleurs passages (chunks) ;
3. fournit ces informations au LLM ;
4. génère une réponse claire en quelques secondes.

---

# Ce que réalise ce projet

Ce projet implémente un pipeline RAG complet.

```text
Documents
    │
    ▼
Extraction du texte
    │
    ▼
Découpage en chunks
    │
    ▼
Génération des embeddings
    │
    ▼
Stockage dans une base vectorielle
    │
    ▼
Question utilisateur
    │
    ▼
Recherche vectorielle
    │
    ▼
Récupération des meilleurs chunks
    │
    ▼
Construction du contexte
    │
    ▼
LLM
    │
    ▼
Réponse
```

Ce projet est donc une implémentation complète d'un système **Retrieval-Augmented Generation (RAG)**, capable de répondre à des questions en s'appuyant sur les documents de l'entreprise plutôt que uniquement sur les connaissances générales du modèle de langage.