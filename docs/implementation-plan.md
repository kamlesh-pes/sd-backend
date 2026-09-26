# Sahastra Digital Backend — Implementation Plan

**Last Updated:** 2026-08-29  
**Status:** Guidance Document for All AI Agents and Developers

---

## Overview

This document consolidates all AI agent instructions (AGENTS.md, CLAUDE.md, copilot-instructions.md) and provides a single source of truth for implementation planning and execution on the Sahastra Digital Backend.

**Key Principle:** All authoritative business, security, pricing, stock, and authorization rules must be kept on the server. Never trust client data for these critical domains.

---

## Technology Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| Java | 21 | Runtime |
| Spring Boot | 3.x | Framework |
| Spring Web | Latest | HTTP APIs |
| Spring Security | Latest | Authentication/Authorization |
| Spring Data JPA | Latest | ORM & Persistence |
| Spring Validation | Latest | DTO validation |
| Spring Cache | Latest | Caching support |
| PostgreSQL | 16 | Transactional database |
| OpenSearch/Elasticsearch | Latest | Full-text search index |
| Redis | Latest | Rate limiting, sessions, caching |
| Docker | Latest | Containerization |
| GitHub Actions | Native | CI/CD pipelines |
| Terraform/AWS CDK | Latest | Infrastructure as Code |

---

## Architecture Principles

### Data Flow
1. **PostgreSQL** is the transactional source of truth for all business data
2. **OpenSearch** is a search index for product queries (populated asynchronously)
3. **Redis** provides:
   - Rate limiting enforcement
   - Refresh/session token state
   - Caching layer (configurable TTL)
4. **Product changes** flow to search via:
   - Transactional outbox pattern, or
   - Change Data Capture (CDC), or
   - Async indexer service

### Consistency Model
- Optimistic locking for inventory and order state to prevent overselling
- Transactions must protect multi-step operations (cart → checkout → order)
- Order status transitions only through backend/admin processes (never client)
- Cancellation eligibility calculated from server-stored `Order.created_at`

---

## Non-Negotiable Security & Business Rules

### Data Validation
- ❌ **NEVER** trust client-provided: price, discount, totals, stock, ownership, payment success, cancellation eligibility, security timestamps
- ✅ **ALWAYS** validate and recalculate server-side:
  - Cart subtotals and product prices
  - Discount application and effective price
  - Shipping and tax calculations
  - Order totals
  - Stock availability and decrement

### Monetary Handling
- Use PostgreSQL `DECIMAL(10,2)` columns for all money fields
- Use Java `BigDecimal` for all money calculations (never `float` or `double`)
- Explicit rounding using standard rounding mode (`HALF_UP`)
- Example: tax, discounts, totals must all be calculated server-side

### Order Lifecycle & Inventory
- Order creation is **authenticated** and **idempotent** (via idempotency key)
- Stock decrement is **transactional** and **locked** (optimistic locking)
- Order status transitions: `PENDING → CONFIRMED → PACKED → SHIPPED → OUT_FOR_DELIVERY → DELIVERED`
- Status can be `CANCELLED` according to business rules only
- Every status transition recorded in `OrderStatusHistory` table
- Customers **cannot** directly set order status

### Cancellation Rules
| Condition | Actor | Action | Notes |
|-----------|-------|--------|-------|
| Within 24h, before shipped | Customer | Self-cancel | Record reason, restore stock, refund |
| After 24h or shipped | Customer | File support request | Order unchanged; admin decides |
| Support request filed | Support | Review, not auto-cancel | Explicit admin decision required |
| Cancellation confirmed | Admin | Cancel order | Restore stock, issue refund, audit log |

### Authentication & Authorization
- **Password Hashing:** BCrypt (strength ≥12) or Argon2id
- **Token Strategy:**
  - Access token (short-lived, 15min default)
  - Rotating refresh token (medium-lived, 7d customer / 8h admin default)
  - Secure `httpOnly` cookies (preferred over header)
