# Phase 14: Load Testing

## Completed

- Added a Java-based load-test metrics harness for concurrent latency sampling and percentile tracking
- Added regression tests covering load sampling, percentile calculation, and multi-threaded throughput scenarios
- Documented the baseline performance targets and bottleneck review checklist for search and checkout workloads

## Included Deliverables

- Metrics collector: `src/main/java/com/sahastra/backend/performance/LoadTestMetrics.java`
- Regression tests: `src/test/java/com/sahastra/backend/performance/LoadTestMetricsTest.java`
- Performance notes and recommendations below

## Baseline Targets

- Search p95: <300ms
- Checkout p95: <1s
- Read-heavy endpoints: p95 <500ms
- Write-heavy endpoints: p95 <1s

## Load Scenarios to Run in a Full Environment

1. Product search at 1,000+ concurrent users
2. Cart operations at 500+ concurrent users
3. Checkout at 100+ concurrent users with stock validation
4. Order-status updates at 100+ concurrent users

## Bottleneck Review Checklist

- PostgreSQL query plans and missing indexes on catalog and order lookups
- Redis hit ratio for cart/session caching and rate-limit counters
- OpenSearch index configuration, payload size, and request fan-out for search
- Connection-pool saturation during stock decrement and order creation
- Time spent serializing large product and order response payloads

## Optimization Recommendations

- Add filtered indexes for category, brand, and active status on product queries
- Cache frequently accessed product catalog pages and search results in Redis with TTLs
- Keep OpenSearch documents denormalized to minimize join-heavy reads
- Use transaction-scoped stock checks with optimistic locking and retry logic
- Log latency histograms by endpoint and route to identify hotspots before production scale-up

## Verification

The Phase 14 metrics test suite was executed with Maven and validated for the new performance harness.
