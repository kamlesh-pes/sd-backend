# Troubleshooting Guide

## Application does not start

- Check environment variables for database and service connectivity.
- Confirm PostgreSQL, Redis, and OpenSearch are reachable.
- Review startup logs for migrations, security config, or bean creation issues.

## Authentication failures

- Verify JWT secret and refresh token configuration.
- Confirm the request contains a valid bearer token.
- Review expired, revoked, or stale-token scenarios in the security logs.

## Data issues

- Validate DB connectivity and migration status.
- Review transaction boundaries for stock and order workflows.
- Check whether stale product or cart state has been cached incorrectly.

## Search issues

- Confirm OpenSearch is reachable and the product index is healthy.
- Validate the product indexing pipeline and index mappings.
- Review query errors and payload size limits.

## Performance problems

- Review p95 latency and error rate metrics.
- Inspect slow queries and DB connection pool usage.
- Validate Redis memory usage and cache hit rate.
- Check for indexing lag or large payload serialization.
