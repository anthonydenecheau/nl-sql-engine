# TODO - NL-SQL-Engine

## 1. Configuration & Infrastructure
- [x] Initialiser le projet Quarkus
- [x] Ajouter les dépendances (LangChain4j, JSQLParser, pgvector, REST Jackson)
- [x] Configurer `application.yml` (datasource, LLM, pgvector)
- [x] Configurer `docker-compose.yml` (PostgreSQL + pgvector)

## 2. API REST
- [x] Créer le endpoint POST `/api/query` (reçoit une question NL, retourne le SQL + résultats)
- [x] Définir les DTOs (QueryRequest, QueryResponse)
- [x] Gestion des erreurs REST (ExceptionMapper)

## 3. SQL Validator
- [x] Implémenter la validation SQL avec JSQLParser
- [x] Bloquer DDL/DML destructif (DROP, DELETE, UPDATE, INSERT, ALTER, TRUNCATE)
- [x] Interdire SELECT *
- [x] Valider que les tables/colonnes appartiennent au schéma autorisé

## 4. SQL Executor
- [x] Implémenter l'exécution read-only des requêtes validées
- [x] Ajouter limite de lignes retournées
- [x] Ajouter timeout sur l'exécution
- [x] Connexion DB en utilisateur lecture seule

## 5. Prompt Builder
- [x] Créer les templates de prompts versionnés (sous `src/main/resources/prompts/`)
- [x] Injection du schéma DB dans le prompt
- [x] Injection des règles métier
- [x] Support few-shot (exemples NL <-> SQL)

## 6. Client LLM
- [x] Configurer le client LangChain4j (OpenAI ou Ollama)
- [x] Intégrer l'appel LLM avec le prompt construit
- [x] Parser la réponse LLM pour extraire le SQL

## 7. RAG (Retrieval Augmented Generation)
- [x] Définir le modèle de documents (schéma DB, règles métier)
- [x] Implémenter l'import des données Star Wars depuis SWAPI
- [x] Implémenter l'ingestion des métadonnées dans pgvector (embeddings)
- [x] Implémenter la recherche de contexte pertinent par similarité
- [x] Ré-ingestion automatique des embeddings au démarrage (hash SHA-256 + purge si changement)

## 8. Orchestrateur
- [x] Implémenter le service d'orchestration NL -> SQL
- [x] Chaîner : RAG -> Prompt Builder -> LLM -> Validator -> Executor
- [x] Gestion des erreurs et retries
- [x] Self-correction avec feedback d'erreur (validation + exécution) renvoyé au LLM lors des retries

## 9. Sécurité
- [ ] Authentification (JWT / API key)
- [ ] Autorisation par rôle
- [ ] Rate limiting

## 10. Tests
- [x] Tests unitaires du SQL Validator (19 tests)
- [x] Tests unitaires du Prompt Builder (4 tests)
- [x] Tests d'intégration de l'orchestrateur (4 tests, @Tag integration)
- [x] Tests REST des endpoints (4 tests)
- [x] Jeux de données NL <-> SQL attendus (10 cas)
- [x] Tests du hash repository et du calcul de hash embeddings (5 tests)

## 11. Évolutions futures

### UX avancée — Domaines, prompts et visualisation

#### Domaines fonctionnels
Le frontend propose un sélecteur de domaine (ex: "Ventes", "RH", "Stock"). Le domaine sélectionné est envoyé au backend pour filtrer le contexte RAG, ce qui améliore drastiquement la pertinence sur un schéma large.

**Backend :**
- [x] Créer la table `domains(id, name, description)` dans PostgreSQL
- [x] Associer chaque vue/table à un domaine (table `domain_tables`)
- [x] `GET /api/domains` — liste les domaines disponibles
- [x] Ajouter le champ optionnel `domain` dans `QueryRequest`
- [x] Adapter `ContextRetriever` pour filtrer les embeddings par domaine (metadata filter pgvector)

