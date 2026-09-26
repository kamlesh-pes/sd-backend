# Implementation Guide — Quick Reference

**Read this first.** Then refer to the detailed documents below for specific work.

---

## For All Developers & AI Agents

### The Golden Rule
**All authoritative business, security, pricing, stock, and authorization rules belong on the server.**

Never trust:
- Client-provided price, discount, totals, or effective price
- Client-provided stock levels
- Client ownership claims
- Client payment success confirmation
- Client cancellation eligibility
- Client security timestamps

Always:
- Validate and recalculate server-side
- Check ownership and authorization on every request
- Use transactions for multi-step operations
- Audit sensitive actions
- Log structured JSON with correlation IDs

---

## Document Navigation

### 1. **Read First: AGENTS.md** (root)
- Non-negotiable security & business rules
- Stack overview
- Quick reference for authentication, API, and observability requirements

### 2. **Implementation Blueprint: docs/implementation-plan.md** (NEW)
- Comprehensive implementation roadmap (17 phases)
- Detailed requirements for each component
- Quality assurance checklist
- All principles and key takeaways in one place

### 3. **Specification Documents**
- **docs/backend-spec.md** — Service responsibilities and behavior
- **docs/data-model.md** — Database schema and entity definitions
- **docs/api-spec.md** — REST API endpoints and contracts
- **docs/security.md** — Authentication, authorization, and compliance
- **docs/testing.md** — Testing strategy and coverage expectations

### 4. **Build Plan**
- **docs/build-plan.md** — Original 17-phase plan (reference for scope)

---

## Before Starting Any Work

1. ✅ Read **AGENTS.md** (non-negotiables)
2. ✅ Read **docs/implementation-plan.md** (full roadmap)
3. ✅ Read relevant **specification documents** (backend-spec, api-spec, data-model, security, testing)
4. ✅ Check **acceptance-criteria.md** for what "done" means
5. ✅ Review **this guide** (IMPLEMENTATION_GUIDE.md) — you're reading it now!

---

## Implementation Phases at a Glance

| Phase | Focus | Priority | Dependencies |
|-------|-------|----------|--------------|
| 1 | Foundation (Spring Boot, Docker, DB, Redis, OpenSearch) | 🔴 P0 | None |
| 2 | Auth (users, passwords, JWT, refresh tokens, OTP, RBAC) | 🔴 P0 | Phase 1 |
| 3 | Products (CRUD, admin endpoints, audit logging) | 🔴 P0 | Phase 2 |
| 4 | Search (OpenSearch indexing, full-text, filters) | 🟡 P1 | Phase 1, 3 |
| 5 | Cart (add/remove items, guest sessions, merge) | 🟡 P1 | Phase 2, 3 |
| 6 | Checkout (order creation, stock decrement, idempotency) | 🔴 P0 | Phase 2, 3, 5 |
| 7 | Payment (abstraction, providers, webhooks) | 🟡 P1 | Phase 6 |
| 8 | Order Lifecycle (status transitions, cancellation, support) | 🟡 P1 | Phase 6, 7 |
| 9 | Discounts (admin, application, audit logging) | 🟢 P2 | Phase 3, 6 |
| 10 | Reports (orders, cancellations, CSV, audit trail) | 🟢 P2 | Phase 6, 8 |
| 11 | Security Settings (configurable, cached, audit logged) | 🟢 P2 | Phase 2 |
| 12 | Rate Limiting & Hardening (DDoS protection, security headers) | 🟡 P1 | Phase 1 |
| 13 | Testing (unit, integration, concurrency, security) | 🔴 P0 | All phases |
| 14 | Load Testing (performance validation, optimization) | 🟡 P1 | Phase 13 |
| 15 | Infrastructure & CI/CD (Docker, GitHub Actions, Terraform) | 🟡 P1 | Phase 1 |
| 16 | Monitoring & Alerting (CloudWatch, dashboards, alerts) | 🟢 P2 | Phase 15 |
| 17 | Documentation & Handoff (API docs, runbooks, guides) | 🟡 P1 | All phases |

---

## Non-Negotiable Implementation Requirements

### Authentication & Authorization
- ✅ Password hashing: BCrypt ≥12 or Argon2id
- ✅ JWT access token (15min default, configurable)
- ✅ Rotating refresh tokens (hashed, stale-token detection)
- ✅ Secure httpOnly cookies (preferred over headers)
- ✅ OTP: E.164 normalized, hashed, 5min expiry, 5 attempts, rate-limited
- ✅ RBAC with `ROLE_ADMIN` for sensitive operations
- ✅ Ownership checks on all customer resources
- ✅ Session revocation support
- ✅ Device binding (optional but recommended)
- ✅ Step-up authentication for sensitive operations

