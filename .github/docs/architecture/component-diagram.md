# Component Diagram - AuthService2

## Overview
This diagram illustrates the main components of the AuthService2 system based on the authentication service requirements (AUTH-FR-001 through AUTH-FR-007), their responsibilities, and interactions within security zones.

---

## Component Diagram

```mermaid
graph TB
    subgraph "DMZ Zone - Trust Boundary 1"
        WAF[AWS WAF<br/>OWASP Rules<br/>DDoS Protection]
        ALB[Application Load Balancer<br/>TLS Termination<br/>Rate Limiting]
    end
    
    subgraph "Application Zone - Trust Boundary 2"
        subgraph "API Layer"
            API[API Gateway<br/>v1 Routing<br/>CORS Handler]
        end
        
        subgraph "Controller Layer"
            AUTH_CTRL[Authentication Controller<br/>- POST /auth/register<br/>- POST /auth/login<br/>- POST /auth/logout<br/>- POST /auth/token/validate<br/>- POST /auth/token/refresh]
            USER_CTRL[User Controller<br/>- GET /auth/me]
            HEALTH_CTRL[Health Controller<br/>- /actuator/health<br/>- /actuator/health/liveness<br/>- /actuator/health/readiness]
        end
        
        subgraph "Service Layer"
            AUTH_SVC[Authentication Service<br/>AUTH-FR-002: Login<br/>AUTH-FR-003: Token Validation<br/>AUTH-FR-004: Token Refresh<br/>AUTH-FR-005: Session Invalidation]
            USER_SVC[User Service<br/>AUTH-FR-001: Registration<br/>AUTH-FR-006: User Details]
            TOKEN_SVC[Token Service<br/>JWT Generation<br/>JWT Validation<br/>Token Rotation<br/>Replay Detection]
            RBAC_SVC[RBAC Service<br/>AUTH-FR-007: Role Check<br/>Permission Validation]
        end
        
        subgraph "Security Components"
            PWD_MGR[Password Manager<br/>bcrypt Hashing<br/>Password Policy Validator<br/>Min 12 chars requirement]
            JWT_MGR[JWT Manager<br/>RS256 Signing<br/>Claims Generator<br/>Token Verifier]
            RATE_LIMITER[Rate Limiter<br/>5 login attempts/15min<br/>3 registrations/hour<br/>IP & User-based]
            REPLAY_GUARD[Replay Attack Prevention<br/>Token Family Tracking<br/>Rotation Detection]
            BLACKLIST[Token Blacklist Manager<br/>Redis-based<br/>JTI Tracking]
        end
        
        subgraph "Cross-Cutting Concerns"
            VALIDATOR[Input Validator<br/>Email Format<br/>Password Policy<br/>OWASP Controls]
            AUDIT[Audit Logger<br/>Login Success/Failure<br/>Token Operations<br/>RBAC Denials]
            METRICS[Metrics Collector<br/>Authentication Rates<br/>Token Refresh Metrics<br/>Security Events]
            LOGGER[Structured Logger<br/>JSON Format<br/>No Sensitive Data<br/>Correlation IDs]
        end
    end
    
    subgraph "Data Zone - Trust Boundary 3"
        DB[(PostgreSQL Database<br/>- User Table<br/>- Role Table<br/>- UserRole Table<br/>- Permission Table<br/>- RefreshToken Table<br/>- Session Table<br/>- AuditLog Table<br/>Encrypted at Rest)]
        
        CACHE[(Redis Cache<br/>- Token Blacklist<br/>- Rate Limit Counters<br/>- Login Attempts<br/>- Temporary Lockouts<br/>TLS Enabled)]
        
        SECRETS[AWS Secrets Manager<br/>- JWT Signing Keys<br/>- DB Credentials<br/>- Redis Password<br/>Auto-Rotation<br/>KMS Encrypted)]
    end
    
    subgraph "External Services - Trust Boundary 4"
        EMAIL[Email Service<br/>AWS SES<br/>Verification Emails]
    end
    
    %% Edge to Application
    WAF -->|Filtered Traffic| ALB
    ALB -->|HTTPS| API
    
    %% API to Controllers
    API -->|/auth/*| AUTH_CTRL
    API -->|/auth/me| USER_CTRL
    API -->|/actuator/*| HEALTH_CTRL
    
    %% Controllers to Services
    AUTH_CTRL -->|register()| USER_SVC
    AUTH_CTRL -->|login()| AUTH_SVC
    AUTH_CTRL -->|validateToken()| TOKEN_SVC
    AUTH_CTRL -->|refreshToken()| TOKEN_SVC
    AUTH_CTRL -->|logout()| AUTH_SVC
    USER_CTRL -->|getCurrentUser()| USER_SVC
    
    %% Service Dependencies
    AUTH_SVC -->|generateTokens()| TOKEN_SVC
    AUTH_SVC -->|verifyPassword()| PWD_MGR
    AUTH_SVC -->|checkRateLimit()| RATE_LIMITER
    AUTH_SVC -->|invalidateSession()| BLACKLIST
    
    USER_SVC -->|hashPassword()| PWD_MGR
    USER_SVC -->|getRoles()| RBAC_SVC
    USER_SVC -->|checkPermission()| RBAC_SVC
    
    TOKEN_SVC -->|sign/verify| JWT_MGR
    TOKEN_SVC -->|detectReplay()| REPLAY_GUARD
    TOKEN_SVC -->|checkBlacklist()| BLACKLIST
    
    %% Security Component Dependencies
    JWT_MGR -->|loadKeys()| SECRETS
    REPLAY_GUARD -->|trackFamily()| CACHE
    BLACKLIST -->|store/check| CACHE
    RATE_LIMITER -->|counters| CACHE
    
    %% Validation
    AUTH_CTRL -->|validate()| VALIDATOR
    USER_CTRL -->|validate()| VALIDATOR
    
    %% Data Access
    AUTH_SVC -->|query/update| DB
    USER_SVC -->|query/update| DB
    TOKEN_SVC -->|query/update| DB
    RBAC_SVC -->|query| DB
    
    %% Audit & Logging
    AUTH_SVC -->|logEvent()| AUDIT
    USER_SVC -->|logEvent()| AUDIT
    TOKEN_SVC -->|logEvent()| AUDIT
    RBAC_SVC -->|logDenial()| AUDIT
    
    AUTH_SVC -->|metrics()| METRICS
    TOKEN_SVC -->|metrics()| METRICS
    
    AUTH_CTRL -->|log()| LOGGER
    USER_CTRL -->|log()| LOGGER
    
    %% External Services
    USER_SVC -->|sendEmail()| EMAIL
    
    %% Health Checks
    HEALTH_CTRL -->|check| DB
    HEALTH_CTRL -->|check| CACHE
    
    %% Styling
    classDef dmzZone fill:#ff6b6b,stroke:#c92a2a,stroke-width:3px,color:#fff
    classDef appZone fill:#4ecdc4,stroke:#087f5b,stroke-width:2px,color:#000
    classDef dataZone fill:#ffd93d,stroke:#f08c00,stroke-width:3px,color:#000
    classDef externalZone fill:#95afc0,stroke:#535c68,stroke-width:2px,color:#000
    
    class WAF,ALB dmzZone
    class API,AUTH_CTRL,USER_CTRL,HEALTH_CTRL,AUTH_SVC,USER_SVC,TOKEN_SVC,RBAC_SVC,PWD_MGR,JWT_MGR,RATE_LIMITER,REPLAY_GUARD,BLACKLIST,VALIDATOR,AUDIT,METRICS,LOGGER appZone
    class DB,CACHE,SECRETS dataZone
    class EMAIL externalZone
```

