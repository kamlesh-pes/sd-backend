# Phase 2: Authentication and Authorization

## Completed

- User registration with Argon2id password hashing
- Email and phone normalization and OTP verification
- JWT access tokens with configurable expiry
- Hashed refresh tokens with rotation and replay-family revocation
- Login, refresh, logout, profile, OTP, and password-reset endpoints
- Stateless Spring Security configuration with bearer-token authentication
- `ROLE_CUSTOMER` and `ROLE_ADMIN` authorities
- Protected routes and admin namespace authorization
- Flyway migration for users, roles, refresh tokens, and OTPs
- Unit coverage for OTP verification and attempt handling

## Endpoints

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `GET /api/v1/auth/me`
- `POST /api/v1/auth/verify-otp`
- `POST /api/v1/auth/request-password-reset`
- `POST /api/v1/auth/reset-password`

## Configuration

Set `security.jwt.secret` to a random secret of at least 32 UTF-8 bytes in non-development environments. Defaults for local development are access tokens of 15 minutes, refresh tokens of 7 days, OTP expiry of 5 minutes, and five OTP attempts.

## Deferred Integration Work

OTP delivery, Redis-backed rate limiting, device fingerprint binding, step-up authentication, encrypted PII, and secure production cookie transport require the notification, platform-security, and deployment phases. The server remains authoritative for credentials, token state, roles, ownership, and password-reset invalidation.

## Verification

Run with Java 21:

```bash
JAVA_HOME=/path/to/java-21 mvn test
```

The new Phase 2 tests pass. Existing `CorrelationIdFilterTest` currently has stale expectations that inspect a ThreadLocal after the filter intentionally clears it and invokes the same `MockFilterChain` twice; those unrelated Phase 1 tests remain to be corrected separately.
