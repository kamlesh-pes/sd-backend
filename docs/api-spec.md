# Backend API Specification v1

Base: `/api/v1`

### Auth
`POST /auth/register`, `/auth/login`, `/auth/otp/request`, `/auth/otp/verify`, `/auth/refresh`, `/auth/logout`, `/users/me/link-email`, `/users/me/link-mobile`

### Products/search
`GET /products`, `GET /products/{id}`, `GET /search?q=...`, `GET /search/suggest?q=...`

### Cart
`GET /cart`, `POST /cart/items`, `PATCH /cart/items/{itemId}`, `DELETE /cart/items/{itemId}`

### Orders
`POST /orders`, `GET /orders`, `GET /orders/{id}`, `POST /orders/{id}/cancel`, `POST /orders/{id}/support-request`

### User
`GET /users/me`, `PATCH /users/me`, `GET /users/me/addresses`, `POST /users/me/addresses`

### Admin
Product CRUD/stock/discount; admin orders; order/cancellation reports and CSV; audit log; support request queue/resolve; session settings.

All admin endpoints require `ROLE_ADMIN`.

### Error envelope
```json
{"error":{"code":"ERROR_CODE","message":"Human-readable message","correlationId":"request-id"}}
```

Use 401 for missing/invalid auth and 403 for insufficient role. Validate DTOs, reject unknown fields, enforce pagination limits, and never expose stack traces.
