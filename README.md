# NL-SQL-Engine

Transforme des questions en langage naturel en requêtes SQL valides, sécurisées et exécutables.

L'utilisateur pose une question en français, le système retrouve le contexte pertinent du schéma via RAG (pgvector), construit un prompt enrichi, génère le SQL via un LLM local (Mistral 7B / Ollama), le valide avec JSQLParser, puis l'exécute en lecture seule sur PostgreSQL.

## Architecture

```
Frontend (Vue.js :3000)
    ↓
API REST Quarkus (:8080)
    ↓
Orchestrateur NL → SQL
    ↓
RAG (pgvector) → Contexte schéma + règles métier
    ↓
Prompt Builder → LLM (Ollama / Mistral)
    ↓
SQL Validator (JSQLParser)
    ↓
SQL Executor (read-only)
    ↓
PostgreSQL
```

## Stack technique

| Composant       | Technologie                          |
|-----------------|--------------------------------------|
| Backend         | Quarkus 3.31, Java 17               |
| LLM             | Ollama + Mistral 7B                 |
| Embeddings      | e5-mistral-7b-instruct (Q4_0)       |
| Vector Store    | pgvector (PostgreSQL 16)             |
| SQL Parser      | JSQLParser                           |
| Frontend        | Vue.js                               |
| Orchestration   | Docker Compose                       |

## Prérequis

- Docker et Docker Compose
- NVIDIA GPU + [NVIDIA Container Toolkit](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/install-guide.html) (recommandé)

## Installation et démarrage

### 1. Cloner les dépôts

```bash
git clone <url>/nl-sql-engine.git
git clone <url>/nl-sql-front.git
```

Les deux dépôts doivent être côte à côte :

```
dev/
├── nl-sql-engine/    # Backend + docker-compose
└── nl-sql-front/     # Frontend Vue.js
```

### 2. Lancer l'infrastructure

```bash
cd nl-sql-engine
docker compose up -d
```

Cela démarre :
- **PostgreSQL + pgvector** sur le port `5432`
- **Ollama** sur le port `11434` (télécharge automatiquement les modèles Mistral et e5-mistral au premier lancement)
- **Backend Quarkus** sur le port `8080` (mode dev avec live reload)
- **Frontend Vue.js** sur le port `3000`

Le premier démarrage prend plusieurs minutes (téléchargement des modèles ~8 Go).

### 3. Importer les données Star Wars

```bash
# Importer le schéma et les données depuis SWAPI
curl -X POST http://localhost:8080/api/import

# Les embeddings sont ingérés automatiquement au démarrage
# Pour forcer une ré-ingestion manuelle :
curl -X POST http://localhost:8080/api/import/embeddings
```

### 4. Utiliser

Ouvrir http://localhost:3000 et poser une question, par exemple :
- "Quels sont les personnages originaires de Tatooine ?"
- "Dans quels films apparaît Luke Skywalker ?"
- "Quels vaisseaux sont pilotés par Han Solo ?"

Ou via l'API directement :

```bash
curl -X POST http://localhost:8080/api/query \
  -H "Content-Type: application/json" \
  -d '{"question": "Quels sont les personnages originaires de Tatooine ?"}'
```

## Sans GPU NVIDIA

Si la machine ne dispose pas de GPU NVIDIA, il faut supprimer la section `deploy.resources` du service `ollama` dans `docker-compose.yml` :

```yaml
  ollama:
    image: ollama/ollama:latest
    container_name: nl-sql-ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama
    # Supprimer le bloc ci-dessous si pas de GPU NVIDIA :
    # deploy:
    #   resources:
    #     reservations:
    #       devices:
    #         - driver: nvidia
    #           count: all
    #           capabilities: [gpu]
    entrypoint: ["/bin/sh", "-c", "ollama serve & until curl -s http://localhost:11434/api/tags > /dev/null 2>&1; do sleep 1; done && ollama pull mistral && ollama pull hellord/e5-mistral-7b-instruct:Q4_0 && wait"]
```

> **Note :** Sans GPU, l'inférence sera nettement plus lente (CPU uniquement). Prévoir 30 secondes ou plus par requête selon la machine.

## Développement local (sans Docker pour le backend)

```bash
# Démarrer uniquement PostgreSQL et Ollama
docker compose up -d postgres ollama

# Lancer Quarkus en mode dev
./mvnw quarkus:dev
```

## Tests

```bash
# Tests unitaires et d'intégration (hors tests réseau)
./mvnw test

# Tests d'intégration complets (nécessite accès réseau + Ollama)
./mvnw test -Dgroups="integration"
```
