# Phase 9: Discount Management

## Completed

- Added admin-only discount CRUD endpoints for products
- Reused the product's existing single discount fields to enforce one discount per product
- Added server-side validation for percentage, fixed amount, positive values, base-price limits, and date windows
- Preserved effective price calculation at product read, cart, and checkout time
- Added discount active-state calculation based on server time, including expired and not-yet-started discounts
- Added audit logging for discount creation, updates, and deletion
- Added regression tests for conflicting discounts, invalid percentages, invalid windows, expiration, and audit creation

## Endpoints

- `POST /api/v1/admin/products/{productId}/discount`
- `GET /api/v1/admin/products/{productId}/discount`
- `PUT /api/v1/admin/products/{productId}/discount`
- `DELETE /api/v1/admin/products/{productId}/discount`

All endpoints require `ROLE_ADMIN` through the existing `/api/v1/admin/**` security rule.

## Rules

- Percentage discounts must be between greater than zero and 100 percent.
- Fixed discounts must be positive and cannot exceed the product base price.
- An end date must be after the start date.
- A product cannot receive a second discount while one is already configured.
- Effective prices are calculated from server-side product state and current server time.
- Discount changes are written to the existing audit log with the admin actor and correlation ID.

## Files Added

- [src/main/java/com/sahastra/backend/api/dto/DiscountRequest.java](src/main/java/com/sahastra/backend/api/dto/DiscountRequest.java)
- [src/main/java/com/sahastra/backend/api/dto/DiscountResponse.java](src/main/java/com/sahastra/backend/api/dto/DiscountResponse.java)
- [src/main/java/com/sahastra/backend/service/DiscountService.java](src/main/java/com/sahastra/backend/service/DiscountService.java)
- [src/main/java/com/sahastra/backend/api/controller/AdminDiscountController.java](src/main/java/com/sahastra/backend/api/controller/AdminDiscountController.java)
- [src/test/java/com/sahastra/backend/service/DiscountServiceTest.java](src/test/java/com/sahastra/backend/service/DiscountServiceTest.java)

## Compatibility Fix

- Made `OrderItemResponse` public in [src/main/java/com/sahastra/backend/api/dto/OrderItemResponse.java](src/main/java/com/sahastra/backend/api/dto/OrderItemResponse.java) so the existing order service compiles across package boundaries.

## Verification

The focused Maven test command passed after the compatibility fix:

```powershell
Set-Location C:\Users\Administrator\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin
.\mvn.cmd -f C:\Users\Administrator\sd-repo\sd-backend\pom.xml -Dtest=DiscountServiceTest test
```

The build completed successfully after compilation. Result: `DiscountServiceTest` passed with 5 tests, 0 failures, 0 errors, and 0 skipped.
