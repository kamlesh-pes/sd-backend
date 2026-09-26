# Phase 10: Reporting & Export

## Completed

- Added admin-only paginated order reports with date-range and status filters
- Added cancellation reports sourced from order status history, including reason and actor data
- Added summary metrics for order count, cancellations, revenue, units sold, and average order value
- Excluded cancelled orders from revenue, unit, and average-value calculations
- Added CSV export for filtered order reports with safe CSV escaping
- Added paginated audit-trail reports over the existing audit log
- Added server-side date-range validation and default 30-day report windows
- Added audit logging for every report view and CSV export
- Added database query methods for indexed date/status report access
- Added unit tests for metrics, CSV output, audit access, and invalid ranges

## Endpoints

- `GET /api/v1/admin/reports/orders`
- `GET /api/v1/admin/reports/orders.csv`
- `GET /api/v1/admin/reports/cancellations`
- `GET /api/v1/admin/reports/metrics`
- `GET /api/v1/admin/reports/audit`

All endpoints require `ROLE_ADMIN` through the existing `/api/v1/admin/**` security rule.

## Query Parameters

- `from`: inclusive ISO-8601 timestamp; defaults to 30 days before the current server time
- `to`: exclusive ISO-8601 timestamp; defaults to the current server time
- `status`: optional order status filter for order reports and CSV export
- `page`: zero-based page number for paginated reports
- `size`: page size, limited by the existing 100-item controller validation

## Files Added

- [src/main/java/com/sahastra/backend/service/ReportService.java](src/main/java/com/sahastra/backend/service/ReportService.java)
- [src/main/java/com/sahastra/backend/api/controller/AdminReportController.java](src/main/java/com/sahastra/backend/api/controller/AdminReportController.java)
- [src/test/java/com/sahastra/backend/service/ReportServiceTest.java](src/test/java/com/sahastra/backend/service/ReportServiceTest.java)

## Verification

The focused Maven command was started from the Maven installation directory:

```powershell
Set-Location C:\Users\Administrator\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin
.\mvn.cmd -f C:\Users\Administrator\sd-repo\sd-backend\pom.xml -Dtest=ReportServiceTest test
```

Main and test compilation completed successfully. `ReportServiceTest` passed with 3 tests, 0 failures, 0 errors, and 0 skipped.
