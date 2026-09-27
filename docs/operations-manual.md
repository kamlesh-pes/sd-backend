# Operations Manual

## Backup and restore

- PostgreSQL: take regular logical or physical snapshots using the platform’s backup tooling.
- Redis: back up persistent data if enabled; ensure the cache can be rebuilt from source data.
- OpenSearch: snapshot indices to a durable repository or equivalent backup target.

## Capacity and scaling

- Scale the API service horizontally behind a load balancer.
- Tune PostgreSQL connection limits and pool sizing.
- Monitor Redis memory and OpenSearch cluster health.
- Keep a warm rollback image in the container registry.

## Maintenance

- Apply migration scripts in a controlled release window.
- Verify health checks after release.
- Review logs, alerts, and metrics before and after traffic shifts.

## Incident response

1. Identify failing subsystem: database, Redis, OpenSearch, or app.
2. Check health endpoints and recent logs.
3. Verify external dependency status and throttling.
4. Roll back or isolate the bad release if needed.
5. Capture root cause and update runbook procedures.