### Data & Money
- ✅ PostgreSQL `DECIMAL(10,2)` for all money fields
- ✅ Java `BigDecimal` for all calculations (never `float` or `double`)
- ✅ Explicit rounding (HALF_UP mode)
- ✅ Server-side price and total validation
- ✅ Server-side discount application and effective price calculation
- ✅ Server-side tax and shipping calculations

### Order & Inventory
- ✅ Idempotent order creation (via idempotency key)
- ✅ Transactional stock decrement with optimistic locking
- ✅ Order status only via backend/admin processes (never client)
- ✅ Cancellation eligibility from server `Order.created_at`
- ✅ OrderStatusHistory logging on every transition
- ✅ Support request required for post-24h cancellations

### API
- ✅ Endpoint version: `/api/v1`
- ✅ Validate all DTOs (reject unknown fields)
- ✅ Pagination limits enforced (max 100 items per page)
- ✅ Correlation ID on all responses
- ✅ Consistent error envelope
- ✅ No production stack traces in responses

### Observability
- ✅ Structured JSON logging with correlation IDs
- ✅ `/actuator/health` endpoint
- ✅ Micrometer metrics (latency, errors, etc.)
- ✅ Audit logging for sensitive actions
- ✅ Never log secrets, passwords, tokens, or PII

### Audit Logging
- ✅ Admin actions (user management, order updates, refunds)
- ✅ Security events (login, password change, role grants)
- ✅ Configuration changes (discounts, security settings)
- ✅ Discount application and changes
- ✅ Support request lifecycle
- ✅ All actions must include: timestamp, actor, action, target, changes

### Security
- ✅ Never commit secrets to repo
- ✅ Use environment variables or secure vaults
- ✅ HTTPS enforced in production
- ✅ Security headers (X-Frame-Options, X-Content-Type-Options, etc.)
- ✅ CORS properly configured
- ✅ SQL injection prevention (parameterized queries)
- ✅ Input validation and sanitization
- ✅ Rate limiting (Redis-backed)
- ✅ No hardcoded credentials or API keys

### Testing
- ✅ Unit test coverage ≥80%
- ✅ Integration tests (database, transactions)
- ✅ Endpoint tests (REST layer)
- ✅ Concurrency tests (stock decrement, order creation)
- ✅ Security tests (ownership, RBAC, authorization)
- ✅ Data validation tests (unknown fields, type errors)

---

## Execution Workflow

### Before Starting a Phase
1. Read [docs/implementation-plan.md](docs/implementation-plan.md) for the phase requirements
2. Check relevant specification documents (api-spec, data-model, security, etc.)
3. Review acceptance criteria and QA checklist
4. Estimate scope and dependencies
5. Create GitHub issue(s) for the phase
6. Assign to developer(s)

### During Development
1. ✅ Write unit tests first (TDD preferred)
2. ✅ Implement business logic in services (keep controllers thin)
3. ✅ Add integration tests for database/transaction behavior
4. ✅ Add endpoint tests (REST layer)
5. ✅ Implement audit logging for sensitive operations
6. ✅ Add validation to all DTOs
7. ✅ Ensure authorization/ownership checks
8. ✅ Use structured logging with correlation IDs
9. ✅ No hardcoded secrets
10. ✅ Code review before merge

### After Completing a Phase
1. ✅ Verify all unit/integration tests pass
2. ✅ Run QA checklist (see docs/implementation-plan.md)
3. ✅ Update API documentation (Swagger)
4. ✅ Update architecture/data model diagrams if changed
5. ✅ Document new configuration options or environment variables
6. ✅ Close GitHub issue(s)
7. ✅ Merge to main branch
8. ✅ Deploy to staging environment
9. ✅ Manual testing in staging
10. ✅ Mark phase complete in the implementation tracker

---

## Common Patterns & Examples

### Server-Side Total Calculation
```
CartTotal = sum(cartItems.price × cartItems.quantity)
Subtotal = CartTotal
DiscountAmount = Subtotal × discountPercent (if applicable)
TaxableAmount = Subtotal - DiscountAmount
Tax = TaxableAmount × taxRate
ShippingCost = (determined by backend, not client)
OrderTotal = TaxableAmount + Tax + ShippingCost
```
**Never accept client-calculated totals.**