- **Refresh Token Security:**
  - Hashed tokens in database
  - Detect stale-token reuse (potential compromise)
  - Support session revocation
  - Optional device binding
  - Step-up authentication for sensitive ops
- **One-Time Password (OTP):**
  - E.164 normalization for phone numbers
  - Hashed/HMACed OTP codes
  - 5-minute default expiry (configurable)
  - 5 attempt limit per OTP (configurable)
  - Rate-limited issuance (prevent spam)
- **Authorization:**
  - Server-side ownership checks on all user resources
  - Role-Based Access Control (RBAC):
    - `ROLE_ADMIN` for sensitive admin APIs
    - Role-based endpoint restrictions
    - Explicit permission checks in business logic

### Audit & Logging
- ✅ **Required audit logging:**
  - All admin actions (user management, order updates, refunds, etc.)
  - All security-relevant events (login, password change, role grants)
  - All configuration changes (security settings, discounts, etc.)
  - Discount application and changes
  - Support request lifecycle
- ❌ **Never log:**
  - Passwords, secrets, API keys
  - Full credit card numbers or sensitive payment data
  - OTP codes or raw tokens
- ✅ **Audit table design:**
  - `action` (enum: CREATE, UPDATE, DELETE, CANCEL, etc.)
  - `actor_id` (who performed the action)
  - `actor_type` (ADMIN, SYSTEM, CUSTOMER)
  - `target_entity` (Order, User, Product, etc.)
  - `target_id` (entity primary key)
  - `timestamp` (server time)
  - `changes` (JSON of before/after, null-safe)

### Secrets Management
- ❌ **Never commit secrets** to repository (`.env`, API keys, DB passwords, JWT secret, etc.)
- ✅ Use environment variables or secure vaults (AWS Secrets Manager, HashiCorp Vault)
- ✅ Rotate secrets regularly
- ✅ Never log or expose secrets in error messages

---

## API Design

### Versioning & Structure
- All endpoints prefixed with `/api/v1`
- RESTful resource-based design
- Consistent request/response envelopes

### Request Validation
- Validate **all** DTOs:
  - Required fields, type coercion
  - Length, range, format (email, phone, etc.)
  - Business rule validation (e.g., start date < end date)
  - Reject unknown fields (strict mode, fail-fast)
- Return `400 Bad Request` with validation error details
- Provide field-level error messages for UX clarity

### Response Format
- Consistent error envelope:
  ```json
  {
    "success": false,
    "error": {
      "code": "INSUFFICIENT_STOCK",
      "message": "Product out of stock",
      "details": { "requested": 5, "available": 2 },
      "correlationId": "abc-123-def"
    }
  }
  ```
- Correlation ID on all responses (trace requests through logs)
- Pagination:
  - Enforce maximum page size (e.g., limit to 100 items)
  - Support `?page=1&size=20&sort=created_at,desc`
  - Return `totalElements`, `totalPages`, `currentPage` in response
- **No production stack traces** in error responses (log internally, return generic message to client)

### Pagination Limits
- Default page size: 20 items
- Maximum page size: 100 items
- Default sort: newest first
- Consistent across all list endpoints

---

## Observability & Monitoring

### Structured Logging
- JSON-formatted logs for easy parsing
- Include correlation ID in all logs
- Log levels: DEBUG, INFO, WARN, ERROR
- Sensitive data (passwords, tokens, PII) never logged
- Example:
  ```json
  {
    "timestamp": "2026-08-29T10:30:45Z",
    "correlationId": "abc-123-def",
    "level": "INFO",
    "service": "order-service",
    "event": "order.created",
    "orderId": "ORD-12345",
    "customerId": "CUST-789",
    "amount": "99.99"
  }
  ```

### Health & Metrics
- `/actuator/health` endpoint (Spring Boot default)
- Micrometer metrics for:
  - API endpoint latency (p50, p95, p99)
  - Database query times
  - Cache hit/miss rates
  - Search index sync lag
  - Error rates by type