**Frontend :**
- [x] Sélecteur de domaine fonctionnel (dropdown ou chips) en amont du champ de saisie
- [x] Filtrage automatique des prompts suggérés selon le domaine sélectionné

#### Bibliothèque de prompts
Les utilisateurs peuvent enregistrer des prompts validés et retrouver les plus populaires. Les prompts enregistrés servent aussi de few-shot examples pour le LLM.

**Backend :**
- [x] Créer la table `saved_prompts(id, domain_id FK, question, sql_generated, usage_count, created_at)`
- [x] `GET /api/prompts?domain={id}` — liste les prompts enregistrés pour un domaine
- [x] `GET /api/prompts/popular?domain={id}&limit=5` — prompts les plus utilisés (triés par `usage_count`)
- [x] `POST /api/prompts` — enregistrer un prompt (question + SQL validé)
- [x] Incrémenter `usage_count` à chaque réutilisation d'un prompt enregistré
- [x] Optionnel : utiliser les prompts enregistrés comme few-shot examples dynamiques dans le `PromptBuilder`

**Frontend :**
- [x] Afficher les prompts populaires du domaine sélectionné (cliquables pour pré-remplir le champ)
- [x] Liste searchable des prompts enregistrés
- [x] Bouton "Enregistrer ce prompt" après un résultat réussi
- [x] Champ libre toujours disponible pour saisir un prompt custom

#### Visualisation des résultats
Selon les données retournées, proposer différentes mises en forme.

**Frontend :**
- [x] Vue tabulaire (par défaut) — affichage des résultats sous forme de tableau paginé/triable
- [x] Vue graphique — détection automatique du type de visualisation pertinent :
  - Données avec agrégation + labels → bar chart / pie chart
  - Données avec dimension temporelle → line chart
  - Données avec 2 mesures numériques → scatter plot
- [x] Sélecteur manuel du type de graphique (override de la détection auto)
- [x] Export des résultats (CSV, JSON)
- Librairie suggérée : **Chart.js** ou **ECharts** (léger, bonne intégration Vue)

### Mode vocal
Permettre aux utilisateurs de poser leurs questions à l'oral et d'écouter la réponse.

#### Speech-to-Text (voix → texte)
~~- [x] **MVP navigateur** : intégrer la Web Speech API (`SpeechRecognition`) côté React pour capturer la voix et injecter le texte dans le champ de saisie (Chrome/Edge uniquement)~~
- [x] **Version locale** : déployer Faster-Whisper en conteneur Docker (modèle `medium` ~2 Go VRAM, français), exposer une API HTTP pour la transcription
- [x] Ajouter un bouton micro dans `QueryForm.jsx` avec indicateur visuel d'écoute

#### Text-to-Speech (texte → voix)
~~- [x] **MVP navigateur** : utiliser la `SpeechSynthesis` API pour lire la réponse en langage naturel~~
- [x] **Version locale** : déployer Piper TTS en conteneur Docker (voix française, CPU uniquement, ~15 Mo par voix)
- [x] Ajouter un bouton lecture audio sur la section "Réponse"

#### Budget VRAM estimé (approche tout-local)
| Composant | VRAM |
|-----------|------|
| Mistral 7B (Ollama) | ~5 Go |
| Faster-Whisper medium | ~2 Go |
| Piper TTS | CPU uniquement |
| **Total** | **~7 Go** (RTX 4060 8 Go OK) |

> **Easter egg Chewbacca** : Pour le fun, ajouter une option "Voix Wookiee" dans les paramètres TTS. Piste : utiliser **RVC** (Retrieval-based Voice Conversion) entraîné sur des samples de Chewbacca pour transformer la sortie Piper en rugissements compréhensibles... ou pas. Alternative rapide : appliquer un pitch shift (-8 demi-tons) + distorsion via la **Web Audio API** côté frontend. Rrraaaargh !

