# Banque CQRS · Event Sourcing

Activité pratique Spring Boot 3 / Axon Framework 4 : gestion de comptes bancaires avec séparation commandes/requêtes, événements persistés dans **Axon Server** et projections de lecture dans **PostgreSQL**.

## Démarrer

Prérequis : Java 21+, Maven, Docker Compose.

```bash
docker compose up -d
mvn spring-boot:run
```

- Interface bancaire : http://localhost:8080
- Console Axon Server et historique des événements : http://localhost:8024
- PostgreSQL : `localhost:5432`, base `bank_cqrs`, utilisateur/mot de passe `bank` (configuration locale de démonstration).

Les volumes Docker préservent les données entre deux redémarrages. Les valeurs `DB_URL`, `DB_USER`, `DB_PASSWORD`, `AXON_SERVER` et `PORT` peuvent remplacer les paramètres locaux.

## Architecture

```text
POST/PATCH → CommandGateway → AccountAggregate (@CommandHandler)
                                 ├── règles métier (solde, devise, statut)
                                 └── événements → Axon Server (source de vérité)
                                                    ↓
GET ← QueryGateway ← AccountQueryHandler ← PostgreSQL ← AccountProjection (@EventHandler)
                                                    └── mises à jour → subscription query → SSE
```

Un compte créé est aussitôt activé. Les montants utilisent `BigDecimal` ; un débit dépassant le solde, une opération sur un compte suspendu ou une devise différente sont refusés avant publication de l’événement. Les événements reconstruisent l’état de l’agrégat ; la projection suit le journal via un processeur Axon et stocke comptes et opérations dans PostgreSQL. Les réponses aux commandes sont `202 Accepted` : la projection de lecture se met à jour **de manière asynchrone**, donc une lecture immédiate peut encore refléter l’état précédent.

## API

| Méthode | URL | Corps / résultat |
| --- | --- | --- |
| `POST` | `/api/accounts` | `{"initialBalance":1000,"currency":"MAD"}` → identifiant |
| `POST` | `/api/accounts/{id}/credits` | `{"amount":250,"currency":"MAD"}` |
| `POST` | `/api/accounts/{id}/debits` | `{"amount":50,"currency":"MAD"}` |
| `PATCH` | `/api/accounts/{id}/status` | `{"status":"SUSPENDED"}` ou `{"status":"ACTIVE"}` |
| `GET` | `/api/accounts` | `{"accounts":[...]}` |
| `GET` | `/api/accounts/{id}` | Solde, devise, statut, date de création |
| `GET` | `/api/accounts/{id}/operations` | `{"operations":[...]}` |
| `GET` | `/api/accounts/{id}/events` | Journal d’événements de l’agrégat, avec séquence, date et contenu |
| `GET` | `/api/accounts/{id}/state` | Solde, devise, statut et séquence d’événement courants |
| `GET` | `/api/accounts/{id}/state?sequence=N` | État reconstruit en rejouant les événements jusqu’à la séquence `N` |
| `GET` | `/api/accounts/{id}/stream` | Flux SSE (`event: operation`) : historique puis nouveaux mouvements |

Exemple de création :

```bash
curl -X POST http://localhost:8080/api/accounts \
  -H 'Content-Type: application/json' \
  -d '{"initialBalance":1000,"currency":"MAD"}'
```

## Tests

```bash
mvn test
```

Ce lancement vérifie l’agrégat et l’API avec une base H2 et un bus Axon local, sans Docker. Pour vérifier **aussi** PostgreSQL, Axon Server, la projection, les requêtes distantes et le flux SSE :

```bash
docker compose up -d
RUN_EXTERNAL_TESTS=true mvn test
```

Le test externe est ignoré sauf si `RUN_EXTERNAL_TESTS=true` est défini. Le client fourni dans `src/main/resources/static/` est servi directement par Spring Boot.