- Custom business metrics:
  - Orders created (rate)
  - Cancellations (rate)
  - Search query latency
  - Payment provider latency

### Tracing
- Correlation ID generated at API entry, passed through all service calls
- Optional distributed tracing (Jaeger, Zipkin) for production
- Database query tracing (slow query logs, query counts)

---

## Implementation Phases

### Phase 1: Foundation (Core Infrastructure)
1. Spring Boot scaffold + Docker Compose setup
2. PostgreSQL migrations and entity definitions
3. Redis setup and connection pooling
4. Error handling framework and exception classes
5. Correlation ID middleware
6. Structured logging configuration

**Deliverables:**
- Containerized local dev environment
- Database schema version control
- Logging framework ready
- Error handling patterns established

---

### Phase 2: Authentication & Authorization (Complete)
1. User entity and repository
2. Password hashing (BCrypt/Argon2id)
3. JWT access token generation and validation
4. Refresh token strategy (rotating tokens + hashed storage)
5. OTP generation, delivery, and verification
6. Security configuration (CORS, CSRF if applicable)
7. RBAC setup (`ROLE_ADMIN`, `ROLE_CUSTOMER`, etc.)
8. Ownership checks for user resources
9. Session management (device binding, revocation)
10. Step-up authentication for sensitive operations

**Deliverables:**
- User registration endpoint
- Login endpoint with JWT + refresh token
- Token refresh endpoint (rotating token)
- OTP generation and verification
- Password reset flow
- Role-based access control tests
- Authorization integration tests

**Status:** Core Phase 2 implementation is complete. Registration, Argon2id password hashing, JWT access tokens, hashed rotating refresh tokens with replay-family revocation, email/phone OTP verification, password reset, protected user endpoints, admin route authorization, and the V2 authentication schema are implemented. External OTP delivery, Redis rate limiting, device binding, and step-up authentication remain integration work for the relevant later phases.

---

### Phase 3: Product & Catalog Management (Complete)
1. Product entity and admin CRUD
2. Product validation (price, stock, categorization)
3. Search entity (simplified for OpenSearch)
4. Admin-only endpoints for product management
5. Category and brand entities
6. Pricing and stock fields with optimistic locking
7. Product change audit logging

**Deliverables:**
- Product CRUD endpoints (admin-only)
- Product retrieval endpoint (public)
- Price and stock recalculation logic
- Audit trail for product changes
- Optimization: database indexes on frequently queried fields

**Status:** Product, category, and brand persistence; validated admin CRUD; public product retrieval; server-side effective pricing; optimistic-lock versioning; catalog indexes; and product audit logging are implemented. OpenSearch documents, full-text search, suggestions, and asynchronous indexing are deferred to Phase 4.

---

### Phase 4: Search & Indexing
1. OpenSearch cluster integration
2. Product document mapping (name, brand, category, description, SKU)
3. Full-text search implementation
4. Fuzzy matching and type-ahead
5. Filtering (by category, price range, etc.)
6. Sorting (relevance, price, newest)
7. Async product indexing (outbox pattern or CDC)
8. Pagination and result limits
9. Search latency optimization (target p95 <300ms)

**Deliverables:**
- Product search endpoint with filters and sorting
- Type-ahead endpoint for suggestions
- Async indexing pipeline (outbox + indexer service or CDC)
- Search latency monitoring
- Indexing lag alerts

---

### Phase 5: Cart Management
1. Cart entity (authenticated and guest sessions)
2. Cart item add/update/remove logic
3. Stock validation on cart operations
4. Server-side cart total recalculation
5. Guest-to-authenticated merge after login
6. Cart persistence and session management
7. Discount application validation

**Deliverables:**
- Get cart endpoint
- Add/update/remove items endpoints
- Guest cart session handling
- Cart merge on authentication
- Server-side price and total validation
- Cart expiration (cleanup old guest carts)

