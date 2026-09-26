# Phase 8: Order Lifecycle & Cancellation

## Completed

- Added server-side order cancellation eligibility checks based on the order's created timestamp
- Enabled self-cancel behavior for eligible orders within the 24-hour window
- Prevented direct client-driven status changes by enforcing status transitions in backend logic only
- Restored product stock when a qualifying customer cancellation is approved
- Recorded status history transitions in the order status audit trail
- Created a support-request flow for late or shipped orders where self-cancel is not allowed
- Added support request persistence and status enum to track review lifecycle
- Added regression coverage for cancellation within 24 hours and support-request creation for late/shipped orders

## Scope

This phase covers the backend order lifecycle rules and supports the requirement that cancellation eligibility is calculated from the server-authoritative order timestamp, never from client input.

## Key Rules Implemented

- Customer self-cancel is allowed only when:
  - the order belongs to the authenticated user
  - the order is not already cancelled
  - the order status is not shipped/out-for-delivery/delivered
  - the order was created within the last 24 hours
- Orders outside the cancellation window or already shipped must create a support request instead of being auto-cancelled
- Approved cancellations restore reserved inventory to the product stock level
- Each transition is stored in the status history table for auditability

## Files Updated

- [src/main/java/com/sahastra/backend/service/OrderService.java](src/main/java/com/sahastra/backend/service/OrderService.java)
- [src/main/java/com/sahastra/backend/domain/entity/SupportRequest.java](src/main/java/com/sahastra/backend/domain/entity/SupportRequest.java)
- [src/main/java/com/sahastra/backend/domain/repository/SupportRequestRepository.java](src/main/java/com/sahastra/backend/domain/repository/SupportRequestRepository.java)
- [src/main/java/com/sahastra/backend/domain/enums/SupportRequestStatus.java](src/main/java/com/sahastra/backend/domain/enums/SupportRequestStatus.java)
- [src/main/resources/db/migration/V7__support_requests.sql](src/main/resources/db/migration/V7__support_requests.sql)
- [src/test/java/com/sahastra/backend/service/OrderServiceTest.java](src/test/java/com/sahastra/backend/service/OrderServiceTest.java)

## Verification Status

The implementation has been added and the targeted lifecycle regression tests were invoked with Maven. The environment is still reporting a build hang while continuing through the Java compilation stage, so the final test pass/fail result is not yet confirmed in this session. The code paths for cancellation and support requests are implemented and aligned with the project rules, but the terminal verification remains incomplete.

## Next Step

Retry the focused Maven verification once the build environment is stable. The expected validation command is:

```bash
C:\Users\Administrator\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd -Dtest=OrderServiceTest test
```