---

## Component Descriptions

### DMZ Zone (Trust Boundary 1)
Components exposed to the public internet with maximum security controls.

#### AWS WAF (Web Application Firewall)
- **Purpose**: First line of defense against attacks
- **Capabilities**:
  - OWASP Top 10 protection rules
  - DDoS mitigation
  - IP reputation filtering
  - Bot detection and blocking
  - Geographic restrictions
- **Requirements**: As per enterprise security standards

#### Application Load Balancer (ALB)
- **Purpose**: Traffic distribution and TLS termination
- **Capabilities**:
  - TLS 1.3 enforcement
  - SSL offloading
  - Health check routing
  - Multi-AZ distribution
  - Connection-level rate limiting

---

### Application Zone (Trust Boundary 2)

#### API Gateway
- **Purpose**: API versioning and routing
- **Capabilities**:
  - URI-based versioning (/api/v1/)
  - CORS policy enforcement
  - Request/response transformation
  - API analytics

#### Authentication Controller
- **Requirements Mapping**:
  - `POST /api/v1/auth/register` → AUTH-FR-001
  - `POST /api/v1/auth/login` → AUTH-FR-002
  - `POST /api/v1/auth/token/validate` → AUTH-FR-003
  - `POST /api/v1/auth/token/refresh` → AUTH-FR-004
  - `POST /api/v1/auth/logout` → AUTH-FR-005
