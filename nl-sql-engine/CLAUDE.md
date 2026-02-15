# 🧠 NL‑SQL‑Engine

**Transformer des requêtes en langage naturel en SQL correct, sécurisé et exécutables.**

Ce projet vise à fournir une application robuste capable de prendre une question en langage naturel, de l’interpréter via une génération assistée par modèle de langage, de produire une requête SQL valide (dans un dialecte donné), puis de la valider et l’exécuter en toute sécurité.

---

## 🎯 Objectif

Le but principal de **NL‑SQL‑Engine** est de permettre à des utilisateurs (techniques ou métier) de poser des questions en langage naturel et d’obtenir une requête SQL correspondante, adaptée au schéma de la base de données cible, tout en respectant des règles de sécurité (pas de DDL/DML destructif, pas de SELECT `*`, etc.).

Fonctionnalités clés :
- Compréhension du langage naturel
- Connaissance du schéma DB + règles métier
- Génération SQL fiable et validée
- Architecture évolutive et testable
- Exécution SQL sécurisée en lecture seule

---

## 🧩 Setup developpement
OS principal  : Ubuntu 22.04 / 24.04
IDE           : VS Code
LLM           : Ollama (GPU) - Mistral 7B 
Backend       : Quarkus
RAG           : Qdrant / FAISS
SQL Parser    : JSQLParser
DB            : Docker

## Machine developpement
CPU : Intel i9-13900H (14 cœurs / 20 threads)
GPU : NVIDIA GeForce RTX 4060 – 8 Go VRAM
RAM : 64 Go DDR5
Stockage : 100 Go NVMe

## 🏗️ Architecture technique

L’architecture est pensée pour être claire, modulaire, testable et extensible :

Frontend (UI/REST)
↓
API REST Quarkus
↓
Orchestrateur NL→SQL
↓
+----------------------+
| RAG (Context) | → Embeddings + Vector Store
+----------------------+
↓
Prompt Builder
↓
LLM (Local ou API)
↓
SQL Validator (JSQLParser)
↓
SQL Executor (Read‑Only)
↓
Base de données


### 👇 Description des principales briques

#### 🔹 API REST (Quarkus)
- Expose des endpoints pour recevoir une question en NL
- Gère l’authentification / autorisation
- Retourne le SQL généré + résultats

#### 🔹 Orchestrateur
Coordonne l’ensemble du traitement :
- Récupère le contexte pertinent
- Construit le prompt avec le schéma et les règles métier
- Appelle le LLM pour générer du SQL

#### 🔹 RAG – (Retrieval Augmented Generation)
Un système de recherche de contexte permet de fournir uniquement les informations pertinentes au LLM :
- Schéma de base de données
- Documentation des tables / règles métier

Vector store
- Qdrant (Docker)

Embeddings
- HuggingFace (local, CPU/GPU)

#### 🔹 Prompt Builder
Gère les modèles de prompt versionnés :
- Règles strictes de génération (pas de DDL/DML dangereux, etc.)
- Injection du schéma & contexte
- Exemples (few‑shot) le cas échéant

#### 🔹 LLM
Moteur de génération de texte :
- Peut être une API externe (OpenAI, etc.)
- ou un modèle local exécuté via runtime (Ollama, etc.)

Disponible LLM local – Ollama
- ollama run mistral

#### 🔹 SQL Validator
Analyse le SQL produit pour s’assurer :
- pas de commandes interdites
- conformité au schéma autorisé
- respect des règles définies

#### 🔹 SQL Executor
Exécute la requête SQL en lecture seule contre la base de données cible :
- Limites de lignes
- Timeout
- Utiliseur DB en RO

---

## 📁 Arborescence du projet


nl-sql-engine/
├── src/main/java
│ ├── api # Endpoints REST
│ ├── orchestration # Services d’orchestration NL→SQL
│ ├── rag # Composants RAG / contexte
│ ├── llm # Clients LLM et prompt builder
│ ├── sql # Analyse / validation de SQL
│ └── security # Authentification & sécurité
├── src/main/resources
│ ├── prompts # Templates de prompts versionnés
│ ├── schema # Schémas DB et métadonnées
│ └── application.yml # Configuration principale
└── docker-compose.yml # Définition des services pour dev (DB, RAG, etc.)


---

## 🚀 Démarrage rapide

### Prérequis
- Java 17+
- Maven
- Docker & Docker Compose
- (Optionnel) LLM local via Ollama ou équivalent

### Lancer en mode développement

```bash
./mvnw quarkus:dev

Déployer les services annexes
docker-compose up -d

🧪 Tests

Les tests doivent couvrir :

la génération SQL attendue pour des questions NL

la validation de schéma

les cas ambigus ou interdits

les performances et limites

📌 Bonnes pratiques

Versionner les prompts

Documenter les règles métier

Utiliser des jeux d’exemples NL ↔ SQL

Ne jamais exécuter sans validation SQL préalable

📄 Licence

Ce projet est sous licence MIT (ou à définir selon les besoins).



