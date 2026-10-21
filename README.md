# Sahastra Digital Backend

Sahastra Digital is a Spring Boot 3 backend for an e-commerce platform with PostgreSQL, Redis, and OpenSearch. The service exposes authenticated customer APIs, admin operations, search, cart, checkout, and reporting workflows.

## Stack

- Java 21
- Spring Boot 3.3.x
- Spring Web / Security / Data JPA / Validation / Cache
- PostgreSQL 16
- Redis 7
- OpenSearch 2.11
- Docker / Docker Compose
- GitHub Actions
- Terraform

## Quick start

1. Copy the environment template:
   ```bash
   cp .env.example .env
   ```
2. Start local infrastructure:
   ```bash
   docker compose up -d postgres redis opensearch
   ```
3. Run the app:
   ```bash
   ./mvnw spring-boot:run
   ```
   or
   ```bash
   mvn spring-boot:run
   ```
4. Verify health endpoints:
   ```bash
   curl http://localhost:8080/actuator/health
   curl http://localhost:8080/api/v1/health/ping
   ```

## Environment variables

Required runtime settings include:

- DB_PASSWORD
- REDIS_PASSWORD
- OPENSEARCH_HOST
- OPENSEARCH_PORT
- OPENSEARCH_USERNAME
- OPENSEARCH_PASSWORD
- ENVIRONMENT
- APP_PORT

Production secrets should be stored in a vault or cloud secret manager, not in source control.

## OpenSearch product index

Create the `products` index before using search. For a local OpenSearch installation
with HTTPS and a self-signed certificate, run this in PowerShell, using the same
credentials and endpoint configured for the backend:

```powershell
$credentials = '{0}:{1}' -f $env:OPENSEARCH_USERNAME, $env:OPENSEARCH_PASSWORD
curl.exe --insecure --user $credentials --request PUT "https://localhost:9200/products"
```

`--insecure` is for local self-signed certificates only. In production, trust the
OpenSearch CA certificate instead of disabling certificate verification.

The local Docker Compose OpenSearch service has security disabled and uses HTTP;
create the index with:

```powershell
curl.exe --request PUT "http://localhost:9200/products"
```

An acknowledged response means the index was created. To verify it, request
`/_cat/indices?v` from the same OpenSearch endpoint. Creating the index does not
populate it with products already in PostgreSQL. New or updated products are
indexed after their database transaction commits; existing products need to be
re-saved or explicitly reindexed before they appear in search.

## Core endpoints

- Auth: `/api/v1/auth/**`
- Products: `/api/v1/products/**`
- Search: `/api/v1/search/**`
- Cart: `/api/v1/cart/**`
- Orders: `/api/v1/orders/**`
- Admin: `/api/v1/admin/**`
- Health: `/api/v1/health/**` and `/actuator/health`

## Database and migrations

The project uses Flyway for schema evolution. Migrations live under:

- src/main/resources/db/migration

When the database is available, Flyway runs the pending migration scripts automatically on startup.

## Observability

The application writes JSON logs to the file appender and emits structured logs for the console in local development. Actuator exposes health and metrics endpoints.

## Deployment

- Local: Docker Compose and local JVM
- Staging: CI-built container deployed via infrastructure automation
- Production: GitHub Actions + Docker + Terraform/AWS deployment

## Documentation

- [docs/api-spec.md](docs/api-spec.md)
- [docs/backend-spec.md](docs/backend-spec.md)
- [docs/data-model.md](docs/data-model.md)
- [docs/security.md](docs/security.md)
- [docs/testing.md](docs/testing.md)
- [docs/deployment-guide.md](docs/deployment-guide.md)
- [docs/architecture-diagram.md](docs/architecture-diagram.md)
- [docs/security-architecture.md](docs/security-architecture.md)
- [docs/operations-manual.md](docs/operations-manual.md)
- [docs/troubleshooting-guide.md](docs/troubleshooting-guide.md)
- [docs/openapi.yaml](docs/openapi.yaml)

## Security notes

- Server-side price, stock, ownership, and discount checks are mandatory.
- JWT refresh tokens are rotated and replayed token families are invalidated.
- Admin endpoints require `ROLE_ADMIN`.
- Secrets never belong in the repo.
