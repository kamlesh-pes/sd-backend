# Phase 16: Monitoring & Alerting

## Completed

- Added CloudWatch dashboard and alarm scaffolding for API latency, error rate, database connections, Redis memory, and OpenSearch indexing lag
- Added an SNS alert topic for operational escalation
- Added a structured runbook for common production incidents and operational checks
- Documented the monitoring configuration and alert thresholds for the deployed backend

## Included Files

- [terraform/monitoring.tf](../terraform/monitoring.tf)
- [docs/monitoring-runbook.md](monitoring-runbook.md)

## Target Thresholds

- API latency (p95): <1000ms
- Error rate: <1% for 5xx responses
- Database connection pool: warn before 80% saturation
- Redis memory usage: <80%
- OpenSearch indexing lag: <300 seconds (5 minutes)

## Operational Notes

- App metrics are expected to publish under the `SahastraBackend` namespace through Micrometer and Spring Boot Actuator.
- Database, Redis, and OpenSearch alarms are wired to SNS so downstream paging or email escalation can be added without changing the application code.
- Log retention is configured for 30 days in the backend CloudWatch log stream to keep diagnostics available for incident review.

## Verification

The backend package still compiles successfully after the monitoring configuration was added.
