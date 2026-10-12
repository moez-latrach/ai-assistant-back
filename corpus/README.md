# Corpus de test du RAG

Documents **fictifs ou publics uniquement**. Ils peuvent être commités, montrés en démo,
collés dans une conversation et envoyés à un modèle cloud (profil `azure`).

> ⚠️ Ne jamais déposer ici un document réel, interne ou confidentiel.
> Les documents confidentiels restent hors du projet et ne sont utilisés qu'en mode `local`.

## exemplia-teletravail.docx

Politique de télétravail d'une entreprise fictive (« Exemplia »). Les règles sont inventées :
un LLM ne peut pas les connaître par ses connaissances générales, donc une bonne réponse
prouve que le Retrieval a trouvé le bon passage.

Le texte est assez long pour produire plusieurs chunks (chunking 500/100).

### Jeu de test : `exemplia-teletravail.eval.json`

18 questions avec la réponse attendue, au format JSON (lisible par un humain, et par du code Java via Jackson
le jour où l'évaluation sera automatisée). Chaque cas contient :

- `question` / `expectedAnswer` : la question et la réponse attendue, en clair ;
- `expectedKeywords` : groupes de mots-clés ; la réponse doit contenir au moins un mot de chaque groupe ;
- `expectedSections` : section(s) du document où se trouve l'information (pour vérifier le Retrieval dans les logs) ;
- `outOfScope` : `true` si la réponse n'est pas dans le document (le RAG doit répondre « aucune information »).

Catégories couvertes :

| Catégorie | Ce qu'elle teste |
|---|---|
| `fait-simple` | retrouver une information explicite |
| `chiffre` | restituer un nombre exact (jours, euros, délai) |
| `regle-implicite` | appliquer une règle (« sans réponse → acceptée ») |
| `negation` | répondre « non » quand le document l'exclut |
| `cas-particulier` | ne pas confondre la règle générale et l'exception (alternants) |
| `multi-informations` | combiner plusieurs éléments d'une réponse |
| `reformulation` | comprendre une question formulée avec d'autres mots que le document |
| `premisse-fausse` | corriger une question qui suppose une règle fausse |
| `hors-document` | ne rien inventer, même sur un sujet proche ou connu du LLM |

Utilisation manuelle (aujourd'hui) : poser chaque question dans le front avec le seul document
`exemplia-teletravail.docx` sélectionné, et comparer à `expectedAnswer`. Les logs (rang, score, chunk)
permettent de voir si la bonne section a été retrouvée.

Ce fichier est la **seule** liste de référence : ne pas recopier les questions ailleurs.
