# Backend API Specification v1

Base path: `/api/v1`

This document describes the endpoints currently mapped by the backend controllers. Unless an endpoint says otherwise, JSON request bodies use `Content-Type: application/json`. Unknown JSON properties are rejected. Examples omit optional fields unless they help illustrate the request.

## Common conventions

### Authentication and authorization

Send an access token as `Authorization: Bearer <accessToken>` for protected endpoints. Public endpoints are identified below. Admin endpoints require `ROLE_ADMIN`; other protected endpoints require an authenticated user.

The cart controller accepts an optional `X-Guest-Session` header, but security currently requires authentication for `/api/v1/cart/**`. Guest cart access is therefore not available without a security configuration change.

### Success response

Most endpoints return an `ApiResponse` envelope:

```json
{
  "success": true,
  "data": {},
  "message": "Request completed",
  "correlationId": "request-correlation-id",
  "timestamp": 1791660000000
}
```

Fields that do not apply to a particular response may be omitted. CSV exports and empty responses are documented separately.

### Errors

The standard error shape is:

```json
{
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable message",
    "correlationId": "request-correlation-id"
  }
}
```

Typical statuses include `400` for invalid input, `401` for missing or invalid authentication, `403` for insufficient permissions, `404` for a missing resource, `429` for a rate limit, and `500` for an unexpected server error. Some business errors currently use `422`.

## Health

### `GET /health/ping`

- **Authentication:** Public
- **Request body:** None
- **Success:** `200`; service status information in `data`.

### `GET /health/ready`

- **Authentication:** Public
- **Request body:** None
- **Success:** `200`; readiness information in `data`.

## Authentication

### `POST /auth/register`

- **Authentication:** Public
- **Request body:** `RegisterRequest`

```json
{
  "email": "customer@example.com",
  "firstName": "Jane",
  "lastName": "Doe",
  "password": "strong-password",
  "phone": "+15551234567"
}
```

`email`, `firstName`, `lastName`, and `password` are required. Names must be 2–100 characters; password must be 8–255 characters. `phone` is optional. Registration creates an inactive account and sends an email verification link. The link expires after 24 hours by default and can be used once.

- **Success:** `201 Created`; user data in the standard envelope.

### `GET /auth/verify-email?token={token}`

- **Authentication:** Public
- **Request body:** None
- **Query parameters:** `token` (required): one-time token delivered to the registered email address.
- **Success:** `200`; activates the account and marks its email as verified.
- **Failure:** `422` if the token is invalid, expired, or already used.

The verification link targets this backend endpoint by default. Set `EMAIL_VERIFICATION_URL` to the externally reachable HTTPS endpoint in non-local deployments.

### `POST /auth/login`

- **Authentication:** Public
- **Request body:** `LoginRequest`

```json
{
  "email": "customer@example.com",
  "password": "strong-password"
}
```

Both fields are required; `email` must be a valid email address.

- **Success:** `200`; `data` contains `accessToken`, `refreshToken`, `tokenType`, `expiresIn` (seconds), and `user`.

### `POST /auth/refresh`

- **Authentication:** Public; provide the refresh token in the request body.
- **Request body:** `RefreshTokenRequest`

```json
{
  "refreshToken": "<refresh-token>"
}
```

- **Success:** `200`; returns a refreshed authentication response.

### `POST /auth/logout`

- **Authentication:** Required
- **Request body:** `RefreshTokenRequest`

```json
{
  "refreshToken": "<refresh-token-to-revoke>"
}
```

- **Success:** `200`; returns a success envelope without `data`.

### `GET /auth/me`

- **Authentication:** Required
- **Request body:** None
- **Success:** `200`; current user data in the standard envelope.

### `POST /auth/verify-otp`

- **Authentication:** Public
- **Request body:** `OTPVerificationRequest`

```json
{
  "email": "customer@example.com",
  "code": "123456"
}
```

Supply exactly one of `email` or `phone`, together with the required `code`. For phone verification, use:

```json
{
  "phone": "+15551234567",
  "code": "123456"
}
```

- **Success:** `200`; success envelope without `data`.

### `POST /auth/request-password-reset`

- **Authentication:** Public
- **Request body:** `PasswordResetRequest`

```json
{
  "email": "customer@example.com"
}
```

- **Success:** `200`; generic confirmation message that does not disclose whether the account exists.

### `POST /auth/reset-password`

- **Authentication:** Public
- **Request body:** `PasswordResetConfirmRequest`

```json
{
  "email": "customer@example.com",
  "code": "123456",
  "newPassword": "new-strong-password"
}
```

All fields are required. The new password must be 8–255 characters.

- **Success:** `200`; success envelope without `data`.

## Products and search

### `GET /products`

- **Authentication:** Public
- **Request body:** None
- **Query parameters:**
  - `category` (optional): category filter.
  - `brand` (optional): brand filter.
  - `page` (optional, default `0`): zero-based page index.
  - `size` (optional, default `20`): page size, from 1 through 100.