### Password Hashing (BCrypt Example)
```java
BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12); // strength >= 12
String hashed = encoder.encode(rawPassword);
boolean matches = encoder.matches(rawPassword, hashed);
```

### Audit Logging
```java
auditLogService.log(AuditAction.ORDER_CANCEL, 
    actor: currentUser, 
    target: Order, 
    targetId: orderId,
    changes: { "status": "PENDING" → "CANCELLED", "cancelled_reason": "User requested" }
);
```

### Server-Side Authorization Check
```java
@GetMapping("/api/v1/orders/{orderId}")
public ResponseEntity<?> getOrder(@PathVariable String orderId, @CurrentUser User user) {
    Order order = orderRepository.findById(orderId).orElseThrow(() -> 
        new ResourceNotFoundException("Order not found"));
    
    // Ownership check
    if (!order.getCustomerId().equals(user.getId()) && !user.hasRole("ROLE_ADMIN")) {
        throw new AccessDeniedException("You do not own this order");
    }
    
    return ResponseEntity.ok(orderToDTO(order));
}
```

### Idempotent Request Handling
```java
@PostMapping("/api/v1/orders")
public ResponseEntity<?> createOrder(@RequestBody OrderRequest request,
    @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {
    
    // Check if we already processed this request
    Optional<Order> existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey);
    if (existingOrder.isPresent()) {
        return ResponseEntity.status(201).body(orderToDTO(existingOrder.get()));
    }
    
    // Process order (stock decrement, etc.)
    Order order = processOrder(request);
    order.setIdempotencyKey(idempotencyKey);
    orderRepository.save(order);
    
    return ResponseEntity.status(201).body(orderToDTO(order));
}
```

---

## Troubleshooting & Support

### Performance Issues
- Check database query logs (slow query log)
- Check database indexes (see data-model.md)
- Monitor Redis hit rate
- Check OpenSearch indexing lag
- Profile with JProfiler or async-profiler

### Concurrency Issues
- Add optimistic locking (@Version on entities)
- Use transactions (@Transactional)
- Review business logic for race conditions
- Add concurrency tests

### Security Issues
- Run OWASP dependency check (gradle owasp)
- Review OWASP Top 10 checklist
- Audit secrets management
- Review authorization and ownership checks
- Check for SQL injection vulnerabilities

### Deployment Issues
- Check environment variables are set
- Verify database migrations ran
- Check Redis connectivity
- Verify OpenSearch connectivity
- Check CloudWatch logs for errors

---

## Key Contacts & Resources

- **Architecture & Design:** See [docs/implementation-plan.md](docs/implementation-plan.md)
- **API Contracts:** See [docs/api-spec.md](docs/api-spec.md)
- **Database Schema:** See [docs/data-model.md](docs/data-model.md)
- **Security Requirements:** See [docs/security.md](docs/security.md)
- **Testing Strategy:** See [docs/testing.md](docs/testing.md)
- **Non-Negotiables:** See [AGENTS.md](../AGENTS.md)

---

## Success Criteria

By the end of all 17 phases, the system will:

1. ✅ Authenticate users securely (BCrypt/Argon2id, JWT + refresh tokens)
2. ✅ Authorize requests (RBAC, ownership checks)
3. ✅ Manage products with admin CRUD
4. ✅ Search products with full-text search, filters, type-ahead
5. ✅ Support guest and authenticated shopping carts
6. ✅ Process orders idempotently with server-side calculations
7. ✅ Integrate with payment providers securely
8. ✅ Handle order lifecycle (status transitions, cancellations, support)
9. ✅ Apply discounts server-side
10. ✅ Generate reports and audit trails
11. ✅ Enforce configurable security settings
12. ✅ Rate-limit API requests to prevent abuse
13. ✅ Pass comprehensive test suite (unit, integration, security, concurrency)
14. ✅ Handle high load gracefully (p95 latency acceptable)
15. ✅ Deploy via CI/CD (GitHub Actions, Docker, Terraform)
16. ✅ Provide observability (metrics, logs, traces, dashboards)
17. ✅ Document all APIs and operational procedures

---

**Next Step:** Choose a phase based on priority. Read [docs/implementation-plan.md](docs/implementation-plan.md) for detailed requirements. Start with Phase 1 (foundation) if beginning from scratch.
