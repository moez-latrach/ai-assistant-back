# L'évolution du RAG en 2026

# Roadmap

Ce projet implémente un système **RAG (Retrieval-Augmented Generation)** fonctionnel.

L'objectif est désormais de le faire évoluer progressivement vers une solution d'entreprise moderne, puis vers un **AI Agent**.

---

# Version 1 - RAG de base ✅

Fonctionnalités déjà implémentées :

- Import de documents PDF et Word
- Extraction du texte
- Découpage en chunks (Chunking)
- Génération des embeddings
- Stockage dans PostgreSQL + pgvector
- Recherche vectorielle
- Construction du contexte
- Génération de réponses avec un LLM

---

# Version 2 - Amélioration de l'expérience utilisateur

## Historique des conversations (Multi-turn)

Le chatbot conserve le contexte de la conversation.

### Aujourd'hui

```text
Utilisateur :
Qu'est-ce que Kafka ?

Assistant :
...

Utilisateur :
Et les offsets ?

→ Le chatbot ne sait plus que l'on parle de Kafka.
```

### Après

```text
Utilisateur :
Qu'est-ce que Kafka ?

Assistant :
...

Utilisateur :
Et les offsets ?

→ Le chatbot comprend que la question concerne toujours Kafka.
```

---

## Citations des sources

Chaque réponse indique les documents utilisés.

Exemple :

```text
Les offsets permettent...

Sources :

- kafka.pdf (page 12)
- architecture.pdf (page 5)
```

Objectifs :

- améliorer la confiance de l'utilisateur ;
- rendre les réponses vérifiables ;
- faciliter la traçabilité.

---

## Affichage des passages utilisés

Afficher le ou les passages exacts utilisés pour construire la réponse.

Exemple :

```text
Source : kafka.pdf

"Les offsets permettent de suivre..."
```

Cela permet à l'utilisateur de vérifier immédiatement l'origine de la réponse.

---

# Version 3 - Amélioration de la qualité du RAG

## Hybrid Retrieval

Combiner plusieurs méthodes de recherche :

- recherche vectorielle ;
- recherche par mots-clés (BM25, Lucene, Elasticsearch).

Objectif :

- améliorer le rappel (**Recall**) ;
- retrouver davantage de documents pertinents.

---

## Reranking

Après la recherche vectorielle, les résultats sont reclassés avant d'être envoyés au LLM.

Pipeline :

```text
Recherche vectorielle
        │
        ▼
Top 20 résultats
        │
        ▼
Reranker
        │
        ▼
Top 5 résultats
        │
        ▼
LLM
```

Objectif :

- envoyer au LLM les chunks les plus pertinents.

---

## Optimisation du Chunking

Expérimenter différentes stratégies :

- taille des chunks ;
- overlap ;
- découpage par titres ;
- découpage sémantique.

Objectif :

- améliorer la précision de la recherche.

---

# Version 4 - Fonctionnalités d'entreprise

## Streaming

Afficher la réponse progressivement, comme ChatGPT.

---

## Upload multiple

Importer plusieurs documents simultanément.

---

## Espaces documentaires

Organiser les documents par domaine :

- RH
- Technique
- Juridique
- Commercial

---

## Recherche par espace

Limiter les recherches à un domaine spécifique.

Exemple :

```text
☑ RH
☐ Technique
☐ Juridique
```

---

## Authentification

Gestion des utilisateurs.

---

## Gestion des droits

Exemple :

- un utilisateur RH ne peut consulter que les documents RH ;
- un utilisateur Finance ne peut consulter que les documents Finance.

---

## Dashboard

Afficher des statistiques :

- nombre de documents ;
- nombre de chunks ;
- nombre de questions ;
- temps moyen de réponse.

---

## Feedback utilisateur

Permettre d'évaluer une réponse.

Exemple :

```text
👍 Réponse utile

👎 Réponse incorrecte
```

Ces retours peuvent être utilisés pour améliorer le système.

---

# Version 5 - AI Agent

Le RAG devient une composante d'un Agent IA.

Le système peut non seulement répondre, mais également effectuer des actions.

Exemples :

- consulter des documents ;
- appeler une API REST ;
- interroger une base SQL ;
- créer un ticket Jira ;
- envoyer un e-mail ;
- planifier un rendez-vous ;
- déclencher un workflow métier.

Architecture cible :