- **Success:** `200`; page of active product data in the standard envelope.

### `GET /products/{id}`

- **Authentication:** Public
- **Path parameters:** `id` is the product UUID.
- **Request body:** None
- **Success:** `200`; product data in the standard envelope.

### `GET /search`

- **Authentication:** Required by the current security configuration
- **Request body:** None
- **Query parameters:**
  - `query` (optional): text query.
  - `category`, `brand` (optional): filters.
  - `minPrice`, `maxPrice` (optional): price bounds.
  - `sortBy` (optional, default `relevance`): documented values are `relevance`, `price_asc`, `price_desc`, and `newest`.
  - `page` (optional, default `0`): zero-based page index.
  - `size` (optional, default `20`): page size, from 1 through 100.
- **Success:** `200`; page of search results in the standard envelope.

**Implementation note:** The current search service applies pagination to the OpenSearch request, but does not yet apply the query, filters, or sort parameter.

### `GET /search/suggest`

- **Authentication:** Required by the current security configuration
- **Request body:** None
- **Query parameters:**
  - `q` (required): non-blank suggestion prefix.
  - `limit` (optional, default `10`): from 1 through 100.
- **Success:** `200`; list of suggestion strings in the standard envelope.

**Implementation note:** Suggestion lookup is currently a placeholder and returns an empty list.

## Cart

All cart routes currently require authentication. They also accept optional `X-Guest-Session`, but unauthenticated guest access is not enabled by the security configuration.

### `GET /cart`

- **Request body:** None
- **Headers:** `X-Guest-Session` (optional)
- **Success:** `200`; cart data in the standard envelope.

### `POST /cart/items`

- **Request body:** `CartItemRequest`

```json
{
  "productId": "8b2f071d-6cc5-4be4-a3cf-1af9326a0f44",
  "quantity": 2
}
```

`productId` is required and `quantity` must be at least 1. `quantity` defaults to 1 when omitted.

- **Headers:** `X-Guest-Session` (optional)
- **Success:** `200`; updated cart in the standard envelope.

### `PUT /cart/items/{productId}`

- **Path parameters:** `productId` is the product UUID.
- **Request body:**

```json
{
  "quantity": 3
}
```

The quantity must be at least 1. The product ID is taken from the path.

- **Headers:** `X-Guest-Session` (optional)
- **Success:** `200`; updated cart in the standard envelope.

### `DELETE /cart/items/{productId}`

- **Path parameters:** `productId` is the product UUID.
- **Request body:** None
- **Headers:** `X-Guest-Session` (optional)
- **Success:** `200`; updated cart in the standard envelope.

## Orders

### `POST /orders`

- **Authentication:** Required
- **Headers:** `Idempotency-Key` (optional in the current controller; clients should provide a unique key for each order-creation attempt).
- **Request body:** `OrderRequest`

```json
{
  "shippingAddress": "123 Example Street, Apartment 4",
  "city": "Example City",
  "state": "CA",
  "postalCode": "90210",
  "country": "US"
}
```

`shippingAddress`, `city`, `postalCode`, and `country` are required. `state` is optional.

Do not send prices, discounts, totals, stock values, order status, or user ownership; these are determined by the backend.

- **Success:** `201 Created`; created order data in the standard envelope.

## Admin catalog

All `/admin/**` endpoints require `ROLE_ADMIN`.

### `POST /admin/catalog/categories`

- **Request body:** `CatalogNameRequest`

```json
{
  "name": "Home",
  "slug": "home"
}
```

Both fields are required. `name` is limited to 120 characters and `slug` to 140 characters.

- **Success:** `201 Created`; created category in the standard envelope.

### `GET /admin/catalog/categories`

- **Request body:** None
- **Success:** `200`; list of categories in the standard envelope.

### `POST /admin/catalog/brands`

- **Request body:** `CatalogNameRequest`

```json
{
  "name": "Example Brand",
  "slug": "example-brand"
}
```

Both fields are required. `name` is limited to 120 characters and `slug` to 140 characters.

- **Success:** `201 Created`; created brand in the standard envelope.

### `GET /admin/catalog/brands`

- **Request body:** None
- **Success:** `200`; list of brands in the standard envelope.

## Admin products

All endpoints require `ROLE_ADMIN`.

### `POST /admin/products`

- **Request body:** `ProductRequest`

```json
{
  "sku": "SKU-1001",
  "name": "Example Product",
  "description": "Product description",
  "categoryId": "8b2f071d-6cc5-4be4-a3cf-1af9326a0f44",
  "brandId": "de1ea672-d6d6-45d5-85fb-a3514334df44",
  "basePrice": 49.99,
  "discountType": "PERCENTAGE",
  "discountValue": 10,
  "discountStartsAt": "2026-10-10T12:00:00Z",
  "discountEndsAt": "2026-10-31T23:59:59Z",
  "stock": 25,
  "attributes": {
    "color": "blue"
  },
  "images": {
    "primary": "https://example.com/product.jpg"
  },
  "active": true
}
```