### LangChain4j AI Services avec Tool Use (function calling)
- [ ] Migrer l'orchestration vers `@RegisterAiService` + `@Tool` de LangChain4j
- Permettrait au LLM d'appeler des outils Java (introspection schéma, vérification de tables) avant de générer le SQL, au lieu de dépendre uniquement du prompt
- Le LLM pourrait vérifier lui-même l'existence d'une table via `information_schema` avant de l'utiliser
- **Prérequis modèle** : le function calling nécessite un modèle qui le supporte nativement. Mistral 7B est limité sur ce point. Modèles recommandés :
  - **Mistral Small / Large** (via API Mistral) — support natif function calling
  - **Llama 3.1 70B+** (via Ollama) — bon support tools, mais nécessite plus de VRAM (~40 Go)
  - **Qwen 2.5 7B** (via Ollama) — bon compromis taille/function calling pour du local
  - **GPT-4 / Claude** (via API externe) — excellent support, mais nécessite une connexion API
- Impact : pas de ressources matérielles supplémentaires (même infra), mais plus d'appels LLM par requête (latence x2-x4)

### Scaling 100+ tables — Vues métier PostgreSQL + Oracle FDW
Objectif : permettre à l'application d'interroger un schéma Oracle de 100+ tables sans perdre en qualité de génération SQL.

#### Principe
- Les tables sources restent dans **Oracle** (source de vérité)
- **PostgreSQL** expose les données Oracle via `oracle_fdw` (Foreign Data Wrapper)
- Des **vues métier** PostgreSQL pré-agrègent les jointures complexes par domaine fonctionnel
- Le **LLM ne voit que les vues** (15-20 vues simples au lieu de 100+ tables avec FK)
- Les requêtes générées sont de simples SELECT/WHERE/GROUP BY, pas de jointures multi-tables

#### Étapes
- [ ] Installer `oracle_fdw` + Oracle Instant Client dans le conteneur PostgreSQL
- [ ] Configurer le serveur distant Oracle (`CREATE SERVER`, `USER MAPPING`)
- [ ] Importer les foreign tables depuis Oracle (`IMPORT FOREIGN SCHEMA` ou `CREATE FOREIGN TABLE`)
- [ ] Créer les vues métier par domaine fonctionnel (ex: `v_sales_summary`, `v_employee_directory`)
- [ ] Adapter `SchemaProvider` pour décrire les vues (pas les tables Oracle)
- [ ] Adapter `SqlValidator` pour n'autoriser que les vues dans la whitelist
- [ ] Adapter les embeddings RAG sur les descriptions des vues

#### Performances — deux stratégies selon la fraîcheur des données
| Stratégie | Fraîcheur | Performance | Cas d'usage |
|-----------|-----------|-------------|-------------|
| `oracle_fdw` + vues PG | Temps réel | Dépend du réseau + Oracle | Données critiques, stock, commandes en cours |
| `MATERIALIZED VIEW` + refresh planifié (`pg_cron`) | Différé (5min, 1h...) | Excellente, tout local PG | Reporting, référentiels stables |

#### Points d'attention
- Les `JOIN` entre foreign tables s'exécutent côté PG → pour les jointures lourdes, préférer créer la vue côté Oracle et exposer une seule foreign table
- `oracle_fdw` pousse les `WHERE` vers Oracle (filter pushdown) mais pas tous les opérateurs
- Prévoir un utilisateur Oracle en **lecture seule** dédié au FDW

### Amélioration du RAG pour schémas larges
- [ ] Two-stage retrieval : 1) identifier les vues pertinentes, 2) récupérer leur schéma détaillé
- [ ] Schema compact dans le prompt : `vue(col1, col2, col3)` au lieu des descriptions longues
- [ ] Reranking avec cross-encoder après la recherche vectorielle
- [ ] Enrichir les embeddings avec des synonymes et questions typiques par vue