```text
Utilisateur
      │
      ▼
   Agent IA
      │
      ├───────────────┐
      ▼               ▼
     RAG            API REST
      │               │
      ▼               ▼
Base vectorielle   Base SQL
      │               │
      └──────┬────────┘
             ▼
     Outils externes
             │
             ▼
      Réponse / Action
```

---

# Objectif final

Construire une plateforme d'assistance documentaire capable de :

- répondre à des questions à partir des documents de l'entreprise ;
- citer précisément ses sources ;
- conserver le contexte d'une conversation ;
- rechercher efficacement les informations pertinentes ;
- interagir avec les systèmes d'information de l'entreprise ;
- automatiser certaines tâches métier grâce à un Agent IA.


___ 

# Future

Le **RAG (Retrieval-Augmented Generation)** est aujourd'hui une technologie largement adoptée. En 2026, il n'est plus considéré comme une innovation à lui seul.

Les entreprises cherchent désormais à construire des **AI Agents**, capables non seulement de répondre à une question, mais également d'interagir avec différents systèmes d'information et d'exécuter des actions.

---

# Avant

```text
Utilisateur
      │
      ▼
     RAG
      │
      ▼
   Réponse
```

Le système répond uniquement aux questions de l'utilisateur.

---

# Aujourd'hui (2026)

```text
Utilisateur
      │
      ▼
   Agent IA
      │
      ├───────────────┐
      ▼               ▼
     RAG            API REST
      │               │
      ▼               ▼
 Base vectorielle   Base SQL
      │               │
      └──────┬────────┘
             ▼
     Outils externes
 (Email, Jira, CRM, ERP...)
             │
             ▼
         Réponse / Action
```

L'IA ne se contente plus de générer une réponse.

Elle est capable d'analyser une demande, de récupérer des informations provenant de plusieurs sources, puis d'exécuter des actions.

---

# Exemple concret

Un employé demande :

> Je veux poser trois jours de congés en août.

L'agent IA peut alors :

1. consulter la documentation RH grâce au **RAG** ;
2. vérifier le solde de congés via une **API** ;
3. consulter le calendrier de l'utilisateur ;
4. créer automatiquement la demande dans le SIRH ;
5. confirmer l'opération à l'utilisateur.

Dans cette architecture, le **RAG n'est qu'une brique** parmi d'autres.

---

# Ce que maîtrise déjà ce projet

Ce projet implémente toutes les briques fondamentales d'un système RAG :

- ingestion de documents ;
- extraction du texte ;
- découpage en chunks (chunking) ;
- génération des embeddings ;
- stockage dans une base vectorielle ;
- recherche vectorielle ;
- construction du contexte ;
- création du prompt ;
- génération de la réponse par le LLM.

Ces compétences constituent une excellente base pour évoluer vers des systèmes d'IA plus avancés.

---

# Évolutions possibles

## 1. Hybrid Retrieval

Combiner plusieurs stratégies de recherche :

- recherche vectorielle ;
- recherche par mots-clés (BM25, Lucene, Elasticsearch).

Objectif :

> améliorer le rappel (recall) et la pertinence des résultats.

---

## 2. Reranking

Après la recherche vectorielle, un modèle de reranking reclasse les résultats avant leur envoi au LLM.

Objectif :

> améliorer la qualité des chunks fournis au modèle.

---

## 3. Conversations multi-turn

Le chatbot conserve le contexte des échanges précédents.

Exemple :

**Utilisateur :**

> Explique Kafka.

Puis :

> Et comment gérer les offsets ?

Le système comprend que la deuxième question fait référence à Kafka sans que l'utilisateur ait besoin de le préciser.

---

## 4. Citations des sources

Le chatbot indique précisément l'origine des informations.

Exemple :

```text
Selon SQL.pdf, page 12...
```

Cela améliore :

- la confiance des utilisateurs ;
- la traçabilité des réponses ;
- la vérifiabilité des informations.

---

## 5. AI Agent

Le système ne se limite plus à répondre.

Il peut également :

- consulter des documents ;
- appeler des API ;
- interroger une base de données ;
- créer un ticket Jira ;
- envoyer un e-mail ;
- planifier un rendez-vous ;
- déclencher un workflow métier ;
- exécuter automatiquement une action.

Le RAG devient alors une composante d'un **Agent IA**, capable de raisonner, de prendre des décisions et d'interagir avec l'écosystème informatique de l'entreprise.