Required fields: `sku` (max 64 characters), `name` (max 200 characters), `description`, `categoryId`, `brandId`, and `basePrice` (from `0.01` to `99999999.99`). `stock` must be nonnegative. Discount fields, timestamp fields, `attributes`, and `images` are optional; `discountType` is `PERCENTAGE` or `FIXED`. `active` defaults to `true`.

- **Success:** `201 Created`; created product in the standard envelope.

### `PUT /admin/products/{id}`

- **Path parameters:** `id` is the product UUID.
- **Request body:** Same `ProductRequest` shape as `POST /admin/products`.
- **Success:** `200`; updated product in the standard envelope.

### `GET /admin/products`

- **Query parameters:** `page` (default `0`), `size` (default `20`, from 1 through 100).
- **Request body:** None
- **Success:** `200`; page of products in the standard envelope.

### `DELETE /admin/products/{id}`

- **Path parameters:** `id` is the product UUID.
- **Request body:** None
- **Success:** `200`; success envelope indicating the product was deactivated.

## Admin discounts

All endpoints require `ROLE_ADMIN`. `{productId}` is the product UUID.

### `POST /admin/products/{productId}/discount`

- **Request body:** `DiscountRequest`

```json
{
  "type": "PERCENTAGE",
  "value": 10,
  "startsAt": "2026-10-10T12:00:00Z",
  "endsAt": "2026-10-31T23:59:59Z"
}
```

`type` and `value` are required. `type` is `PERCENTAGE` or `FIXED`; `value` must be at least `0.01`. Start and end instants are optional.

- **Success:** `201 Created`; discount data in the standard envelope.

### `PUT /admin/products/{productId}/discount`

- **Request body:** Same `DiscountRequest` shape as `POST`.
- **Success:** `200`; updated discount in the standard envelope.

### `GET /admin/products/{productId}/discount`

- **Request body:** None
- **Success:** `200`; discount data in the standard envelope.

### `DELETE /admin/products/{productId}/discount`

- **Request body:** None
- **Success:** `200`; success envelope without `data`.

## Admin reports

All endpoints require `ROLE_ADMIN`. Unless otherwise noted, `from` and `to` accept ISO-8601 instants and default to the last 30 days and current time, respectively.

### `GET /admin/reports/orders`

- **Query parameters:** `from`, `to`, `status` (optional; `PENDING`, `CONFIRMED`, `PACKED`, `SHIPPED`, `OUT_FOR_DELIVERY`, `DELIVERED`, or `CANCELLED`), `page` (default `0`), `size` (default `20`, from 1 through 100).
- **Request body:** None
- **Success:** `200`; paged order report in the standard envelope.

### `GET /admin/reports/orders.csv`

- **Query parameters:** `from`, `to`, `status` (optional; same values as above).
- **Request body:** None
- **Success:** `200`; `text/csv` attachment named `order-report.csv`.

### `GET /admin/reports/cancellations`

- **Query parameters:** `from`, `to` (optional; same defaults as above).
- **Request body:** None
- **Success:** `200`; cancellation report list in the standard envelope.

### `GET /admin/reports/metrics`

- **Query parameters:** `from`, `to` (optional; same defaults as above).
- **Request body:** None
- **Success:** `200`; summary metrics in the standard envelope.

### `GET /admin/reports/audit`

- **Query parameters:** `from`, `to` (optional; same defaults as above), `page` (default `0`), `size` (default `20`, from 1 through 100).
- **Request body:** None
- **Success:** `200`; paged audit report in the standard envelope.

## Admin system settings

All endpoints require `ROLE_ADMIN`.

### `GET /admin/settings`

- **Request body:** None
- **Success:** `200`; list of system settings in the standard envelope.

### `PUT /admin/settings/{key}`

- **Path parameters:** `key` is the setting key.
- **Request body:**

```json
{
  "value": "15"
}
```

`value` is required and must be a non-blank string. The backend validates it against the selected setting's rules.

- **Success:** `200`; updated setting in the standard envelope.

## Routes listed in the earlier overview but not currently implemented

The following route families do not currently have controller mappings; do not treat them as available APIs until implemented:

- OTP request (`POST /auth/otp/request`).
- Email/mobile linking (`POST /users/me/link-email`, `POST /users/me/link-mobile`).
- User profile/address routes (`GET` or `PATCH /users/me`, `GET` or `POST /users/me/addresses`).
- Customer order listing/details/cancellation/support requests (`GET /orders`, `GET /orders/{id}`, `POST /orders/{id}/cancel`, `POST /orders/{id}/support-request`).
- Admin order management and support-request queue/resolution routes.

`POST /auth/verify-otp` and the password-reset routes are implemented under the paths documented above; there is no separate `/auth/otp/verify` mapping.
