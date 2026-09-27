# Phase 15: Infrastructure & CI/CD

## Completed

- Added a production-ready multi-stage Docker build for the Spring Boot backend
- Added Docker Compose support for the application service alongside PostgreSQL, Redis, and OpenSearch
- Added CI workflow for Maven test + package validation on pull requests and main-branch pushes
- Added release workflow for building and publishing the backend image to GitHub Container Registry
- Added Terraform skeleton for PostgreSQL, Redis, OpenSearch, and ECS deployment scaffolding
- Added deployment documentation and environment variable conventions for application and infrastructure configuration

## Files Added

- [Dockerfile](../Dockerfile)
- [.dockerignore](../.dockerignore)
- [.github/workflows/ci.yml](../.github/workflows/ci.yml)
- [.github/workflows/release.yml](../.github/workflows/release.yml)
- [terraform/main.tf](../terraform/main.tf)
- [terraform/variables.tf](../terraform/variables.tf)
- [terraform/outputs.tf](../terraform/outputs.tf)

## Notes

- The Dockerfile builds the jar with Maven before creating the runtime image.
- The Docker Compose application service uses the same environment variables as the local database, cache, and search dependencies.
- Terraform is included as an AWS scaffold rather than a full production deployment so the project can evolve with account-specific networking and IAM settings.
- GitHub Actions validates the Java build on PRs and packages the application for release publication.

## Verification

The project was packaged successfully with Maven after Phase 15 scaffolding was added.