---

### Phase 6: Checkout & Order Creation
1. Checkout endpoint (authentication + idempotency key)
2. Order entity and repository
3. OrderItem entity (snapshot of product at purchase time)
4. Server-side total calculation (subtotal + discount + tax + shipping)
5. Stock decrement (transactional + optimistic locking)
6. Order status initialization (`PENDING`)
7. Idempotent order creation (detect duplicates via idempotency key)
8. OrderStatusHistory logging

**Deliverables:**
- Checkout endpoint with validation
- Order creation with atomic stock decrement
- Idempotency key tracking table
- Order retrieval endpoint (customer-owned only)
- Concurrency tests (overselling prevention)

---

### Phase 7: Payment Abstraction
1. PaymentProvider interface
2. Mock payment provider implementation
3. Real payment provider implementation (e.g., Stripe, PayPal)
4. Payment request/response handling
5. Webhook handling for payment confirmations
6. Order status update on payment success/failure
7. PCI compliance (no raw card data stored)

**Deliverables:**
- Payment provider abstraction
- Mock provider for testing
- Real provider integration
- Webhook security (signature verification)
- Payment idempotency
- Error handling (retry logic, fallback)

---

### Phase 8: Order Lifecycle & Cancellation
1. Order status transitions and validation
2. OrderStatusHistory record creation
3. Cancellation eligibility check (24h + before shipped)
4. Customer self-cancel endpoint
5. Support request entity and flow
6. Admin cancellation endpoint
7. Stock restoration on cancellation
8. Refund flow (link to payment provider)
9. Audit logging for all transitions

**Deliverables:**
- Order status update endpoints (admin-only)
- Customer self-cancel endpoint
- Support request creation and management (admin views)
- Stock restoration logic
- Refund processing
- OrderStatusHistory view endpoint

---

### Phase 9: Discount Management
1. Discount entity (percentage, flat amount, start/end dates)
2. Admin endpoints for discount CRUD
3. Validation: only one active discount per product
4. Effective price calculation (at read, cart, checkout time)
5. Discount application audit logging
6. Discount change tracking

**Deliverables:**
- Discount CRUD endpoints (admin-only)
- Effective price calculation logic
- Discount application in cart/checkout
- Discount change audit log
- Tests for discount edge cases (expired, conflicting)

---

### Phase 10: Reporting & Export
1. Order report (query, filter, sort)
2. Cancellation report (trends, reasons)
3. Summary metrics (revenue, units sold, avg order value)
4. CSV export functionality
5. Audit trail report (admin actions, security events)
6. Report access audit logging
7. Date range filtering
8. Admin-only access control

**Deliverables:**
- Report endpoints (admin-only)
- CSV export endpoints
- Audit trail querying
- Report access logging
- Performance optimization (indexed queries)

---

### Phase 11: Configurable Security Settings
1. SystemSetting entity (key-value store)
2. Default settings:
   - Access token expiry: 15 minutes
   - Customer refresh token: 7 days
   - Remember-me token: 30 days
   - Admin refresh token: 8 hours
   - Absolute cap: 30 days
   - Idle timeout: 14 days
   - OTP expiry: 5 minutes
   - OTP max attempts: 5
   - Login rate limit: 5/min per IP
3. Admin endpoints for setting updates
4. Min/max validation for all settings
5. Caching (reload on update)
6. Audit logging for setting changes
7. **No retroactive token changes** (existing tokens keep old TTLs)

**Deliverables:**
- SystemSetting entity and repository
- Settings update endpoint (admin-only)
- Settings caching + cache invalidation
- Validation framework (min/max bounds)
- Settings audit trail
- Tests for setting boundary conditions

---

