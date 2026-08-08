# Backend Data Model

## User
UUID, encrypted email/mobile/full name, HMAC blind indexes, password hash, verification flags, role, timestamps. At least one of email_hash/mobile_hash is required.

## OtpChallenge
UUID, mobile_hash, otp_hash, purpose, expires_at, attempt_count, consumed, created_at. Never store raw OTP/mobile.

## Address
UUID, user_id, encrypted address lines/postal code, city/state/country, default flag.

## Category
id, name, slug, parent_id.

## Product
UUID, unique SKU, name, description, category, brand, base price, discount fields, stock, JSONB attributes, images, active flag, optimistic-lock version, timestamps.

## ProductDiscountHistory
Product, actor, old/new discount values and windows, timestamp.

## Cart/CartItem
Cart belongs to user or guest session; item has product, quantity, price snapshot.

## Order
UUID, user, status, shipping address, subtotal/tax/shipping/total, unique idempotency key, cancellation fields, timestamps.

## OrderStatusHistory
Order, from/to status, actor, reason, timestamp.

## SupportRequest
Order, user, message, status, timestamps.

## OrderItem
Order, product, product-name snapshot, unit-price snapshot, quantity, line total.

## AuditLog
Actor, action, entity type/id, JSON payload, timestamp.

## SystemSetting
Key/value, min/max, description, updater, timestamp.

## Persistence rules
Money is `DECIMAL(10,2)` / `BigDecimal`, never float/double. Explicit rounding. Effective discount price is computed, not stored as an editable sale price. Use DB constraints and optimistic locking for stock.
