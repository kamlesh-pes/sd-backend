# Sahastra Digital Backend — AI Agent Instructions

Read `docs/backend-spec.md`, `docs/api-spec.md`, `docs/data-model.md`, `docs/security.md`, and `docs/testing.md` before relevant work.

## Stack
Java 21, Spring Boot 3.x, Spring Web/Security/Data JPA/Validation/Cache, PostgreSQL 16, OpenSearch/Elasticsearch, Redis, Docker, GitHub Actions, Terraform or AWS CDK.

## Architecture
PostgreSQL is the transactional source of truth. OpenSearch is a search index. Redis supports rate limiting, refresh/session state, and caching. Product changes reach search asynchronously through outbox/indexer or CDC.

## Non-negotiable
- Never trust client price, discount, totals, stock, ownership, payment success, cancellation eligibility, or security timestamps.
- Money: PostgreSQL `DECIMAL(10,2)` and Java `BigDecimal`; never float/double.
- Explicit rounding.
- Transactions + optimistic locking for inventory/order state.
- Authenticated, idempotent order creation.
- Server-side ownership and RBAC.
- `ROLE_ADMIN` for admin APIs.
- Order status only through backend/admin processes.
- Cancellation eligibility calculated from server `Order.created_at`.
- Support request does not auto-cancel.
- Required admin/security actions audit logged.
- Secrets never committed.

## Authentication
BCrypt >=12 or Argon2id; JWT access + rotating refresh tokens; secure httpOnly cookies preferred; hashed refresh tokens; stale-token reuse detection; session revocation; device binding; step-up authentication.

OTP: E.164 normalization, hashed/HMACed code, 5-minute default expiry, 5 attempts, rate-limited.

## API
Version `/api/v1`, validate DTOs, reject unknown fields, enforce pagination limits, consistent error envelope, correlation IDs, no production stack traces.

## Observability
Structured JSON logs, correlation IDs, `/actuator/health`, Micrometer metrics.

## Done
Implementation + unit/integration tests + authorization/ownership + validation + concurrency/idempotency + security review + documentation updates.
