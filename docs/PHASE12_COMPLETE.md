# Phase 12: Rate Limiting & Security Hardening

## Completed

- Added an atomic Redis-backed fixed-window rate-limit service using `INCR` and `EXPIRE`
- Added per-client-address rate limiting middleware for API requests
- Added dedicated login, OTP/password-reset, and general API thresholds
- Connected rate-limit thresholds to the configurable Phase 11 system settings
- Added standard `X-Content-Type-Options`, `X-Frame-Options`, referrer-policy, and HSTS security headers
- Preserved stateless bearer-token security and disabled CSRF for the API-only application
- Added fail-open behavior when Redis is unavailable so a cache outage does not take down authentication or API traffic
- Added `429 Too Many Requests`, `Retry-After`, and rate-limit response headers
- Added a Flyway migration for OTP and general API rate-limit settings
- Added unit tests for allowed requests, exceeded limits, Redis failure behavior, and retry metadata

## Thresholds

- Login: 5 requests per minute per client address
- OTP and password reset: 5 requests per minute per client address
- General API: 120 requests per minute per client address

Thresholds are admin-configurable through the Phase 11 settings endpoints and validated against their configured bounds.

## Security Notes

- Rate-limit keys use `HttpServletRequest.getRemoteAddr()` and do not trust spoofable forwarding headers.
- Redis counter updates are atomic through a Lua script.
- HSTS is emitted by Spring Security for secure requests, with subdomains included and a one-year max age.
- SQL access remains parameterized through Spring Data repository queries.
- CSRF is disabled because the service uses stateless bearer authentication rather than browser cookies for API authentication.

## Files Added

- [src/main/java/com/sahastra/backend/security/ratelimit/RateLimitService.java](src/main/java/com/sahastra/backend/security/ratelimit/RateLimitService.java)
- [src/main/java/com/sahastra/backend/security/ratelimit/RateLimitDecision.java](src/main/java/com/sahastra/backend/security/ratelimit/RateLimitDecision.java)
- [src/main/java/com/sahastra/backend/security/filter/RateLimitFilter.java](src/main/java/com/sahastra/backend/security/filter/RateLimitFilter.java)
- [src/main/resources/db/migration/V9__rate_limit_settings.sql](src/main/resources/db/migration/V9__rate_limit_settings.sql)
- [src/test/java/com/sahastra/backend/security/ratelimit/RateLimitServiceTest.java](src/test/java/com/sahastra/backend/security/ratelimit/RateLimitServiceTest.java)

## Verification

`RateLimitServiceTest` passed with 3 tests, 0 failures, 0 errors, and 0 skipped.

The main source compilation also passed after wiring the filter, Redis template, settings, and security headers into the application.