- **Responsibilities**:
  - HTTP request handling
  - Input validation delegation
  - Error response formatting
  - Correlation ID propagation

#### User Controller
- **Requirements Mapping**:
  - `GET /api/v1/auth/me` → AUTH-FR-006
- **Responsibilities**:
  - User profile retrieval
  - RBAC enforcement → AUTH-FR-007
  - Sensitive data filtering

#### Health Controller
- **Endpoints**:
  - `/actuator/health` - Overall health
  - `/actuator/health/liveness` - Kubernetes liveness probe
  - `/actuator/health/readiness` - Kubernetes readiness probe
- **Responsibilities**:
  - Database connectivity check
  - Redis connectivity check
  - Service availability reporting

---

#### Authentication Service
- **Primary Requirement**: AUTH-FR-002 (User Authentication and JWT Issuance)
- **Responsibilities**:
  - User credential verification
  - Account status validation (ACTIVE, LOCKED, DISABLED)
  - Password verification via bcrypt
  - Rate limiting enforcement (5 attempts per 15 minutes)
  - JWT access token generation (15-minute expiry)
  - Refresh token generation (30-day expiry)
  - Session tracking
  - Audit logging of authentication events
- **Security Controls**:
  - Brute-force protection
  - Account lockout after failed attempts
  - Suspicious activity detection

#### User Service
- **Primary Requirements**: AUTH-FR-001 (User Registration), AUTH-FR-006 (User Details)
- **Responsibilities**:
  - User registration with validation
  - Email uniqueness check
  - Password policy enforcement (12+ chars, uppercase, lowercase, number, special char)
  - bcrypt password hashing
  - User profile management
  - Default role assignment (USER)
  - Account status management (ACTIVE, PENDING, LOCKED, DISABLED)
- **Security Controls**:
  - Never store plain-text passwords
  - Never log passwords
  - Duplicate email rejection

#### Token Service
- **Primary Requirements**: AUTH-FR-003 (Token Validation), AUTH-FR-004 (Token Refresh)
- **Responsibilities**:
  - JWT access token generation
  - JWT access token validation
  - Token signature verification (RS256)
  - Token expiration checking
  - Token blacklist verification
  - Refresh token generation and rotation
  - Refresh token validation
  - Token family tracking for replay detection
  - Session correlation
- **Security Controls**:
  - Refresh token rotation on every use
  - Replay attack detection
  - Token reuse prevention
  - Session revocation on suspicious activity

#### RBAC Service
- **Primary Requirement**: AUTH-FR-007 (Role-Based Access Control)
- **Responsibilities**:
  - Role assignment and validation
  - Permission checking
  - User role retrieval
  - Authorization decision making
- **Default Roles**:
  - USER - Standard authenticated user
  - ADMIN - Administrative privileges
  - SERVICE - Service-to-service identity
- **Permissions**:
  - PROFILE_READ - Read own profile
  - USER_READ - Read user records
  - USER_WRITE - Create/update users
  - SESSION_REVOKE - Revoke sessions
  - ROLE_MANAGE - Assign roles

---

### Security Components

#### Password Manager
- **Requirement**: AUTH-FR-001 (Password Policy)
- **Capabilities**:
  - bcrypt hashing with auto-generated salt
  - Password policy validation:
    - Minimum 12 characters
    - At least one uppercase letter
    - At least one lowercase letter
    - At least one number
    - At least one special character
    - Not matching common weak passwords
    - Not containing user email or name
  - Password verification
- **Security**: Never returns plain-text passwords

#### JWT Manager
- **Requirement**: AUTH-FR-002, AUTH-FR-003
- **Capabilities**:
  - RS256 asymmetric signing
  - JWT claims generation (sub, email, roles, permissions, iss, aud, iat, exp, jti)
  - Signature verification
  - Expiration validation
  - Issuer and audience validation
