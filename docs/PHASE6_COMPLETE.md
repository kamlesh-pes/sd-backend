# Phase 6: Checkout & Order Creation

## Completed

- Checkout endpoint for authenticated users
- Idempotency-key enforcement for duplicate order submissions
- Order entity, item snapshot, status history, and idempotency persistence models
- Server-side subtotal, tax, shipping, and total calculation
- Stock decrement during order creation
- Initial order status set to `PENDING`
- Order status history entry recorded at creation time
- Cart clearing after successful order creation
- Regression tests covering successful order creation and duplicate idempotency rejection

## Endpoints

- `POST /api/v1/orders`

Headers:
- `Idempotency-Key: <unique-value>`

## Notes

- Orders are created only for authenticated users.
- Order totals are calculated on the server, not trusted from client input.
- Stock is decremented only after cart validation passes.
- Duplicate idempotency keys for the same user are rejected to preserve order safety.

## Verification

Run the focused order test suite:

```bash
mvn.cmd -q -Dtest=OrderServiceTest test
```

Result: passed successfully with no failing tests.

## Next Phase

Phase 6 is complete and the backend is ready to continue with Phase 7: Payment Abstraction.
