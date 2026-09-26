# Phase 5: Cart Management

## Completed

- Cart and cart-item persistence for authenticated and guest-session carts
- Add, update, and remove item operations with server-side validation
- Stock checks before item insertion and quantity changes
- Server-side subtotal and total recalculation for each cart response
- Guest cart merge into the authenticated user cart when a user signs in
- Guest cart expiration cleanup for stale anonymous carts
- Request validation and business exception handling for invalid cart operations
- Regression coverage for oversize stock validation and guest-cart merge behavior

## Endpoints

- `GET /api/v1/cart`
- `POST /api/v1/cart/items`
- `PUT /api/v1/cart/items/{productId}`
- `DELETE /api/v1/cart/items/{productId}`

Guest sessions are handled via the `X-Guest-Session` request header when the user is not authenticated.

## Notes

- Cart totals are recalculated on the server using the current effective product price.
- Quantity updates respect available stock and reject invalid requests.
- Guest carts are merged into the user cart to preserve the customer experience across login.
- Old guest carts are eligible for cleanup via the cart cleanup logic for stale anonymous sessions.

## Verification

Run the focused cart test suite:

```bash
mvn -q -Dtest=CartServiceTest test
```

Result: passed successfully with no failing tests.

## Next Phase

Phase 5 is complete and the backend is ready to continue with Phase 6: Checkout & Order Creation.
