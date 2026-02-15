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
