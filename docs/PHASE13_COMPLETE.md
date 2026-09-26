# Phase 13: Comprehensive Testing

## Completed

- Added controller validation tests for pagination bounds and maximum page size
- Added strict JSON contract coverage for rejecting unknown request fields
- Added Spring MVC security tests for public access, unauthenticated `401`, authenticated non-admin `403`, and hardening headers
- Added cancellation regression coverage for late cancellation support escalation and unchanged order state
- Added Redis rate-limit tests for allowed, rejected, retry metadata, and Redis-unavailable behavior
- Added PostgreSQL/Testcontainers Flyway migration integration coverage
- Added missing Testcontainers JUnit 5 dependency
- Repaired payment test provider injection and correlation-ID test lifecycle assertions
- Fixed application YAML parsing for the Redis cache key prefix
- Configured the security chain to return `401 Unauthorized` for unauthenticated API requests

## Coverage Areas

- Business rules: cart, discounts, orders, payment, reports, settings, OTP
- Validation: pagination, unknown fields, discount rules, setting bounds, date ranges
- Authorization: public endpoints, admin RBAC, customer-vs-admin access
- Security: 401/403 behavior, security headers, Redis fail-open behavior
- Persistence: Flyway migration integration scaffold using PostgreSQL 16
- Lifecycle: cancellation, support escalation, correlation-context cleanup

## Tests Added or Expanded

- [src/test/java/com/sahastra/backend/security/SecurityAuthorizationTest.java](src/test/java/com/sahastra/backend/security/SecurityAuthorizationTest.java)
- [src/test/java/com/sahastra/backend/security/ratelimit/RateLimitServiceTest.java](src/test/java/com/sahastra/backend/security/ratelimit/RateLimitServiceTest.java)
- [src/test/java/com/sahastra/backend/integration/PostgresMigrationIntegrationTest.java](src/test/java/com/sahastra/backend/integration/PostgresMigrationIntegrationTest.java)
- [src/test/java/com/sahastra/backend/api/controller/ProductControllerTest.java](src/test/java/com/sahastra/backend/api/controller/ProductControllerTest.java)
- [src/test/java/com/sahastra/backend/api/dto/UnknownFieldContractTest.java](src/test/java/com/sahastra/backend/api/dto/UnknownFieldContractTest.java)
- [src/test/java/com/sahastra/backend/service/OrderServiceTest.java](src/test/java/com/sahastra/backend/service/OrderServiceTest.java)
- [src/test/java/com/sahastra/backend/service/PaymentServiceTest.java](src/test/java/com/sahastra/backend/service/PaymentServiceTest.java)
- [src/test/java/com/sahastra/backend/common/CorrelationIdFilterTest.java](src/test/java/com/sahastra/backend/common/CorrelationIdFilterTest.java)

## Verification

Full Maven test suite:

```powershell
Set-Location C:\Users\Administrator\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin
.\mvn.cmd -f C:\Users\Administrator\sd-repo\sd-backend\pom.xml test
```

Result: 47 tests passed, 0 failures, 0 errors, 1 skipped.

The skipped test is `PostgresMigrationIntegrationTest`, which requires Docker and was automatically skipped because no Docker environment was available.
