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

## SMTP connectivity test

`ZohoMailConnectivityTest` sends a real test message and is skipped unless `MAIL_HOST`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, and `MAIL_TEST_TO` are set in the test process environment. Set `MAIL_TEST_TO` to an address you control, then run:

```powershell
$env:MAIL_TEST_TO = "your-address@example.com"
mvn "-Dtest=ZohoMailConnectivityTest" test
```

The test uses `MAIL_PORT` if set, otherwise port 587 with SMTP authentication and STARTTLS. Keep mailbox credentials out of source control.
