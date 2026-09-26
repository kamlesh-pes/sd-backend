# Phase 1: Foundation Setup

## Overview
This document describes the local development setup for the Sahastra Digital Backend.

## Prerequisites
- Java 21 (JDK 21 or later)
- Maven 3.8+
- Docker & Docker Compose
- Git

## Local Development Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd sd-backend
```

### 2. Set Up Environment Variables
```bash
cp .env.example .env
# Edit .env with your local settings if needed
```

### 3. Start Infrastructure Services
```bash
# Start all services (PostgreSQL, Redis, OpenSearch)
docker-compose up -d

# Verify services are running
docker-compose ps

# Check service health
docker-compose logs postgres redis opensearch
```

### 4. Build the Project
```bash
# Build the project
mvn clean package

# Skip tests if you want to build faster
mvn clean package -DskipTests
```

### 5. Run the Application
```bash
# Option 1: Using Spring Boot Maven plugin
mvn spring-boot:run

# Option 2: Using Java command
java -jar target/sd-backend-1.0.0-SNAPSHOT.jar

# Option 3: Using IDE (IntelliJ IDEA / Eclipse)
- Open SahastraBackendApplication.java
- Run as Java Application (right-click -> Run)
```

The application should start on `http://localhost:8080`

### 6. Verify Health Checks
```bash
# Test basic health check
curl http://localhost:8080/api/v1/health/ping

# Expected response:
# {
#   "success": true,
#   "data": {
#     "status": "UP",
#     "service": "sahastra-backend",
#     "message": "Service is running"
#   },
#   "message": "Service is healthy"
# }
```

## Access Services

### PostgreSQL
- **Connection String:** postgresql://sahastra_user:sahastra_password@localhost:5432/sahastra_db
- **pgAdmin:** http://localhost:5050 (Optional - start with `docker-compose --profile dev up`)
  - Email: admin@sahastra.local
  - Password: admin (from .env)

### Redis
- **Connection:** localhost:6379
- **Redis Commander:** http://localhost:8081 (Optional - start with `docker-compose --profile dev up`)

### OpenSearch
- **Connection:** http://localhost:9200
- **Cluster Health:** http://localhost:9200/_cluster/health

### Spring Boot Actuator
- **Health:** http://localhost:8080/actuator/health
- **Metrics:** http://localhost:8080/actuator/metrics
- **Info:** http://localhost:8080/actuator/info

## Useful Maven Commands

```bash
# Run tests
mvn test

# Run specific test class
mvn test -Dtest=HealthControllerTest

# Run integration tests
mvn verify

# Check for security vulnerabilities (OWASP)
mvn org.owasp:dependency-check-maven:check

# Clean build
mvn clean

# Build without running tests
mvn package -DskipTests

# View dependency tree
mvn dependency:tree
```

## Troubleshooting

### PostgreSQL Connection Issues
```bash
# Check if PostgreSQL is running
docker-compose ps postgres

# View PostgreSQL logs
docker-compose logs postgres

# Restart PostgreSQL
docker-compose restart postgres
```

### Redis Connection Issues
```bash
# Check if Redis is running
docker-compose ps redis

# Test Redis connection
redis-cli -h localhost -p 6379 ping

# View Redis logs
docker-compose logs redis
```

### OpenSearch Connection Issues
```bash
# Check OpenSearch status
curl http://localhost:9200/_cluster/health

# View OpenSearch logs
docker-compose logs opensearch
```

### Application Won't Start
1. Check if all services are running: `docker-compose ps`
2. Check application logs: `tail -f logs/application.log`
3. Verify port 8080 is not in use: `lsof -i :8080`
4. Check database migrations: Look in `src/main/resources/db/migration/`

## Stopping Services

```bash
# Stop all services
docker-compose down

# Stop and remove volumes (WARNING: deletes data)
docker-compose down -v

# View logs while running
docker-compose logs -f

# View specific service logs
docker-compose logs -f postgres
docker-compose logs -f redis
docker-compose logs -f opensearch
```

## Database Migrations

Flyway is configured to automatically run migrations on startup. Migration files are located in:
`src/main/resources/db/migration/`

Naming convention: `V{version}__{description}.sql` (e.g., `V1__initial_schema.sql`)

To add a new migration:
1. Create a new SQL file in `src/main/resources/db/migration/`
2. Follow naming convention: `V{next_version}__{description}.sql`
3. Write your SQL statements
4. Restart the application (migrations run automatically)

## Code Quality

### Run All Tests
```bash
mvn clean test
```

### Run Tests with Coverage
```bash
mvn clean test jacoco:report
# Report available at: target/site/jacoco/index.html
```

### Check Code Style
```bash
# Using Checkstyle (if configured)
mvn checkstyle:check
```

## IDE Setup

### IntelliJ IDEA
1. Open Project: File -> Open -> Select `sd-backend` folder
2. Configure JDK: File -> Project Structure -> SDKs -> Add JDK 21
3. Enable Annotation Processing: File -> Settings -> Build, Execution, Deployment -> Compiler -> Annotation Processors -> Enable
4. Install Lombok Plugin: File -> Settings -> Plugins -> Search "Lombok" -> Install

### VS Code
1. Install Extensions:
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - REST Client (for API testing)
2. Configure JDK: Open Command Palette (Cmd+Shift+P) -> Java: Configure Runtime
3. Open Integrated Terminal: View -> Terminal

## Useful Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Redis Documentation](https://redis.io/documentation)
- [OpenSearch Documentation](https://opensearch.org/docs/)
- [Docker Documentation](https://docs.docker.com/)
- [Maven Documentation](https://maven.apache.org/guides/)

## Next Steps

After setting up Phase 1 locally:
1. Read the implementation plan: `docs/implementation-plan.md`
2. Review API specifications: `docs/api-spec.md`
3. Check data model: `docs/data-model.md`
4. Start Phase 2: Authentication & Authorization
