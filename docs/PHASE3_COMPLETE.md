# Phase 3: Product and Catalog Management

## Completed

- `Category`, `Brand`, and `Product` JPA entities
- Unique SKU and normalized catalog slugs
- PostgreSQL V3 migration with catalog indexes and business constraints
- `BigDecimal` pricing with explicit two-decimal rounding
- Percentage and fixed discounts with start/end windows
- Server-side effective-price calculation
- Product stock and `@Version` optimistic locking field
- Public product list/detail endpoints with bounded pagination and filters
- Admin product create/update/deactivate/list endpoints
- Admin category and brand creation/list endpoints
- Product change audit records with actor and correlation ID
- Unit tests for discount pricing and expiration

## Endpoints

- `GET /api/v1/products`
- `GET /api/v1/products/{id}`
- `POST /api/v1/admin/products`
- `PUT /api/v1/admin/products/{id}`
- `DELETE /api/v1/admin/products/{id}`
- `GET /api/v1/admin/products`
- `POST /api/v1/admin/catalog/categories`
- `GET /api/v1/admin/catalog/categories`
- `POST /api/v1/admin/catalog/brands`
- `GET /api/v1/admin/catalog/brands`

All `/api/v1/admin/**` routes require `ROLE_ADMIN`; product retrieval is public.

## Deferred To Phase 4

OpenSearch product documents, full-text search, fuzzy matching, suggestions, and asynchronous indexing are intentionally not included in this phase.

## Verification

Run with Java 21:

```bash
JAVA_HOME=/path/to/java-21 mvn test
```