### Phase 12: Rate Limiting & Security Hardening
1. Rate limiting configuration (Redis-backed)
2. Login rate limit (5 attempts per minute per IP)
3. OTP request rate limit
4. API endpoint rate limits (prevent brute force)
5. DDoS protection headers
6. SQL injection prevention (parameterized queries)
7. CSRF protection (if applicable)
8. Input sanitization
9. X-Content-Type-Options, X-Frame-Options headers
10. HTTPS enforcement (production)

**Deliverables:**
- Rate limit middleware
- Redis-backed rate limit store
- Per-endpoint rate limit configuration
- Security headers middleware
- Load testing with rate limits
- Documentation on rate limit thresholds

---

### Phase 13: Comprehensive Testing
1. **Unit Tests:**
   - Business logic (pricing, discounts, stock)
   - Validation (DTOs, OTP, passwords)
   - Entity lifecycle
   - Utility functions
2. **Integration Tests:**
   - Database migrations
   - Entity relationships
   - Transaction boundaries
   - Caching behavior
3. **Endpoint Tests (Spring MVC Test):**
   - Happy path scenarios
   - Error cases (validation, authorization, not found)
   - Status codes and response format
   - Pagination and sorting
   - Correlation ID propagation
4. **Concurrency Tests:**
   - Stock decrement under concurrent requests
   - Optimistic locking conflicts and retries
   - Order creation idempotency
