# Architecture Diagram

```mermaid
flowchart LR
    Client[Web / Mobile Client] --> API[Spring Boot API]
    API --> Auth[Auth + RBAC + JWT]
    API --> Products[Product Service]
    API --> Cart[Cart Service]
    API --> Orders[Order Service]
    API --> Search[Search Service]

    Products --> PG[(PostgreSQL)]
    Cart --> Redis[(Redis)]
    Orders --> PG
    Search --> OS[(OpenSearch)]
    Auth --> Redis

    Admin[Admin Portal] --> API
    Metrics[Actuator / Micrometer] --> API
    Logs[Structured JSON Logs] --> API
    Cloud[CloudWatch / Observability] --> Metrics
    Cloud --> Logs
```

## Component responsibilities

- API layer: request validation, authorization, API contract, correlation IDs
- Service layer: pricing, stock checks, order lifecycle, auditing
- PostgreSQL: transactional source of truth for product, cart/order, and audit records
- Redis: session state, rate limiting, and request or catalog caching
- OpenSearch: search index for product queries and suggestions
- Observability: structured logs, metrics, health endpoints, and alerting dashboards

## Data flow

1. Client sends a request to the API.
2. Security filters validate JWT state, rate limiting, and role boundaries.
3. Service layer enforces business rules and uses PostgreSQL transactions when necessary.
4. Search-centric operations write to or read from OpenSearch asynchronously where applicable.
5. Metrics and logs are emitted for monitoring and alerting.
