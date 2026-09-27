# Deployment Guide

## Local deployment

Use Docker Compose for the supporting services and run the backend locally with Maven.

```bash
docker compose up -d postgres redis opensearch
mvn spring-boot:run
```

Check health:

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/api/v1/health/ping
```

## Staging deployment

1. Build and test in CI.
2. Publish the Docker image to the registry.
3. Deploy the container to the staging environment using the Terraform scaffolding or the target orchestrator.
4. Set runtime variables from the environment vault.
5. Run migration-based schema validation before traffic is routed to the app.

## Production deployment

1. Use the GitHub Actions release workflow to publish the Docker image.
2. Use secret managers to inject DB, Redis, and OpenSearch credentials.
3. Enable HTTPS and restrict network ingress.
4. Run the app behind a load balancer or ingress controller.
5. Confirm health checks and readiness before promoting traffic.

## Environment checklist

- DB host/user/password valid
- Redis host/password valid
- OpenSearch host/credentials valid
- App port and TLS config correct
- Observability and log stream connected
- Rollback strategy prepared
