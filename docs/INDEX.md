# Sahastra Digital Backend — Documentation Index

**Updated:** 2026-08-29

---

## 📋 Quick Start Navigation

### For Developers Starting Their First Task
1. **[IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md)** ⭐ START HERE
   - Overview of all phases at a glance
   - Non-negotiable requirements checklist
   - Common patterns and examples
   - Phase priority matrix

2. **[../AGENTS.md](../AGENTS.md)** — Read after the guide
   - AI agent instructions and principles
   - Stack overview
   - Non-negotiable security & business rules

### For Detailed Implementation Work
3. **[implementation-plan.md](implementation-plan.md)** — Comprehensive roadmap
   - All 17 phases with detailed requirements
   - Architecture principles
   - Quality assurance checklist
   - Key principles to remember

---

## 📚 Specification Documents

| Document | Purpose | Read When |
|----------|---------|-----------|
| [backend-spec.md](backend-spec.md) | Service responsibilities, search, cart, checkout, order lifecycle, cancellation, discounts, reports, configurable settings | Phase 3+ (products, orders, discounts) |
| [api-spec.md](api-spec.md) | REST API endpoints, request/response contracts, error codes | Before implementing any endpoint |
| [data-model.md](data-model.md) | Database schema, entities, relationships, indexes | Phase 1 (database setup) |
| [security.md](security.md) | Authentication, authorization, compliance requirements | Phase 2 (auth implementation) |
| [testing.md](testing.md) | Testing strategy, unit/integration/concurrency tests, coverage targets | Phase 13 (testing) |
| [acceptance-criteria.md](acceptance-criteria.md) | Definition of "done" for each component | After each phase completion |
| [build-plan.md](build-plan.md) | Original 17-phase plan (reference) | Reference during planning |

---

## 🎯 Implementation Phases Summary

