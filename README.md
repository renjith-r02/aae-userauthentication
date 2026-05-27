# AuthService2 - Enterprise Authentication Service
## Overview
Production-grade authentication service with JWT, RBAC, token rotation, and comprehensive security features.
## Features
- ✅ User Registration with password policy enforcement (AUTH-FR-001)
- ✅ JWT-based Authentication with RS256 signing (AUTH-FR-002)
- ✅ Token Validation with blacklist support (AUTH-FR-003)
- ✅ Refresh Token Rotation with repl- ✅ Refresh Token Rotation with repl- ✅ Refresh Token Rotation with repler Profile Management (AUTH-FR-006)
- ✅ Role-Based Access Control (RBAC) (AUTH-FR-007)
- ✅ Rate Limiting (5 login attempts/15 min)
- ✅ Audit Logging
- ✅ PostgreSQL Database
- ✅ Redis Caching
## Tech Stack
- Java 17
- Spring Boot 3.2.5
- PostgreSQL 14+
- Redis 7+
- JWT (RS256)
- Flyway Migrations
- JUnit 5 & Testcontainers
- JMeter for load testing
- Kubernetes ready
## Quick Start
### Prerequisites
- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- PostgreSQL 14+
- Redis 7+
### Local Development Setup
1. **Start Dependencies**
```bash
docker-compose up -d
```
2. **Build Application**
```bash
mvn clean install
```
3. **Run Application**
```bash
mvn spring-boot:run
```
4. **Access**
- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Actuator: http://localhost:8080/actuator/health
### Running Tests
**Unit Tests**
```bash
mvn test
```
**Integration Tests**
```bash
mvn verify
```
**Load Tests (JMeter)**
```bash
jmeter -n -t jmeter/AuthService-LoadTest.jmx -l results.jtl
```
### Kubernetes Deployment
```bash
kubectl apply -f k8s/
```
## API Endpoints
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/v1/auth/register` | POST | Register new user |
| `/api/v1/auth/login` | POST | Authenticate user |
| `/api/v1/auth/token/validate` | POST | Validate JWT token |
| `/api/v1/auth/token/refresh` | POST | Refresh access token |
| `/api/v1/auth/logout` | POST | Logout user |
| `/api/v1/auth/me` | GET | Get current user |
| `/actuator/health` | GET | Health check |
## Configuration
Key environment variables:
- `DB_URL` - PostgreSQL connection U- `- `DB_USERNAME` - Database username
- `DB_PASSWORD` - Database password
- `REDIS_HOST` - Redis host
- `REDIS_PASSWORD` - Redis password
- `JWT_ISSUER` - JWT issuer
- `JWT_ACCESS_TOKEN_EXPIRY` - Access token expiry (default: 15m)
- `JWT_REFRESH_TOKEN_EXPIRY` - Refresh token expiry (default: 30d)
## Security Features
- bcrypt password hashing (strength: 12)
- JWT RS256 asymmetric signing
- Refresh token rotation
- Replay attack detection
- Rate limiting (5 attempts/15min)
- Redis- Redis- Redislacklist
- RBAC authorization
- Audit logging
- Input validation (OWASP)
## Architecture
See `.github/docs/architecture/` for:
- Component diagram
- Sequence diagrams
- Class diagram
- Deployment diagram
- API contracts
## Performance Targets
| Operation | Target | Status |
|-----------|--------|--------|
| Login | < 300ms (p99) | ✅ |
| Token Validation | < 50ms (p99) | ✅ |
| Token Refresh | < 200ms (p99) | ✅ |
| Availability | 99.9% | ✅ |
## License
Apache 2.0
