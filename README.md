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

---

## GitHub Configuration & Automation (`.github/`)

The `.github/` directory contains all enterprise governance, AI agent definitions, coding instructions, architecture documentation, and requirements for this project.

```
.github/
├── copilot-instructions.md          # GitHub Copilot enterprise coding standards
├── agents/                          # AI agent definitions (6 agents)
│   ├── architecture-planning.agent.md
│   ├── diagram-generation.agent.md
│   ├── repository-analysis.agent.md
│   ├── requirements-analysis.agent.md
│   ├── validation-governance.agent.md
│   └── workspace-persistence.agent.md
├── instructions/                    # Coding & design instruction files (4 files)
│   ├── api.instructions.md
│   ├── architecture.instructions.md
│   ├── security.instructions.md
│   └── validation.instructions.md
└── docs/
    ├── architecture/                # Architecture documentation (6 files)
    │   ├── api-contracts.md
    │   ├── class-diagram.md
    │   ├── component-diagram.md
    │   ├── deployment-diagram.md
    │   ├── sequence-diagram.md
    │   └── traceability-matrix.md
    └── requirements/                # Requirements documentation (1 file)
        └── authentication_service_requirements.md
```

---

### 🤖 AI Agents (`.github/agents/`)

Six specialized AI agents are defined to assist with enterprise development tasks:

| Agent | File | Responsibilities |
|-------|------|-----------------|
| **Architecture Planning** | `architecture-planning.agent.md` | Define service boundaries, select architecture patterns, define deployment topology and resilience strategies. Outputs: component, sequence, class, deployment diagrams, API contracts, traceability matrix. |
| **Diagram Generation** | `diagram-generation.agent.md` | Generate component, deployment, sequence, and data flow diagrams. Includes security zones and trust boundaries. |
| **Repository Analysis** | `repository-analysis.agent.md` | Analyze repository structure, detect anti-patterns, validate dependency management and architecture consistency. Outputs: assessment reports, governance recommendations. |
| **Requirements Analysis** | `requirements-analysis.agent.md` | Analyze business and non-functional requirements, detect security requirements, define traceability mappings. Outputs: functional requirements, risk analysis, dependency analysis. |
| **Validation Governance** | `validation-governance.agent.md` | Validate architecture compliance, security controls, API standards, and CI/CD quality gates. Outputs: compliance reports, security findings, governance recommendations. |
| **Workspace Persistence** | `workspace-persistence.agent.md` | Preserve architecture context, maintain design decisions, synchronize diagrams and APIs, track architecture drift. Governance: detect inconsistencies, track dependency changes. |

---

### 📋 Coding Instructions (`.github/instructions/`)

Four instruction files define enterprise standards for all code generation and design:

#### `api.instructions.md` — API Design Standards
- **API Standards:** OpenAPI 3.x, versioned APIs, JSON payloads, consistent error responses
- **Security:** JWT authentication required, RBAC enforcement mandatory, input validation, rate limiting
- **Error Response Format:**
  ```json
  {
    "status": 400,
    "error": "Bad Request",
    "message": "Validation failed"
  }
  ```
- **Observability:** Correlation IDs, request tracing, audit logging for sensitive APIs

#### `architecture.instructions.md` — Enterprise Architecture Standards
- **Principles:** Independently deployable services, stateless design, no tight coupling, async communication, security at every layer
- **Preferred Patterns:** Clean Architecture, Hexagonal Architecture, CQRS, Saga orchestration, API Gateway pattern
- **Cloud-Native:** Kubernetes-ready, Infrastructure as Code, horizontal scalability, immutable deployments
- **Diagram Requirements:** Service boundaries, security zones, event flows, deployment topology

#### `security.instructions.md` — Enterprise Security Standards
- **Principles:** Zero Trust Architecture, Least Privilege Access, Defense in Depth, Secure by Default
- **OWASP Top 10 Controls:**
  - Broken Access Control → Enforce RBAC, deny by default
  - Cryptographic Failures → TLS 1.2+, bcrypt or Argon2
  - Injection → Parameterized queries, input validation
  - Security Misconfiguration → Harden infrastructure, disable unnecessary services
  - Vulnerable Components → Continuous dependency scanning
  - Authentication Failures → MFA, token expiration & rotation
  - Logging & Monitoring → Centralized audit logs, monitor auth events
- **Container Security:** Minimal base images, non-root containers, continuous image scanning
- **Kubernetes Security:** Network policies, no privileged containers, secure secrets management

#### `validation.instructions.md` — Validation & Governance Standards
- **Validation Areas:** Architecture consistency, security compliance, API standards, code quality, deployment governance
- **Mandatory Quality Gates:** SonarQube, SAST scan, dependency scan, container scan, unit test coverage
- **Traceability:** Requirements ↔ Architecture ↔ APIs ↔ Source Code ↔ Tests

---

### 🧭 GitHub Copilot Instructions (`.github/copilot-instructions.md`)

Defines enterprise-grade AI-assisted coding standards:

| Category | Standards |
|----------|-----------|
| **Core Principles** | Security by Design, API-First Development, Cloud-Native Architecture, Zero Trust Security, Observability by Default, Automated Validation and Governance |
| **Mandatory Standards** | OWASP Top 10, versioned APIs, no hardcoded secrets, JWT + RBAC, structured logging, health checks |
| **Security Requirements** | bcrypt/Argon2 for passwords, rate limiting, replay attack prevention, encryption in transit and at rest |
| **Definition of Done** | Security validation passes, architecture docs updated, APIs documented, tests passing |

---

### 📐 Architecture Documentation (`.github/docs/architecture/`)

| File | Description |
|------|-------------|
| `component-diagram.md` | High-level component interactions and service boundaries |
| `sequence-diagram.md` | Authentication flow sequence diagrams (login, token refresh, logout) |
| `class-diagram.md` | Domain model class relationships |
| `deployment-diagram.md` | Kubernetes and infrastructure deployment topology |
| `api-contracts.md` | OpenAPI 3.0 / Swagger API contracts for all endpoints |
| `traceability-matrix.md` | Requirement-to-implementation traceability mapping |

### 📄 Requirements (`.github/docs/requirements/`)

| File | Description |
|------|-------------|
| `authentication_service_requirements.md` | Full functional and non-functional requirements specification (AUTH-FR-001 through AUTH-FR-007) |
## Performance Targets
| Operation | Target | Status |
|-----------|--------|--------|
| Login | < 300ms (p99) | ✅ |
| Token Validation | < 50ms (p99) | ✅ |
| Token Refresh | < 200ms (p99) | ✅ |
| Availability | 99.9% | ✅ |
## License
Apache 2.0
