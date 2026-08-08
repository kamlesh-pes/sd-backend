# Backend Specification

## Responsibilities
Authentication/session, authorization, users/addresses, product catalog, search indexing, cart, checkout/orders, payment abstraction, stock integrity, cancellation/support, admin management, reports/CSV, audit, security settings, rate limiting, observability.

## Search
Full text across name/brand/category/description/SKU, fuzzy matching, type-ahead, filters, relevance/price/newest sorting, pagination. Target p95 <300ms. Product writes are asynchronous to OpenSearch.

## Cart
Guest session carts and authenticated carts. Validate stock and recalculate totals server-side. Merge guest cart after authentication.

## Checkout
Authentication + idempotency key. Server calculates subtotal/discount/tax/shipping/total. Payment behind `PaymentProvider`; real confirmation server-side. Transactional stock decrement and order creation. Optimistic locking prevents overselling.

## Order lifecycle
`PENDING → CONFIRMED → PACKED → SHIPPED → OUT_FOR_DELIVERY → DELIVERED`; `CANCELLED` according to business rules. Every transition is stored in `OrderStatusHistory`. Customers cannot set status.

## Cancellation
Within 24h and before shipped: customer can self-cancel; record reason/actor, restore stock, audit/status history, refund flow when applicable.
After 24h or shipped: support request; order unchanged until admin decision. Persist `SupportRequest`.

## Discounts
Percentage or flat amount, optional start/end, one active discount. Effective price computed server-side at read/cart/checkout time. Discount changes audit logged.

## Reports
Orders, cancellations, summary metrics, CSV export, audit trail. Admin-only and report retrieval itself is audit logged.

## Configurable security settings
Defaults: access token 15m; customer refresh 7d; remember-me 30d; admin refresh 8h; absolute cap 30d; idle timeout 14d; OTP 5m; OTP attempts 5; login rate limit 5/min/IP. Settings stored in `SystemSetting`, cached, min/max validated, audit logged. Existing tokens are not retroactively changed.