### Phase 1 — Foundation (Infrastructure)
- Spring Boot scaffold, Docker Compose
- PostgreSQL 16, Redis, OpenSearch setup
- Error handling, logging, correlation ID middleware
- **Read:** [implementation-plan.md — Phase 1](implementation-plan.md#phase-1-foundation-core-infrastructure)

### Phase 2 — Authentication & Authorization
- User registration, login, JWT tokens, refresh tokens
- OTP implementation, RBAC, session management
- Password hashing (BCrypt/Argon2id)
- **Read:** [implementation-plan.md — Phase 2](implementation-plan.md#phase-2-authentication--authorization)

### Phase 3 — Product & Catalog Management
- Product CRUD (admin-only), categories, brands
- Pricing and stock management
- Product audit logging
- **Read:** [implementation-plan.md — Phase 3](implementation-plan.md#phase-3-product--catalog-management)

### Phase 4 — Search & Indexing
- OpenSearch integration
- Full-text search, fuzzy matching, type-ahead, filters
- Async product indexing (outbox/CDC)
- **Read:** [implementation-plan.md — Phase 4](implementation-plan.md#phase-4-search--indexing)

### Phase 5 — Cart Management
- Guest and authenticated carts
- Cart item operations, stock validation
- Guest-to-authenticated merge after login
- **Read:** [implementation-plan.md — Phase 5](implementation-plan.md#phase-5-cart-management)

### Phase 6 — Checkout & Order Creation
- Checkout endpoint, order entity
- Transactional stock decrement with optimistic locking
- Idempotent order creation
- **Read:** [implementation-plan.md — Phase 6](implementation-plan.md#phase-6-checkout--order-creation)

### Phase 7 — Payment Abstraction
- PaymentProvider interface and implementations
- Mock and real payment provider integration
- Webhook handling and payment confirmation
- **Read:** [implementation-plan.md — Phase 7](implementation-plan.md#phase-7-payment-abstraction)

### Phase 8 — Order Lifecycle & Cancellation
- Order status transitions and history
- Customer self-cancel (within 24h)
- Support request flow
- Admin cancellation and refunds
- **Read:** [implementation-plan.md — Phase 8](implementation-plan.md#phase-8-order-lifecycle--cancellation)

### Phase 9 — Discount Management
- Discount CRUD (admin-only)
- Effective price calculation
- Discount application in cart/checkout
- **Read:** [implementation-plan.md — Phase 9](implementation-plan.md#phase-9-discount-management)

### Phase 10 — Reporting & Export
- Order reports, cancellation reports, metrics
- CSV export functionality
- Audit trail reporting
- **Read:** [implementation-plan.md — Phase 10](implementation-plan.md#phase-10-reporting--export)

### Phase 11 — Configurable Security Settings
- Configurable token expiry times
- OTP settings, login rate limits
- Settings caching and audit logging
- **Read:** [implementation-plan.md — Phase 11](implementation-plan.md#phase-11-configurable-security-settings)

### Phase 12 — Rate Limiting & Security Hardening
- Redis-backed rate limiting
- Login rate limit (5/min per IP)
- Security headers (X-Frame-Options, etc.)
- DDoS protection
- **Read:** [implementation-plan.md — Phase 12](implementation-plan.md#phase-12-rate-limiting--security-hardening)

### Phase 13 — Comprehensive Testing
- Unit tests (business logic, validation)
- Integration tests (database, transactions)
- Endpoint tests (REST layer)
- Concurrency tests (stock decrement, order creation)
- Security tests (ownership, RBAC)
- **Target:** Unit test coverage ≥80%
- **Read:** [implementation-plan.md — Phase 13](implementation-plan.md#phase-13-comprehensive-testing)

### Phase 14 — Load Testing
- Performance testing with simulated load
- Bottleneck identification
- Optimization recommendations
- **Read:** [implementation-plan.md — Phase 14](implementation-plan.md#phase-14-load-testing)

### Phase 15 — Infrastructure & CI/CD
- Docker images and Docker Compose
- GitHub Actions workflows
- Terraform/AWS CDK infrastructure
- Secret management
- **Read:** [implementation-plan.md — Phase 15](implementation-plan.md#phase-15-infrastructure--cicd)

### Phase 16 — Monitoring & Alerting
- CloudWatch dashboards
- Alert rules (error rate, latency, etc.)
- Log aggregation
- Distributed tracing (optional)
- **Read:** [implementation-plan.md — Phase 16](implementation-plan.md#phase-16-monitoring--alerting)

### Phase 17 — Documentation & Handoff
- API documentation (Swagger/OpenAPI)
- Architecture and schema diagrams
- Deployment runbooks
- Operations procedures
- **Read:** [implementation-plan.md — Phase 17](implementation-plan.md#phase-17-documentation--handoff)

---

## 🔐 Non-Negotiable Requirements (Always Enforce)

### ✅ Must Have
- **Authentication:** BCrypt ≥12 or Argon2id; JWT access + rotating refresh tokens
- **Authorization:** Server-side ownership checks; RBAC with `ROLE_ADMIN`
- **Money:** PostgreSQL `DECIMAL(10,2)` and Java `BigDecimal`; explicit rounding
- **Orders:** Idempotent creation; transactional stock decrement; optimistic locking
- **Validation:** All DTOs validated; unknown fields rejected
- **Audit:** All sensitive actions logged with timestamp, actor, target, changes
- **Security:** No secrets in repo; structured logging; correlation IDs; no stack traces in prod
- **Testing:** Unit tests ≥80% coverage; integration tests; concurrency tests
- **API:** Version `/api/v1`; consistent error envelope; pagination limits enforced

### ❌ Never Do
- Accept client-provided price, discount, totals, or stock
- Trust client cancellation eligibility or payment success
- Log passwords, secrets, tokens, or PII
- Commit secrets to repository
- Use `double` or `float` for money
- Allow direct client order status updates
- Skip authorization checks
- Skip ownership checks on customer resources

---

## 📖 How to Use This Documentation

### Scenario 1: Starting Phase X
1. Open [implementation-plan.md](implementation-plan.md#phase-x) → find "Phase X"
2. Read the phase requirements and deliverables
3. Check if you need to read any specification documents (links provided)
4. Review the QA checklist at the end of the document
5. Start implementing

### Scenario 2: Implementing an API Endpoint
1. Open [api-spec.md](api-spec.md) to find the endpoint contract
2. Open [backend-spec.md](backend-spec.md) to understand the business logic
3. Check [data-model.md](data-model.md) for database schema
4. Check [security.md](security.md) for authentication/authorization requirements
5. Implement with validation, tests, and audit logging

### Scenario 3: Reviewing Security
1. Read [AGENTS.md](../AGENTS.md) — non-negotiables section
2. Read [security.md](security.md) for detailed requirements
3. Read [implementation-plan.md](implementation-plan.md) — "Non-Negotiable Security & Business Rules"
4. Run OWASP checks and security tests

### Scenario 4: Writing Tests
1. Read [testing.md](testing.md) for testing strategy
2. Read [implementation-plan.md](implementation-plan.md#phase-13-comprehensive-testing) for test coverage expectations
3. Implement unit, integration, endpoint, concurrency, and security tests

---

## 🚀 Getting Help

### Find Documentation For...
- **"How do I implement X?"** → Start with [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md), then [implementation-plan.md](implementation-plan.md)
- **"What's the API contract for endpoint Y?"** → [api-spec.md](api-spec.md)
- **"What's the database schema?"** → [data-model.md](data-model.md)
- **"What are security requirements?"** → [security.md](security.md)
- **"How do I test this?"** → [testing.md](testing.md)
- **"What are the acceptance criteria?"** → [acceptance-criteria.md](acceptance-criteria.md)
- **"What are the non-negotiable rules?"** → [../AGENTS.md](../AGENTS.md)

---

## 📋 Document Statistics

| Document | Size | Lines | Purpose |
|----------|------|-------|---------|
| IMPLEMENTATION_GUIDE.md | 12 KB | 334 | Quick reference and navigation guide |
| implementation-plan.md | 22 KB | 697 | Comprehensive 17-phase roadmap |
| backend-spec.md | ~5 KB | ~150 | Service behavior and business logic |
| api-spec.md | ~5 KB | ~100 | REST API contracts |
| data-model.md | 1.5 KB | ~50 | Database schema |
| security.md | 1.5 KB | ~50 | Security requirements |
| testing.md | 677 B | ~30 | Testing strategy |
| acceptance-criteria.md | ~2 KB | ~50 | Definition of "done" |
| build-plan.md | 608 B | ~25 | Original phase plan (reference) |

**Total:** ~55 KB documentation providing comprehensive guidance for all development work.

---

## ✅ Validation Checklist

Before starting any implementation:

- [ ] Read [IMPLEMENTATION_GUIDE.md](IMPLEMENTATION_GUIDE.md)
- [ ] Read [../AGENTS.md](../AGENTS.md) (AI agent instructions)
- [ ] Read relevant specification documents (backend-spec, api-spec, data-model, security, testing)
- [ ] Understand the non-negotiable requirements
- [ ] Review the phase-specific requirements in [implementation-plan.md](implementation-plan.md)
- [ ] Check [acceptance-criteria.md](acceptance-criteria.md) for the definition of "done"
- [ ] Have a plan for testing (unit, integration, concurrency, security)
- [ ] Understand where to add audit logging
- [ ] Know how to validate and authorize requests

**Ready to start?** Pick a phase from [implementation-plan.md](implementation-plan.md) and follow the detailed requirements!

---

**Document Version:** 2026-08-29  
**Last Reviewed:** Today  
**Status:** Current and Complete
