# NL-SQL-Engine — Documentation technique

## Table des matières

1. [Vue d'ensemble](#vue-densemble)
2. [Workflow global](#workflow-global)
3. [Briques du projet](#briques-du-projet)
   - [API REST](#api-rest)
   - [Orchestrateur](#orchestrateur)
   - [RAG — Retrieval Augmented Generation](#rag--retrieval-augmented-generation)
   - [LLM Client & Prompt Builder](#llm-client--prompt-builder)
   - [SQL Validator](#sql-validator)
   - [SQL Executor](#sql-executor)
   - [Import de données (SWAPI)](#import-de-données-swapi)
4. [Modèle de données](#modèle-de-données)
5. [Infrastructure & Configuration](#infrastructure--configuration)

---

## Vue d'ensemble

NL-SQL-Engine est une application **Quarkus** qui transforme une question posée en langage naturel en une requête SQL valide, la valide, l'exécute en lecture seule sur une base PostgreSQL, puis formule une réponse en français à partir des résultats.

Le domaine de démonstration est l'univers **Star Wars**, avec des données importées depuis l'API publique SWAPI.

---

## Workflow global

Le schéma ci-dessous décrit le parcours complet d'une requête, de la réception HTTP jusqu'à la réponse en langage naturel.

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Requête utilisateur                          │
│              POST /api/query  { "question": "..." }                 │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
                 ┌─────────────────────────┐
                 │     QueryResource        │
                 │  (validation entrée)     │
                 └────────────┬────────────┘
                              │
                              ▼
                 ┌─────────────────────────┐
                 │    NlSqlOrchestrator     │
                 │  (chef d'orchestre)      │
                 └────────────┬────────────┘
                              │
              ┌───────────────┼───────────────┐
              │                               │
              ▼                               ▼
┌──────────────────────┐        ┌──────────────────────┐
│   ContextRetriever   │        │    SchemaProvider     │
│  (recherche RAG)     │        │  (règles métier)     │
│                      │        │                      │
│ 1. Embed la question │        │ Retourne 6 règles    │
│ 2. Cherche dans      │        │ métier codées en dur │
│    pgvector (top 5)  │        └──────────┬───────────┘
│ 3. Retourne segments │                   │
│    de schéma         │                   │
└──────────┬───────────┘                   │
           │                               │
           └───────────────┬───────────────┘
                           │
                           ▼
              ┌────────────────────────┐
              │  Boucle de génération  │
              │  (max 3 tentatives)    │
              │                        │
              │  ┌──────────────────┐  │
              │  │  PromptBuilder   │  │
              │  │  Construit le    │  │
              │  │  prompt système  │  │
              │  │  + utilisateur   │  │
              │  └───────┬──────────┘  │
              │          │             │
              │          ▼             │
              │  ┌──────────────────┐  │
              │  │    LlmClient     │  │
              │  │  Appel Ollama /  │  │
              │  │  Mistral 7B      │  │
              │  │  → SQL brut      │  │
              │  └───────┬──────────┘  │
              │          │             │
              │          ▼             │
              │  ┌──────────────────┐  │
              │  │   SqlValidator   │  │
              │  │  JSQLParser +    │  │
              │  │  règles sécu     │  │
              │  └───────┬──────────┘  │
              │          │             │
              │    valide ? ─── non ──►│ retry
              │          │             │
              └──────────┼─────────────┘
                         │ oui
                         ▼
              ┌──────────────────────┐
              │     SqlExecutor      │
              │  Exécution en        │
              │  lecture seule       │
              │  (timeout 30s,       │
              │   max 100 lignes)    │
              └──────────┬───────────┘
                         │
                         ▼
              ┌──────────────────────┐
              │     LlmClient        │
              │  2e appel LLM :      │
              │  formulation de la   │
              │  réponse en français │
              └──────────┬───────────┘
                         │
                         ▼
              ┌──────────────────────┐
              │   Réponse HTTP 200   │
              │  {                   │
              │    question,         │
              │    generatedSql,     │
              │    results,          │
              │    answer            │
              │  }                   │
              └──────────────────────┘
```

### Résumé du workflow en étapes

| Étape | Composant | Action |
|-------|-----------|--------|
| 1 | `QueryResource` | Reçoit la requête HTTP, valide que la question n'est pas vide |
| 2 | `ContextRetriever` | Transforme la question en vecteur (embedding), recherche les segments de schéma les plus pertinents dans pgvector |
| 3 | `SchemaProvider` | Fournit les règles métier (conventions de jointure, formats de données) |
| 4 | `PromptBuilder` | Assemble le prompt système (règles de génération SQL) et le prompt utilisateur (schéma + règles + question) |
| 5 | `LlmClient` | Envoie les prompts à Mistral 7B via Ollama, extrait le SQL de la réponse |
| 6 | `SqlValidator` | Vérifie que le SQL est un SELECT valide, sans commandes interdites ni `SELECT *` |
| 7 | *Retry* | Si le SQL est invalide, retour à l'étape 4 (max 3 tentatives) |
| 8 | `SqlExecutor` | Exécute le SQL validé en lecture seule sur PostgreSQL |
| 9 | `LlmClient` | Deuxième appel LLM pour formuler une réponse en français à partir des résultats |
| 10 | `QueryResource` | Retourne la réponse JSON complète au client |

---

## Briques du projet

### API REST

**Classe** : `com.yourorg.nlsqlengine.api.QueryResource`
**Endpoint** : `POST /api/query`

#### Fonctionnel

L'API REST est le point d'entrée unique de l'application pour les requêtes en langage naturel. Elle expose une interface HTTP JSON simple permettant à un frontend ou à tout client HTTP d'envoyer une question et de recevoir le SQL généré ainsi que les résultats.

#### Principe de fonctionnement

- **Entrée** : Un objet JSON `QueryRequest` contenant un champ `question` (string).
- **Validation** : Le endpoint vérifie que la requête n'est ni nulle ni vide. Si invalide, il retourne un HTTP 400.
- **Délégation** : La logique métier est entièrement déléguée à `NlSqlOrchestrator.process()`.
- **Sortie** : Un objet JSON `QueryResponse` contenant :
  - `question` — la question originale
  - `generatedSql` — le SQL généré et validé
  - `results` — les données retournées par l'exécution (liste de maps clé/valeur)
  - `answer` — la réponse formulée en français par le LLM
  - `error` — message d'erreur le cas échéant (null si succès)
- **Gestion d'erreurs** : Un `ErrorMapper` global intercepte les exceptions non gérées et retourne un HTTP 500 avec un message d'erreur structuré.

**Endpoints complémentaires** (`ImportResource`) :
- `POST /api/import` — déclenche l'import complet des données Star Wars depuis SWAPI
- `POST /api/import/embeddings` — ingère les descriptions de schéma dans le vector store pgvector

**DTOs** :
- `QueryRequest` — record Java : `(String question)`
- `QueryResponse` — record Java : `(String question, String generatedSql, List<Map<String, Object>> results, String answer, String error)`

---

### Orchestrateur

**Classe** : `com.yourorg.nlsqlengine.orchestration.NlSqlOrchestrator`

#### Fonctionnel

L'orchestrateur est le **chef d'orchestre** du pipeline NL→SQL. Il coordonne l'ensemble des briques — récupération de contexte, construction de prompt, appel au LLM, validation SQL, exécution et formulation de la réponse — dans un flux séquentiel avec logique de retry.

#### Principe de fonctionnement

1. **Récupération du contexte** : Appelle `ContextRetriever` pour obtenir les segments de schéma pertinents à la question posée, puis `SchemaProvider` pour les règles métier.

2. **Boucle de génération (max 3 tentatives)** :
   - Construit le prompt et appelle le LLM pour générer du SQL.
   - Soumet le SQL au validateur.
   - Si le SQL est rejeté, relance une nouvelle tentative.
   - Si toutes les tentatives échouent, retourne un `OrchestratorResult.error()`.

3. **Exécution** : Si le SQL est valide, l'exécute via `SqlExecutor` en lecture seule.

4. **Formulation de la réponse** : Effectue un second appel LLM pour transformer les résultats SQL bruts en une réponse rédigée en français.

5. **Retour** : Encapsule le tout dans un `OrchestratorResult` (question, SQL, résultats, réponse ou erreur).

**Record associé** : `OrchestratorResult(String question, String generatedSql, List<Map<String, Object>> results, String answer, String error)` avec factory methods `success()` et `error()`.

---

### RAG — Retrieval Augmented Generation

**Classes** : `ContextRetriever`, `SchemaEmbeddingService`, `SchemaProvider`

#### Fonctionnel

Le système RAG permet de fournir au LLM uniquement le **contexte pertinent** pour chaque question, plutôt que l'intégralité du schéma. Cela améliore la qualité de la génération SQL en réduisant le bruit et en respectant les limites de contexte du modèle.

#### Concepts clés

**Qu'est-ce qu'un embedding (plongement vectoriel) ?**

Un modèle d'embedding est un réseau de neurones entraîné à transformer du texte en un **vecteur numérique** — c'est-à-dire une liste ordonnée de nombres à virgule flottante (ex. `[0.12, -0.87, 0.45, ...]`). Dans notre cas, chaque texte est représenté par un vecteur de **4096 nombres**.

L'intérêt fondamental : deux textes qui parlent du même sujet produisent des vecteurs **proches** dans cet espace à 4096 dimensions, même si les mots utilisés sont différents. Par exemple :
- *"Quels personnages viennent de Tatooine ?"* et la description de la table `people` avec sa colonne `homeworld_id` produiront des vecteurs proches, car le modèle a appris que ces deux textes sont sémantiquement liés.
- En revanche, la description de la table `starships` produira un vecteur éloigné, car elle n'a pas de rapport avec les origines des personnages.

Cette proximité se mesure mathématiquement par la **similarité cosinus** — un score entre 0 (aucun rapport) et 1 (sens identique).

Le modèle d'embedding utilisé dans ce projet est `e5-mistral-7b-instruct` (quantifié en Q4_0), exécuté localement via Ollama. Il produit des vecteurs de dimension 4096.

**Pourquoi un modèle d'embedding plutôt qu'une recherche par mots-clés ?**

Une recherche textuelle classique (LIKE, full-text search) ne trouverait que des correspondances lexicales exactes. L'embedding capture le **sens** : la question *"Qui pilote le Millennium Falcon ?"* sera rapprochée de la description de la table `starship_pilots` même si les mots *"pilote"* ou *"Millennium"* n'y figurent pas littéralement — parce que le modèle comprend la relation sémantique entre ces concepts.

**Qu'est-ce qu'un vector store ?**

Un vector store (base de données vectorielle) est une base de données spécialisée dans le stockage et la recherche de vecteurs. Là où une base relationnelle classique indexe des lignes par clés primaires et permet des recherches par valeur exacte ou par plage, un vector store indexe des vecteurs et permet des **recherches par similarité** : *"trouve-moi les 5 vecteurs les plus proches de celui-ci"*.

Dans ce projet, le vector store est **pgvector** — une extension de PostgreSQL qui ajoute :
- Un type de colonne `vector(4096)` pour stocker des vecteurs de 4096 dimensions
- Des opérateurs de distance (cosinus, euclidienne, produit scalaire)
- Des index spécialisés (IVFFlat, HNSW) pour accélérer les recherches de similarité

L'avantage de pgvector est qu'il s'intègre directement dans le PostgreSQL existant du projet — pas besoin d'un service externe dédié (comme Qdrant, Pinecone ou Weaviate). Les vecteurs sont stockés dans la table `embeddings` aux côtés des données Star Wars, dans la même base `nlsqldb`.

**Pourquoi le RAG ? Le problème qu'il résout**

Un LLM a une **fenêtre de contexte limitée** (le nombre maximum de tokens qu'il peut traiter en une seule requête). Envoyer l'intégralité d'un schéma de base de données volumineux à chaque question serait inefficace et dégraderait la qualité des réponses (le modèle se "noie" dans l'information non pertinente).

Le RAG résout ce problème en deux temps :
1. **Indexation préalable** : on découpe le schéma en petits segments, on les transforme en vecteurs, et on les stocke dans le vector store.
2. **Recherche à la volée** : à chaque question, on transforme la question en vecteur, on cherche les segments les plus proches, et on ne fournit que ceux-là au LLM.

Ainsi, pour la question *"Quels films a réalisé George Lucas ?"*, le LLM reçoit uniquement la description des tables `films` et `film_characters` — pas celle des `starships` ou des `planets`.

#### Principe de fonctionnement

**Phase 1 — Ingestion des embeddings (`SchemaEmbeddingService`)**

L'ingestion est une opération préalable (déclenchée via `POST /api/import/embeddings`) qui prépare le vector store. Elle ne s'exécute qu'une fois (ou à chaque modification du schéma) :

```
starwars-description.txt
        │
        ▼
 Découpage par "###"
        │
        ▼
┌─────────────────────────────────────────────────┐
│ Segment 1: "### planets\n id, name, climate..." │
│ Segment 2: "### species\n id, name, ..."        │
│ Segment 3: "### people\n id, name, ..."         │
│ ...                                             │
│ Segment N: "Règles métier: unknown = pas de..." │
└─────────────────────────┬───────────────────────┘
                          │
                          ▼
              Modèle d'embedding
            (e5-mistral-7b-instruct)
                          │
                          ▼
┌─────────────────────────────────────────────────┐
│ Vecteur 1: [0.12, -0.87, 0.45, ..., 0.33]      │  ← 4096 nombres
│ Vecteur 2: [0.05, 0.91, -0.12, ..., -0.67]     │
│ ...                                             │
└─────────────────────────┬───────────────────────┘
                          │
                          ▼
              Table "embeddings" (pgvector)
              Chaque ligne = vecteur + texte original + métadonnées
```

Étapes détaillées :

1. Charge la description complète du schéma depuis `starwars-description.txt`.
2. Découpe le texte en **segments par table** en se basant sur les délimiteurs `###`.
3. Chaque segment reçoit des métadonnées (`type: table_schema`, `table: <nom_table>`).
4. Les règles métier sont ajoutées comme un segment supplémentaire (`type: business_rules`).
5. Tous les segments sont envoyés au modèle d'embedding qui retourne un vecteur de 4096 dimensions par segment.
6. Les couples (vecteur, texte original) sont stockés dans la table `embeddings` de PostgreSQL via pgvector.

**Phase 2 — Recherche de contexte (`ContextRetriever`)**

À chaque requête utilisateur, le ContextRetriever recherche les segments de schéma les plus pertinents :

```
"Quels personnages viennent de Tatooine ?"
                    │
                    ▼
          Modèle d'embedding
        (même modèle que l'ingestion)
                    │
                    ▼
       Vecteur question: [0.08, -0.92, ...]
                    │
                    ▼
        Recherche de similarité cosinus
        dans pgvector (table "embeddings")
                    │
                    ▼
     ┌──────────────────────────────────┐
     │ Score 0.89 → Segment "people"   │ ✅ retenu (> 0.5)
     │ Score 0.82 → Segment "planets"  │ ✅ retenu (> 0.5)
     │ Score 0.41 → Segment "films"    │ ❌ écarté (< 0.5)
     │ Score 0.38 → Segment "starships"│ ❌ écarté (< 0.5)
     └──────────────────────────────────┘
                    │
                    ▼
     Contexte = description "people" + description "planets"
     → envoyé au LLM dans le prompt
```

Paramètres de recherche :
- `maxResults = 5` — retourne au maximum 5 segments
- `minScore = 0.5` — seuil minimum de pertinence (similarité cosinus)

**Fallback** : Si aucun segment ne dépasse le seuil de 0.5, l'intégralité de la description du schéma est utilisée comme contexte. Cela garantit que le LLM dispose toujours d'informations sur le schéma, même pour des questions inhabituelles.

**Fournisseur de schéma (`SchemaProvider`)** :

- Charge `starwars-description.txt` au démarrage de l'application.
- Expose la description complète du schéma (utilisée en fallback par le `ContextRetriever`).
- Fournit 6 **règles métier** codées en dur :
  - `'unknown'` et `'n/a'` signifient absence de données
  - `birth_year` est au format BBY/ABY
  - Utiliser `film_characters` pour les jointures personnes-films
  - Utiliser `starship_pilots` pour les jointures vaisseaux-pilotes
  - Utiliser `film_planets` pour les jointures planètes-films
  - Utiliser `film_starships` pour les jointures vaisseaux-films

---

### LLM Client & Prompt Builder

**Classes** : `LlmClient`, `PromptBuilder`

#### Fonctionnel

Ces composants gèrent toute l'interaction avec le modèle de langage. Le `PromptBuilder` structure les prompts de manière versionnée et reproductible. Le `LlmClient` envoie les requêtes au LLM et nettoie les réponses.

L'application effectue **deux appels LLM par requête** :
1. **Génération SQL** — produit la requête SQL à partir de la question et du contexte
2. **Formulation de la réponse** — résume les résultats SQL en français

#### Principe de fonctionnement

**PromptBuilder** :

- Charge le **prompt système** depuis `prompts/system-v1.txt` au démarrage. Ce prompt contient :
  - Les règles strictes de génération (SELECT uniquement, pas de `SELECT *`, pas de DDL/DML)
  - Les conventions de formatage (aliases, `ORDER BY`, `LIMIT`)
  - 4 exemples few-shot NL→SQL pour guider le modèle
  - L'instruction de répondre uniquement en SQL brut (sans markdown, sans explication)

- Construit le **prompt utilisateur** dynamiquement avec les sections suivantes :
  ```
  ## Schéma de la base de données
  [segments de schéma pertinents issus du RAG]

  ## Règles métier
  - [règle 1]
  - [règle 2]
  ...

  ## Question
  [question en langage naturel]
  ```

**LlmClient** :

- Utilise l'API **LangChain4j** avec le backend **Ollama** et le modèle **Mistral 7B** (`temperature=0.0` pour des résultats déterministes).

- **`generateSql()`** :
  1. Construit les messages système + utilisateur via `PromptBuilder`
  2. Appelle `ChatModel.chat()` (Ollama/Mistral)
  3. Extrait le SQL de la réponse via `extractSql()` :
     - Supprime les éventuels blocs markdown (` ```sql ... ``` `)
     - Supprime le point-virgule final
     - Retourne le SQL nettoyé

- **`generateAnswer()`** :
  1. Construit un prompt inline demandant une réponse concise en français
  2. Tronque les résultats à 20 lignes maximum pour le contexte
  3. Appelle une seconde fois `ChatModel.chat()`
  4. Retourne la réponse en langage naturel

---

### SQL Validator

**Classe** : `com.yourorg.nlsqlengine.sql.SqlValidator`

#### Fonctionnel

Le validateur SQL est le **gardien de sécurité** du pipeline. Il garantit que le SQL produit par le LLM est :
- Syntaxiquement correct
- Strictement en lecture seule (SELECT uniquement)
- Conforme aux règles de sécurité (pas de `SELECT *`, pas de tables non autorisées)

Aucune requête ne peut atteindre la base de données sans avoir été validée par ce composant.

#### Principe de fonctionnement

La validation s'effectue en **5 étapes séquentielles** — le SQL est rejeté dès qu'une étape échoue :

| Étape | Vérification | Rejet si... |
|-------|-------------|-------------|
| 1 | Nullité / vide | La chaîne SQL est nulle ou blanche |
| 2 | Mots-clés interdits | Le premier mot est l'un de : `DROP`, `DELETE`, `UPDATE`, `INSERT`, `ALTER`, `TRUNCATE`, `CREATE`, `GRANT`, `REVOKE`, `MERGE`, `CALL`, `EXECUTE` |
| 3 | Parsing JSQLParser | Le SQL ne peut pas être parsé (erreur de syntaxe) |
| 4 | Type de requête | Le statement n'est pas un `SELECT` |
| 5 | `SELECT *` | La requête contient `AllColumns` (y compris dans les `UNION`) |
| 6 | Tables autorisées | Si une allowlist est configurée, vérifie que toutes les tables (FROM et JOIN) en font partie |

**Record associé** : `SqlValidationResult(boolean valid, String sql, String error)` avec factory methods `ok()` et `rejected()`.

Le parsing est assuré par **JSQLParser** (v5.3), un parseur SQL Java complet capable d'analyser la structure syntaxique des requêtes.

---

### SQL Executor

**Classe** : `com.yourorg.nlsqlengine.sql.SqlExecutor`

#### Fonctionnel

L'exécuteur SQL est responsable de l'exécution sécurisée des requêtes validées contre la base de données PostgreSQL. Il applique plusieurs niveaux de protection pour garantir qu'aucune modification de données ne puisse survenir.

#### Principe de fonctionnement

1. **Connexion sécurisée** :
   - `setReadOnly(true)` — la connexion JDBC est configurée en lecture seule
   - `setAutoCommit(false)` — désactive l'auto-commit

2. **Limites d'exécution** :
   - `setQueryTimeout(30s)` — timeout configurable (`nlsql.executor.timeout-seconds`)
   - `setMaxRows(100)` — limite le nombre de lignes retournées (`nlsql.executor.max-rows`)

3. **Exécution et mapping** :
   - Exécute la requête via `PreparedStatement`
   - Mappe chaque ligne du `ResultSet` en `LinkedHashMap<String, Object>` (préserve l'ordre des colonnes)
   - Retourne une `List<Map<String, Object>>`

4. **Nettoyage** :
   - `conn.rollback()` dans le bloc `finally` — annule toute transaction éventuelle par sécurité
   - La connexion est retournée au pool (gestion Agroal)

---

### Import de données (SWAPI)

**Classe** : `com.yourorg.nlsqlengine.rag.SwapiImporter`

#### Fonctionnel

Le service d'import alimente la base de données avec les données de l'univers Star Wars en consommant l'API publique **SWAPI** (Star Wars API). Il crée le schéma et importe toutes les entités et leurs relations.

#### Principe de fonctionnement

1. **Création du schéma** : Exécute `starwars-schema.sql` pour créer les 9 tables (avec `IF NOT EXISTS`).

2. **Import séquentiel des entités** (dans l'ordre imposé par les dépendances FK) :
   - `planets` → retourne un mapping URL→ID
   - `species` → utilise le mapping des planètes pour `homeworld_id`
   - `people` → utilise les mappings planètes et espèces
   - `films` → retourne un mapping URL→ID
   - `starships` → retourne un mapping URL→ID

3. **Import des tables de jointure** :
   - `film_characters` (films ↔ personnes)
   - `film_planets` (films ↔ planètes)
   - `film_starships` (films ↔ vaisseaux)
   - `starship_pilots` (vaisseaux ↔ personnes)

4. **Idempotence** : Tous les `INSERT` utilisent `ON CONFLICT DO NOTHING`, ce qui rend l'import réexécutable sans erreur ni doublon.

5. **Transaction unique** : L'ensemble de l'import s'exécute dans une seule transaction avec `commit()` final.

6. **Pagination SWAPI** : La méthode `fetchAll()` suit le champ `next` des réponses SWAPI pour collecter toutes les pages de résultats.

**Volumes attendus** : 60 planètes, 37 espèces, 82 personnages, 6 films, 36 vaisseaux.

---

## Modèle de données

### Tables principales

```
┌──────────────┐       ┌──────────────┐       ┌──────────────┐
│   planets    │       │   species    │       │    people    │
├──────────────┤       ├──────────────┤       ├──────────────┤
│ id (PK)      │◄──┐   │ id (PK)      │◄──┐   │ id (PK)      │
│ name         │   │   │ name         │   │   │ name         │
│ climate      │   │   │ classification│   │   │ height       │
│ terrain      │   │   │ designation  │   │   │ mass         │
│ population   │   │   │ avg_height   │   │   │ hair_color   │
│ ...          │   └───│ homeworld_id │   │   │ birth_year   │
└──────────────┘       │ ...          │   │   │ gender       │
                       └──────────────┘   └───│ species_id   │
                                              │ homeworld_id │──►planets
┌──────────────┐       ┌──────────────┐       └──────────────┘
│    films     │       │  starships   │
├──────────────┤       ├──────────────┤
│ id (PK)      │       │ id (PK)      │
│ title        │       │ name         │
│ episode_id   │       │ model        │
│ director     │       │ manufacturer │
│ producer     │       │ starship_class│
│ release_date │       │ ...          │
└──────────────┘       └──────────────┘
```

### Tables de jointure

```
film_characters (film_id, person_id)      — Films ↔ Personnages
film_planets    (film_id, planet_id)      — Films ↔ Planètes
film_starships  (film_id, starship_id)    — Films ↔ Vaisseaux
starship_pilots (starship_id, person_id)  — Vaisseaux ↔ Pilotes
```

Toutes les clés primaires composites, toutes les clés étrangères avec contraintes FK.

---

## Infrastructure & Configuration

### Services Docker (`docker-compose.yml`)

| Service | Image | Port | Rôle |
|---------|-------|------|------|
| `postgres` | `pgvector/pgvector:pg16` | 5432 | Base de données PostgreSQL avec extension pgvector |
| `ollama` | `ollama/ollama:latest` | 11434 | Serveur LLM local (Mistral 7B + e5-mistral-7b-instruct) |
| `nl-sql-engine` | Maven 3.9 + JDK 17 | 8080 | Application Quarkus en mode dev |
| `nl-sql-front` | Node 22 Alpine | 3000 | Frontend (repo séparé `../nl-sql-front`) |

### Configuration principale (`application.properties`)

| Propriété | Valeur | Description |
|-----------|--------|-------------|
| `quarkus.datasource.jdbc.url` | `jdbc:postgresql://localhost:5432/nlsqldb` | URL de connexion PostgreSQL |
| `quarkus.langchain4j.ollama.chat-model.model-id` | `mistral` | Modèle LLM pour la génération |
| `quarkus.langchain4j.ollama.chat-model.temperature` | `0.0` | Température à zéro pour des résultats déterministes |
| `quarkus.langchain4j.ollama.embedding-model.model-id` | `hellord/e5-mistral-7b-instruct:Q4_0` | Modèle d'embedding (4096 dimensions) |
| `quarkus.langchain4j.pgvector.dimension` | `4096` | Dimension des vecteurs stockés |
| `nlsql.executor.max-rows` | `100` | Nombre maximum de lignes retournées |
| `nlsql.executor.timeout-seconds` | `30` | Timeout d'exécution SQL |

### Stack technologique

| Composant | Technologie | Version |
|-----------|-------------|---------|
| Framework | Quarkus | 3.31.3 |
| Langage | Java | 17+ |
| LLM | Ollama + Mistral 7B | — |
| Embeddings | e5-mistral-7b-instruct (Q4_0) | 4096 dims |
| Intégration LLM | LangChain4j (Quarkus) | 1.7.1 |
| Base de données | PostgreSQL + pgvector | 16 |
| Parseur SQL | JSQLParser | 5.3 |
| Pool connexions | Agroal | (via Quarkus) |
| Build | Maven | 3.9 |