- **Token Structure**:
  ```json
  {
    "sub": "<userId>",
    "email": "<user-email>",
    "roles": ["USER"],
    "permissions": ["PROFILE_READ"],
    "iss": "auth-service",
    "aud": "application-api",
    "iat": 1778670000,
    "exp": 1778670900,
    "jti": "<unique-token-id>"
  }
  ```

#### Rate Limiter
- **Requirement**: AUTH-FR-002 (Rate Limiting)
- **Capabilities**:
  - Per-IP rate limiting
  - Per-user account rate limiting
  - Configurable windows and thresholds
- **Limits**:
  - Login: 5 attempts per 15 minutes
  - Registration: 3 attempts per hour
  - Token refresh: 10 attempts per 5 minutes
  - General API: 100 requests per minute
- **Storage**: Redis-based counters with TTL

#### Replay Attack Prevention
- **Requirement**: AUTH-FR-004 (Refresh Token Replay Detection)
- **Capabilities**:
  - Token family tracking
  - Rotation detection
  - Reuse detection
  - Automatic session revocation on replay
- **Storage**: Redis with token family metadata

#### Token Blacklist Manager
- **Requirement**: AUTH-FR-005 (Session Invalidation)
- **Capabilities**:
  - Access token blacklisting
  - JTI (JWT ID) tracking
  - TTL-based expiration (until original token expiry)
  - Fast lookup using Redis
- **Key Format**: `auth:blacklist:jti:<tokenId>`

---

### Cross-Cutting Concerns

#### Input Validator
- **Requirement**: AUTH-FR-001, OWASP Top 10 Controls
- **Capabilities**:
  - Email format validation
  - Password policy validation
  - XSS prevention
  - SQL injection prevention
  - General input sanitization
  - Field-level error reporting

#### Audit Logger
- **Requirement**: Section 11 (Audit Logging Requirements)
- **Events Logged**:
  - User registration (userId, email, timestamp)
  - Login success (userId, IP, userAgent, timestamp)
  - Login failure (email, IP, reason, timestamp)
  - Token refresh (userId, sessionId, timestamp)
  - Refresh token replay (userId, sessionId, tokenFamilyId, IP)
  - Logout (userId, sessionId, timestamp)
  - Token blacklist (tokenId, userId, expiration)
  - RBAC denial (userId, requiredPermission, endpoint)
- **Security**: Never logs passwords, raw refresh tokens, or full JWTs

#### Metrics Collector
- **Requirement**: Section 6.5 (Observability)
- **Metrics**:
  - Authentication success/failure rates
  - Token refresh counts
  - Token replay detection counts
  - Rate limit violations
  - Session revocations
  - RBAC denials
  - API response times
  - Database query latency

#### Structured Logger
- **Requirement**: Section 6.5 (Observability)
- **Format**: JSON with structured fields
- **Fields**:
  - timestamp
  - level (INFO, WARN, ERROR)
  - correlationId
  - userId (when available)
  - action
  - message
  - details
- **Security**: Sensitive data (passwords, tokens) never logged

---

### Data Zone (Trust Boundary 3)

#### PostgreSQL Database
- **Requirement**: Section 7 (Data Model Requirements)
- **Tables**:
  - `user` - User accounts (id, firstName, lastName, email, passwordHash, status, createdAt, updatedAt)
  - `role` - Roles (id, name, description)
  - `user_role` - User-role mapping (userId, roleId)
  - `permission` - Permissions (id, name, description)
  - `refresh_token` - Refresh token metadata (id, userId, tokenHash, sessionId, tokenFamilyId, status, issuedAt, expiresAt, rotatedAt, revokedAt)
  - `session` - User sessions (id, userId, status, ipAddress, userAgent, createdAt, lastSeenAt, revokedAt)
  - `audit_log` - Security audit trail
- **Security**:
  - Encryption at rest (AES-256)
  - SSL/TLS connections
  - Parameterized queries (SQL injection prevention)
  - Connection pooling with minimal privileges

#### Redis Cache
- **Requirement**: Section 12 (Redis Requirements)
- **Data**:
  - Token blacklist (key: `auth:blacklist:jti:<tokenId>`)
  - Rate limit counters (key: `auth:ratelimit:login:ip:<ip>`, `auth:ratelimit:login:user:<email>`)
  - Login attempt counters
  - Temporary lockout state
  - Token family metadata
- **Security**:
  - TLS connections
  - AUTH password protection
  - TTL-based expiration

