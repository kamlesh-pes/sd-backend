# Backend Security Specification

## Authentication/session
BCrypt >=12 or Argon2id. JWT access + rotating refresh tokens. Secure httpOnly/Secure/SameSite=Strict cookies preferred. Refresh tokens hashed. Stale-token reuse revokes the token family. Device fingerprint binding, session revocation/logout everywhere, refresh-token revocation after password change/compromise, and step-up authentication for sensitive actions.

Defaults: access 15m; customer refresh 7d; remember-me 30d; admin refresh 8h; absolute cap 30d; idle timeout 14d.

## OTP
Normalize E.164. Store only hash/HMAC. Default expiry 5m and max 5 attempts. Rate limit by mobile/IP and throttle resend. Never expose plaintext OTP.

## Authorization
Roles `ROLE_CUSTOMER`, `ROLE_ADMIN`. Method-level security and object-level ownership checks. Protect admin namespace.

## API security
Never trust client price, total, stock, ownership, payment result, cancellation flags, or timestamps. Bean Validation, reject unknown fields, idempotency keys, transactions, optimistic locking, HTTPS/HSTS, CSP, X-Content-Type-Options, X-Frame-Options, Referrer-Policy, parameterized queries, safe errors.

## Rate limiting
Redis-backed. Strict authentication limits, per-user/per-endpoint general limits, conservative support-request limit. Return 429 + Retry-After.

## PII
Email, mobile, full name, and address are sensitive. Encrypt specified fields with AES-256-GCM. Store encryption keys in a secrets manager. Use HMAC blind indexes for login lookup. Mask logs. Minimize PII in list responses. Never store raw card data.
