# Security Architecture

## Authentication and authorization

- JWT access tokens are issued after successful authentication.
- Refresh tokens rotate on use and are stored in a hashed form.
- Authorization is enforced at the API layer and verified through server-side ownership checks.
- Admin endpoints require the `ROLE_ADMIN` authority.

## Transport and headers

- Production traffic should run over HTTPS.
- HSTS, X-Frame-Options, and X-Content-Type-Options are enforced.
- Secret values are externalized and must not be committed to the repository.

## Threat mitigation

- Redis-backed rate limiting reduces abuse and brute-force attempts.
- Validation rejects malformed or unknown data.
- PostgreSQL queries use parameterized mechanisms and server-side validation.
- Sensitive events are audit logged and correlation IDs are included in requests.