5. **Security Tests:**
   - Unauthenticated access denied
   - Unauthorized role access denied
   - Ownership checks (customer A cannot see customer B's orders)
   - RBAC edge cases
6. **Authorization Tests:**
   - Admin-only endpoints reject non-admin
   - Customer endpoints reject wrong customer
7. **Data Validation Tests:**
   - Unknown field rejection
   - Type coercion errors
   - Business rule violations

**Deliverables:**
- Unit test coverage >80%
- Integration test suite (database tests)
- Endpoint test suite (REST layer)
- Concurrency test suite (race conditions)
- Security test checklist and evidence
- Test documentation and patterns

---

### Phase 14: Load Testing
1. Load test scenarios:
   - Product search (1000+ concurrent)
   - Cart operations (500+ concurrent)
   - Checkout (100+ concurrent, monitor stock decrement)
   - Order status updates (100+ concurrent)
2. Target metrics:
   - p95 response time <500ms for read operations
   - p95 <1s for write operations
   - Search latency <300ms (p95)
3. Identify bottlenecks (DB, cache, search)
4. Optimization recommendations

**Deliverables:**
- Load test scripts (JMeter/Gatling)
- Baseline performance metrics
- Bottleneck analysis
- Optimization recommendations (indexes, caching, etc.)
- Performance documentation

---

### Phase 15: Infrastructure & CI/CD
1. Docker image build and push
2. Docker Compose for local development
3. PostgreSQL Docker image with init scripts
4. Redis Docker image
5. OpenSearch Docker image
6. GitHub Actions workflows:
   - Unit tests on every PR
   - Integration tests on main branch
   - Docker image build and push on release
   - IaC deployment (Terraform/AWS CDK) on release
7. Terraform/AWS CDK scripts:
   - RDS PostgreSQL 16 instance
   - Redis cluster
   - OpenSearch cluster
   - ECS/EKS for app deployment
   - Load balancer
   - CloudWatch monitoring
   - Auto-scaling policies

**Deliverables:**
- Dockerfile and Docker Compose
- GitHub Actions workflows
- IaC templates (Terraform/CDK)
- Deployment documentation
- Secret management setup (AWS Secrets Manager)
- Environment configuration templates

---

### Phase 16: Monitoring & Alerting
1. CloudWatch dashboards:
   - API latency (p50, p95, p99)
   - Error rates by endpoint
   - Database connection pool usage
   - Redis memory usage and hit rate
   - OpenSearch indexing lag
2. Alerts:
   - High error rate (>1%)
   - High latency (p95 >1s)
   - Database connection pool exhaustion
   - Search index lag >5 minutes
   - Payment provider errors
   - Low disk space on RDS
3. Log aggregation (CloudWatch Logs or ELK)
4. Distributed tracing (optional, Jaeger/Zipkin)

**Deliverables:**
- CloudWatch dashboards
- Alert rules and thresholds
- On-call escalation process
- Log aggregation setup
- Runbook for common incidents

---

### Phase 17: Documentation & Handoff
1. API documentation (Swagger/OpenAPI)
2. Architecture diagram (components, data flow)
3. Database schema diagram (ER diagram)
4. Deployment guide (local, staging, production)
5. Operations manual (backups, restores, scaling)
6. Security documentation (auth flow, RBAC model)
7. Troubleshooting guide
8. Code walkthrough video (optional)

**Deliverables:**
- Swagger/OpenAPI spec (auto-generated)
- Architecture docs with diagrams
- Database schema diagram
- Deployment runbooks
- Operations procedures
- Security architecture document
- README with quick-start guide

---

## Quality Assurance Checklist

Before marking any phase complete, verify:

- [ ] All code compiles without warnings
- [ ] Unit test coverage ≥80%
- [ ] All integration tests pass
- [ ] No hardcoded secrets in codebase
- [ ] All error cases handled (no bare `Exception` catch)
- [ ] Validation on all DTOs (unknown fields rejected)
- [ ] Authorization checks on all protected endpoints
- [ ] Ownership checks on all customer resource endpoints
- [ ] Concurrency tests pass (no race conditions)
- [ ] API response format is consistent
- [ ] Correlation IDs flow through all calls
- [ ] Structured logging in place
- [ ] Database migrations version-controlled
- [ ] Code reviewed (pair programming or PR review)
- [ ] Security review completed (OWASP Top 10)
- [ ] Documentation updated (README, API docs, architecture)
- [ ] Performance acceptable (latency, throughput, resource usage)
- [ ] No production stack traces in error responses
- [ ] Audit logging in place for sensitive operations
- [ ] Rate limiting configured
- [ ] Secrets externalized (environment variables, vault)

---

## Key Principles to Remember

### Business Rules
- ✅ Server calculates all totals, prices, discounts, taxes
- ✅ Server validates all stock levels and decrements
- ✅ Server determines order status transitions
- ✅ Server authenticates all requests
- ✅ Server checks authorization on all protected resources
- ✅ Audit log all sensitive actions

### Data Integrity
- ✅ Use `DECIMAL(10,2)` for money (never `double` or `float`)
- ✅ Use optimistic locking for inventory/order state
- ✅ Use transactions for multi-step operations
- ✅ Idempotent endpoints where applicable
- ✅ Explicit rounding for all calculations

### Security
- ✅ Never trust client data for pricing, stock, ownership
- ✅ Validate all inputs (type, length, format, business rules)
- ✅ Hash passwords (BCrypt ≥12 or Argon2id)
- ✅ Rotate refresh tokens
- ✅ Store hashed tokens in database
- ✅ Check authorization and ownership on every request
- ✅ Log security-relevant events
- ✅ Never commit secrets
- ✅ Use HTTPS in production
- ✅ Set security headers (X-Frame-Options, X-Content-Type-Options, etc.)

### Code Quality
- ✅ Keep controllers thin; business logic in services
- ✅ Dependency injection for all components
- ✅ Consistent error handling and response format
- ✅ Comprehensive unit and integration tests
- ✅ Code review before merge
- ✅ No dead code
- ✅ Meaningful commit messages
- ✅ Documentation for public APIs and complex logic

---

## References

- [Spring Boot 3.x Documentation](https://spring.io/projects/spring-boot)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [PostgreSQL 16 Documentation](https://www.postgresql.org/docs/)
- [Redis Documentation](https://redis.io/documentation)
- [OpenSearch Documentation](https://opensearch.org/docs/)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)

---

**Next Step:** Start with Phase 1 foundation work and follow the implementation phases sequentially. Adjust scope based on actual requirements and business priorities.
