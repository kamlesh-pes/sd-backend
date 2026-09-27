# Monitoring Runbook

## Service health checks

1. Confirm the application is healthy via `/actuator/health`.
2. Confirm readiness via `/actuator/health/readiness`.
3. Check `logs/application.log` or the cloud log stream for recent exceptions.

## High latency

- Review p95 and p99 latency in the CloudWatch dashboard.
- Check PostgreSQL slow queries and lock waits.
- Check cache hit rate and Redis memory usage.
- Review OpenSearch query latency and index health.

## High error rate

- Check whether the issue is isolated to a single endpoint or all endpoints.
- Review permission failures, validation errors, and database exceptions in logs.
- Validate that the rate-limit filter and Redis dependency are available.
- If the issue is payment-related, confirm provider availability and webhook processing.

## Database exhaustion

- Inspect connection pool metrics in RDS and application logs.
- Check for long-running transactions and blocked queries.
- Validate that the application is not leaking connections or holding transactions open.

## Redis issues

- Validate network reachability and Redis auth configuration.
- Confirm rate-limit counters and cache keys are being updated.
- If Redis is unavailable, the app should fail open for non-critical flows, but alerting should still escalate.

## OpenSearch lag

- Confirm indexing jobs are running and the index is healthy.
- Check cluster health and shard allocation.
- Validate that product update events are reaching the indexing pipeline.

## Escalation

- SNS alert topic sends operational notifications to the configured email address.
- If a critical outage occurs, escalate to the on-call engineer and pull in the DB or search owner based on the failing subsystem.
- Record service restoration time, incident summary, and mitigation steps in the incident log.
