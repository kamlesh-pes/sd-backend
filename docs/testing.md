# Backend Testing

Use JUnit/Mockito for unit tests and Testcontainers for PostgreSQL/OpenSearch/Redis integration tests.

Cover:
- business rules and validation
- authorization and ownership
- pricing/discount calculations
- cart merge
- order transactions/idempotency
- concurrent last-item checkout
- cancellation at 23h/24h/25h and shipped state
- OTP expiry/attempts/rate limiting
- refresh rotation/reuse detection
- reports/CSV/audit
- security-setting bounds

Security tests must verify 401/403 behavior, tampering resistance, PII protection, stale-token family revocation, and absence of secrets.

Performance target: search p95 <300ms; load test search and checkout.