#### AWS Secrets Manager
- **Requirement**: Section 13 (Configuration Requirements)
- **Secrets**:
  - JWT signing private key (RS256)
  - JWT verification public key
  - Database credentials
  - Redis password
  - Email service API keys
- **Security**:
  - KMS encryption
  - Automatic rotation
  - IAM-based access control
  - Audit trail of access

---

### External Services (Trust Boundary 4)

#### Email Service (AWS SES)
- **Requirement**: Future enhancement (email verification)
- **Usage**:
  - Account verification emails
  - Password reset emails (future)
  - Security alerts
- **Security**:
  - DKIM signing
  - SPF records
  - Rate limiting

---

## Security Zones and Trust Boundaries

### Zone 1: DMZ (Public Internet)
- **Risk Level**: HIGH
- **Controls**: WAF, DDoS protection, rate limiting, TLS 1.3
- **Access**: Public

### Zone 2: Application Tier
- **Risk Level**: MEDIUM
- **Controls**: Authentication, authorization, input validation, audit logging
- **Access**: Authenticated requests only (except registration/login)

### Zone 3: Data Tier
- **Risk Level**: CRITICAL
- **Controls**: Encryption at rest, encrypted connections, minimal privileges, connection pooling
- **Access**: Application services only (no direct external access)

### Zone 4: External Services
- **Risk Level**: MEDIUM
- **Controls**: API keys, rate limiting, circuit breakers, monitoring
- **Access**: Controlled via application services

---

## Communication Protocols

| Source | Destination | Protocol | Security |
|--------|-------------|----------|----------|
| Internet | WAF | HTTPS | TLS 1.3 |
| WAF | ALB | HTTPS | TLS 1.3 |
| ALB | API Gateway | HTTP/2 | Internal network |
| API Gateway | Controllers | HTTP | Internal network |
| Controllers | Services | In-process | N/A |
| Services | Database | PostgreSQL | SSL/TLS |
| Services | Redis | Redis protocol | TLS + AUTH |
| Services | Secrets Manager | HTTPS | TLS 1.3 + IAM |
| Services | Email Service | HTTPS | TLS 1.3 + API Key |

---

## High Availability and Scalability

### Stateless Components (Horizontal Scaling)
- API Gateway
- Controllers
- All service layer components
- Security components

### Stateful Components (Replication)
- PostgreSQL (Multi-AZ with read replicas)
- Redis (Cluster mode with replication)

### Health Checks
- Liveness: Service process is running
- Readiness: Service can accept traffic (DB and Redis connections healthy)

---

## Failure Modes and Resilience

| Component | Failure Mode | Mitigation |
|-----------|--------------|------------|
| Database Down | Cannot authenticate | Circuit breaker, read replicas, fail-safe deny |
| Redis Down | Cannot check blacklist | Configurable: deny all or allow with risk |
| Secrets Manager Down | Cannot load keys | Cache secrets locally with TTL |
| Email Service Down | Cannot send emails | Queue for retry, non-blocking operation |
| Rate Limiter Down | Cannot enforce limits | Fail-safe: allow with warning log |

---

## Observability

### Structured Logs
- JSON format with correlation IDs
- Log levels: DEBUG, INFO, WARN, ERROR
- No sensitive data (passwords, tokens)

### Metrics (Prometheus format)
- `auth_login_total{status="success|failure"}`
- `auth_token_refresh_total{status="success|failure"}`
- `auth_token_replay_detected_total`
- `auth_rate_limit_exceeded_total{endpoint}`
- `auth_rbac_denial_total{permission}`

### Distributed Tracing
- Correlation IDs across all components
- Request/response timing
- Service dependencies

### Audit Trail
- Persistent audit log in database
- Security-relevant events only
- Immutable records

---

## Compliance Mapping

| Control | Component | Requirement |
|---------|-----------|-------------|
| Password Protection | Password Manager | OWASP, CIS |
| Token Security | JWT Manager, Token Service | OAuth 2.0 BCP |
| Rate Limiting | Rate Limiter | OWASP API Security |
| Audit Logging | Audit Logger | SOC 2, PCI DSS |
| Encryption at Rest | Database, Secrets Manager | GDPR, HIPAA |
| Encryption in Transit | All network communication | PCI DSS, HIPAA |
| RBAC | RBAC Service | NIST RBAC |
| Input Validation | Input Validator | OWASP Top 10 |

