# Sequence Diagrams - AuthService2

## Overview
This document contains sequence diagrams for all authentication flows defined in the Authentication Service Requirements (AUTH-FR-001 through AUTH-FR-007).

---

## 1. User Registration Flow (AUTH-FR-001)

```mermaid
sequenceDiagram
    actor User
    participant ALB as Load Balancer<br/>(WAF)
    participant API as API Gateway
    participant AuthCtrl as Authentication<br/>Controller
    participant Validator as Input Validator
    participant UserSvc as User Service
    participant PwdMgr as Password Manager
    participant DB as PostgreSQL
    participant Audit as Audit Logger
    participant Metrics as Metrics Collector
    
    User->>ALB: POST /api/v1/auth/register<br/>{firstName, lastName, email, password}
    ALB->>ALB: WAF checks<br/>Rate limit check (3/hour)
    
    alt Rate limit exceeded
        ALB-->>User: 429 Too Many Requests
    end
    
    ALB->>API: Forward request
    API->>AuthCtrl: register(request)
    
    AuthCtrl->>Validator: validateRegistration(request)
    Validator->>Validator: Check email format
    Validator->>Validator: Check password policy:<br/>- Min 12 chars<br/>- Uppercase, lowercase<br/>- Number, special char
    
    alt Validation fails
        Validator-->>AuthCtrl: ValidationException
        AuthCtrl-->>User: 400 Bad Request<br/>{fieldErrors}
    end
    
    Validator-->>AuthCtrl: Valid
    
    AuthCtrl->>UserSvc: createUser(userData)
    UserSvc->>DB: SELECT * FROM user<br/>WHERE email = ?
    
    alt Email exists
        DB-->>UserSvc: User record found
        UserSvc-->>AuthCtrl: ConflictException
        AuthCtrl-->>User: 409 Conflict<br/>"Email already registered"
    end
    
    DB-->>UserSvc: Email unique
    
    UserSvc->>PwdMgr: hashPassword(password)
    PwdMgr->>PwdMgr: Generate bcrypt hash<br/>(strength: 12)
    PwdMgr-->>UserSvc: passwordHash
    
    UserSvc->>DB: INSERT INTO user<br/>(id, firstName, lastName,<br/>email, passwordHash,<br/>status='ACTIVE')
    DB-->>UserSvc: userId
    
    UserSvc->>DB: INSERT INTO user_role<br/>(userId, roleId='USER')
    
    UserSvc->>Audit: log(USER_REGISTERED,<br/>userId, email, timestamp)
    UserSvc->>Metrics: increment(user_registered_total)
    
    UserSvc-->>AuthCtrl: RegisterResponse(<br/>userId, email, status='ACTIVE')
    AuthCtrl-->>API: 201 Created
    API-->>ALB: Response
    ALB-->>User: 201 Created<br/>{userId, email, status, message}
    
    Note over User,DB: Password stored as bcrypt hash,<br/>never in plain text
```

---

## 2. User Login Flow (AUTH-FR-002)

```mermaid
sequenceDiagram
    actor User
    participant ALB as Load Balancer
    participant API as API Gateway
    participant AuthCtrl as Auth Controller
    participant RateLimiter as Rate Limiter
    participant AuthSvc as Auth Service
    participant PwdMgr as Password Manager
    participant TokenSvc as Token Service
    participant JWTMgr as JWT Manager
    participant DB as PostgreSQL
    participant Redis as Redis Cache
    participant Secrets as Secrets Manager
    participant Audit as Audit Logger
    participant Metrics as Metrics
    
    User->>ALB: POST /api/v1/auth/login<br/>{email, password}
    ALB->>API: Forward request
    API->>AuthCtrl: login(credentials)
    
    AuthCtrl->>RateLimiter: checkRateLimit(userIP, email)
    RateLimiter->>Redis: GET auth:ratelimit:login:ip:<ip>
    RateLimiter->>Redis: GET auth:ratelimit:login:user:<email>
    
    alt Rate limit exceeded (5 attempts/15min)
        RateLimiter-->>AuthCtrl: RateLimitExceededException
        AuthCtrl->>Metrics: increment(rate_limit_exceeded_total)
        AuthCtrl-->>User: 429 Too Many Requests<br/>Retry-After: 900
    end
    
    RateLimiter-->>AuthCtrl: OK
    
    AuthCtrl->>AuthSvc: authenticate(email, password)
    AuthSvc->>DB: SELECT id, email, passwordHash,<br/>status, failedLoginAttempts,<br/>lockedUntil<br/>FROM user<br/>WHERE email = ?
    
    alt User not found
        DB-->>AuthSvc: No record
        AuthSvc->>Audit: log(LOGIN_FAILED,<br/>email, reason='user_not_found')
        AuthSvc->>Metrics: increment(login_failed_total)
        AuthSvc-->>AuthCtrl: AuthenticationException
        AuthCtrl-->>User: 401 Unauthorized<br/>"Invalid email or password"
    end
    
    DB-->>AuthSvc: userRecord
    
    alt Account locked or disabled
        AuthSvc->>AuthSvc: Check status !=ACTIVE<br/>or lockedUntil > now
        AuthSvc->>Audit: log(LOGIN_BLOCKED,<br/>userId, reason='account_locked')
        AuthSvc-->>AuthCtrl: AccountLockedException
        AuthCtrl-->>User: 403 Forbidden<br/>"Account temporarily locked"
    end
    
    AuthSvc->>PwdMgr: verifyPassword(<br/>inputPassword,<br/>storedPasswordHash)
    PwdMgr->>PwdMgr: bcrypt.verify()
    
    alt Password invalid
        PwdMgr-->>AuthSvc: false
        AuthSvc->>DB: UPDATE user<br/>SET failedLoginAttempts += 1,<br/>lockedUntil = (if >= 5 attempts)<br/>WHERE id = ?
        AuthSvc->>Audit: log(INVALID_PASSWORD,<br/>userId, IP, userAgent)
        AuthSvc->>Metrics: increment(login_failed_total)
        AuthSvc-->>AuthCtrl: AuthenticationException
        AuthCtrl-->>User: 401 Unauthorized<br/>"Invalid email or password"
    end
    
    PwdMgr-->>AuthSvc: true (password valid)
    
    AuthSvc->>DB: SELECT r.name<br/>FROM role r<br/>JOIN user_role ur ON r.id = ur.roleId<br/>WHERE ur.userId = ?
    DB-->>AuthSvc: roles[] (e.g., ['USER'])
    
    AuthSvc->>DB: SELECT p.name<br/>FROM permission p<br/>JOIN role_permission rp ON p.id = rp.permissionId<br/>WHERE rp.roleId IN (roleIds)
    DB-->>AuthSvc: permissions[] (e.g., ['PROFILE_READ'])
    
    AuthSvc->>TokenSvc: generateAccessToken(<br/>userId, email, roles, permissions)
    TokenSvc->>JWTMgr: createToken(claims)
    JWTMgr->>Secrets: getSigningKey()
    Secrets-->>JWTMgr: privateKey (RS256)
    JWTMgr->>JWTMgr: Build JWT claims:<br/>{sub, email, roles, permissions,<br/>iss, aud, iat, exp=15min, jti}
    JWTMgr->>JWTMgr: Sign with RS256
    JWTMgr-->>TokenSvc: accessToken
    
    TokenSvc->>TokenSvc: generateRefreshToken()
    TokenSvc->>TokenSvc: Create UUID
    TokenSvc->>TokenSvc: Hash refresh token
    TokenSvc->>DB: INSERT INTO refresh_token<br/>(id, userId, tokenHash,<br/>sessionId, tokenFamilyId,<br/>status='ACTIVE',<br/>issuedAt, expiresAt=30days)
    
    TokenSvc->>Redis: SET session:<sessionId><br/>{userId, refreshTokenId}<br/>EX 2592000
    
    TokenSvc-->>AuthSvc: {accessToken, refreshToken, expiresIn=900}
    
    AuthSvc->>DB: UPDATE user<br/>SET failedLoginAttempts = 0,<br/>lastLoginAt = now()<br/>WHERE id = ?
    
    AuthSvc->>Audit: log(LOGIN_SUCCESS,<br/>userId, IP, userAgent, timestamp)
    AuthSvc->>Metrics: increment(login_success_total)
    
    AuthSvc-->>AuthCtrl: LoginResponse{<br/>accessToken, refreshToken,<br/>tokenType='Bearer', expiresIn=900,<br/>user{userId, email, roles}}
    
    AuthCtrl-->>User: 200 OK<br/>{accessToken, refreshToken,<br/>tokenType, expiresIn, user}
    
    Note over User,Redis: Access token expires in 15 minutes<br/>Refresh token expires in 30 days
```

---

## 3. Token Validation Flow (AUTH-FR-003)

```mermaid
sequenceDiagram
    actor Client
    participant API as API Gateway
    participant AuthCtrl as Auth Controller
    participant TokenSvc as Token Service
    participant JWTMgr as JWT Manager
    participant Blacklist as Blacklist Manager
    participant Redis as Redis Cache
    participant Secrets as Secrets Manager
    participant Metrics as Metrics
    
    Client->>API: POST /api/v1/auth/token/validate<br/>Authorization: Bearer <token>
    API->>API: Extract token from<br/>Authorization header
    
    alt No token provided
        API-->>Client: 400 Bad Request<br/>"Missing authorization header"
    end
    
    API->>AuthCtrl: validateToken(token)
    AuthCtrl->>TokenSvc: validate(token)
    
    TokenSvc->>JWTMgr: verifyToken(token)
    JWTMgr->>Secrets: getVerificationKey()
    Secrets-->>JWTMgr: publicKey (RS256)
    
    JWTMgr->>JWTMgr: Verify signature
    
    alt Invalid signature
        JWTMgr-->>TokenSvc: TokenInvalidException
        TokenSvc->>Metrics: increment(token_invalid_total)
        TokenSvc-->>Client: 401 Unauthorized<br/>"Invalid token signature"
    end
    
    JWTMgr->>JWTMgr: Check expiration (exp claim)
    
    alt Token expired
        JWTMgr-->>TokenSvc: TokenExpiredException
        TokenSvc->>Metrics: increment(token_expired_total)
        TokenSvc-->>Client: 401 Unauthorized<br/>"Token has expired"
    end
    
    JWTMgr->>JWTMgr: Validate issuer (iss claim)
    JWTMgr->>JWTMgr: Validate audience (aud claim)
    
    alt Issuer or audience invalid
        JWTMgr-->>TokenSvc: TokenInvalidException
        TokenSvc-->>Client: 401 Unauthorized<br/>"Invalid issuer or audience"
    end
    
    JWTMgr-->>TokenSvc: claims{sub, email,<br/>roles, permissions, jti, exp}
    
    TokenSvc->>Blacklist: isBlacklisted(jti)
    Blacklist->>Redis: GET auth:blacklist:jti:<jti>
    
    alt Token blacklisted
        Redis-->>Blacklist: true
        Blacklist-->>TokenSvc: true
        TokenSvc->>Metrics: increment(token_blacklisted_access_total)
        TokenSvc-->>Client: 401 Unauthorized<br/>"Token has been revoked"
    end
    
    Redis-->>Blacklist: false
    Blacklist-->>TokenSvc: false
    
    TokenSvc->>Metrics: increment(token_validated_total)
    TokenSvc-->>AuthCtrl: TokenValidationResponse{<br/>valid=true, userId, email,<br/>roles, permissions, expiresAt}
    
    AuthCtrl-->>Client: 200 OK<br/>{valid, userId, email,<br/>roles, permissions, expiresAt}
```

---

## 4. Token Refresh Flow with Rotation (AUTH-FR-004)

```mermaid
sequenceDiagram
    actor Client
    participant API as API Gateway
    participant AuthCtrl as Auth Controller
    participant TokenSvc as Token Service
    participant ReplayGuard as Replay Prevention
    participant JWTMgr as JWT Manager
    participant DB as PostgreSQL
    participant Redis as Redis Cache
    participant Audit as Audit Logger
    participant Metrics as Metrics
    
    Client->>API: POST /api/v1/auth/token/refresh<br/>{refreshToken}
    API->>AuthCtrl: refreshToken(refreshToken)
    AuthCtrl->>TokenSvc: validateAndRefresh(refreshToken)
    
    TokenSvc->>TokenSvc: Hash refresh token
    TokenSvc->>DB: SELECT id, userId, tokenHash,<br/>sessionId, tokenFamilyId,<br/>status, expiresAt<br/>FROM refresh_token<br/>WHERE tokenHash = ?
    
    alt Token not found
        DB-->>TokenSvc: No record
        TokenSvc->>Metrics: increment(refresh_token_invalid_total)
        TokenSvc-->>Client: 401 Unauthorized<br/>"Invalid refresh token"
    end
    
    DB-->>TokenSvc: tokenRecord
    
    alt Token expired
        TokenSvc->>TokenSvc: Check expiresAt < now()
        TokenSvc->>Metrics: increment(refresh_token_expired_total)
        TokenSvc-->>Client: 401 Unauthorized<br/>"Refresh token has expired"
    end
    
    TokenSvc->>ReplayGuard: checkReuse(<br/>tokenFamilyId, status)
    
    alt Token already used/rotated
        ReplayGuard->>ReplayGuard: status == 'ROTATED'
        ReplayGuard->>Audit: log(REFRESH_TOKEN_REPLAY,<br/>userId, sessionId,<br/>tokenFamilyId, IP)
        ReplayGuard->>DB: UPDATE refresh_token<br/>SET status = 'REVOKED'<br/>WHERE tokenFamilyId = ?
        ReplayGuard->>Redis: DEL session:<sessionId>
        ReplayGuard->>Metrics: increment(token_replay_detected_total)
        ReplayGuard-->>TokenSvc: ReplayDetectedException
        TokenSvc-->>Client: 403 Forbidden<br/>"Session revoked due to<br/>security event"
    end
    
    ReplayGuard-->>TokenSvc: OK
    
    TokenSvc->>DB: SELECT id, email, status<br/>FROM user<br/>WHERE id = ?
    
    alt User inactive or deleted
        DB-->>TokenSvc: status != 'ACTIVE'
        TokenSvc->>Audit: log(INACTIVE_USER_ACCESS, userId)
        TokenSvc-->>Client: 401 Unauthorized<br/>"User account not active"
    end
    
    DB-->>TokenSvc: userRecord
    
    TokenSvc->>DB: SELECT r.name<br/>FROM role r<br/>JOIN user_role ur<br/>WHERE ur.userId = ?
    DB-->>TokenSvc: roles[]
    
    TokenSvc->>DB: SELECT p.name<br/>FROM permission p<br/>JOIN role_permission rp<br/>WHERE roleId IN (roleIds)
    DB-->>TokenSvc: permissions[]
    
    TokenSvc->>JWTMgr: createAccessToken(<br/>userId, email, roles, permissions)
    JWTMgr-->>TokenSvc: newAccessToken
    
    TokenSvc->>TokenSvc: Generate new refresh token
    TokenSvc->>TokenSvc: Hash new refresh token
    
    TokenSvc->>DB: BEGIN TRANSACTION
    
    TokenSvc->>DB: UPDATE refresh_token<br/>SET status = 'ROTATED',<br/>rotatedAt = now()<br/>WHERE id = ?
    
    TokenSvc->>DB: INSERT INTO refresh_token<br/>(id, userId, tokenHash,<br/>sessionId, tokenFamilyId,<br/>status='ACTIVE',<br/>issuedAt, expiresAt)
    
    TokenSvc->>DB: COMMIT
    
    TokenSvc->>Redis: SET session:<sessionId><br/>{userId, newRefreshTokenId}<br/>EX 2592000
    
    TokenSvc->>Audit: log(TOKEN_REFRESHED,<br/>userId, sessionId, timestamp)
    TokenSvc->>Metrics: increment(token_refreshed_total)
    
    TokenSvc-->>AuthCtrl: TokenResponse{<br/>accessToken, refreshToken,<br/>tokenType='Bearer', expiresIn=900}
    
    AuthCtrl-->>Client: 200 OK<br/>{accessToken, refreshToken,<br/>tokenType, expiresIn}
    
    Note over Client,Redis: Old refresh token invalidated<br/>New tokens issued<br/>Token family tracked
```

---

## 5. Session Invalidation / Logout Flow (AUTH-FR-005)

```mermaid
sequenceDiagram
    actor User
    participant API as API Gateway
    participant AuthCtrl as Auth Controller
    participant AuthSvc as Auth Service
    participant TokenSvc as Token Service
    participant JWTMgr as JWT Manager
    participant Blacklist as Blacklist Manager
    participant DB as PostgreSQL
    participant Redis as Redis Cache
    participant Audit as Audit Logger
    participant Metrics as Metrics
    
    User->>API: POST /api/v1/auth/logout<br/>Authorization: Bearer <token>
    API->>API: Extract access token
    API->>AuthCtrl: logout(token)
    
    AuthCtrl->>JWTMgr: validateToken(token)
    JWTMgr->>JWTMgr: Verify signature & expiration
    JWTMgr-->>AuthCtrl: claims{sub=userId, jti, exp}
    
    AuthCtrl->>AuthSvc: invalidateSession(<br/>userId, tokenId=jti, exp)
    
    AuthSvc->>TokenSvc: revokeTokens(userId)
    TokenSvc->>DB: SELECT sessionId<br/>FROM refresh_token<br/>WHERE userId = ?<br/>AND status = 'ACTIVE'
    DB-->>TokenSvc: sessionIds[]
    
    TokenSvc->>DB: UPDATE refresh_token<br/>SET status = 'REVOKED',<br/>revokedAt = now()<br/>WHERE userId = ?<br/>AND status = 'ACTIVE'
    
    loop For each session
        TokenSvc->>Redis: DEL session:<sessionId>
    end
    
    TokenSvc->>Blacklist: addToBlacklist(<br/>tokenId=jti, userId, exp)
    Blacklist->>Blacklist: Calculate TTL<br/>(exp - now)
    Blacklist->>Redis: SET auth:blacklist:jti:<jti><br/>VALUE {userId, exp, reason='logout'}<br/>EX <ttl_seconds>
    
    Note over Redis: Token blacklisted until<br/>original expiration time
    
    TokenSvc->>Audit: log(USER_LOGOUT,<br/>userId, sessionIds, timestamp)
    TokenSvc->>Metrics: increment(logout_total)
    
    TokenSvc-->>AuthSvc: Success
    AuthSvc-->>AuthCtrl: Success
    AuthCtrl-->>User: 204 No Content
    
    Note over User,Redis: Access token blacklisted<br/>All refresh tokens revoked<br/>All sessions invalidated
```

---

## 6. Retrieve Authenticated User Details (AUTH-FR-006)

```mermaid
sequenceDiagram
    actor User
    participant API as API Gateway
    participant UserCtrl as User Controller
    participant TokenSvc as Token Service
    participant JWTMgr as JWT Manager
    participant Blacklist as Blacklist Manager
    participant UserSvc as User Service
    participant RBACSvc as RBAC Service
    participant DB as PostgreSQL
    participant Redis as Redis Cache
    participant Audit as Audit Logger
    
    User->>API: GET /api/v1/auth/me<br/>Authorization: Bearer <token>
    API->>API: Extract access token
    
    API->>TokenSvc: validateToken(token)
    TokenSvc->>JWTMgr: verifyToken(token)
    JWTMgr->>JWTMgr: Verify signature,<br/>expiration, issuer, audience
    
    alt Token invalid or expired
        JWTMgr-->>User: 401 Unauthorized
    end
    
    JWTMgr-->>TokenSvc: claims{sub=userId, jti}
    
    TokenSvc->>Blacklist: isBlacklisted(jti)
    Blacklist->>Redis: GET auth:blacklist:jti:<jti>
    
    alt Token blacklisted
        Redis-->>Blacklist: true
        Blacklist-->>User: 401 Unauthorized<br/>"Token has been revoked"
    end
    
    Redis-->>Blacklist: false
    TokenSvc-->>API: Valid, userId
    
    API->>UserCtrl: getCurrentUser(userId)
    UserCtrl->>RBACSvc: hasPermission(<br/>userId, 'PROFILE_READ')
    
    alt No permission
        RBACSvc->>Audit: log(ACCESS_DENIED,<br/>userId, 'PROFILE_READ')
        RBACSvc-->>UserCtrl: false
        UserCtrl-->>User: 403 Forbidden<br/>"Insufficient permissions"
    end
    
    RBACSvc-->>UserCtrl: true
    
    UserCtrl->>UserSvc: getUserById(userId)
    UserSvc->>DB: SELECT id, firstName, lastName,<br/>email, status,<br/>createdAt, updatedAt<br/>FROM user<br/>WHERE id = ?
    
    alt User not found
        DB-->>UserSvc: No record
        UserSvc-->>User: 404 Not Found<br/>"User not found"
    end
    
    DB-->>UserSvc: userRecord
    
    alt User not active
        UserSvc->>UserSvc: Check status != 'ACTIVE'
        UserSvc-->>User: 403 Forbidden<br/>"Account not active"
    end
    
    UserSvc->>RBACSvc: getUserRoles(userId)
    RBACSvc->>DB: SELECT r.name<br/>FROM role r<br/>JOIN user_role ur<br/>WHERE ur.userId = ?
    DB-->>RBACSvc: roles[]
    
    RBACSvc->>DB: SELECT p.name<br/>FROM permission p<br/>JOIN role_permission rp<br/>WHERE roleId IN (roleIds)
    DB-->>RBACSvc: permissions[]
    
    RBACSvc-->>UserSvc: {roles[], permissions[]}
    
    UserSvc->>Audit: log(RESOURCE_ACCESSED,<br/>userId, '/auth/me', timestamp)
    
    UserSvc-->>UserCtrl: UserResponse{<br/>userId, firstName, lastName,<br/>email, roles, permissions,<br/>status, createdAt, updatedAt}
    
    UserCtrl-->>User: 200 OK<br/>{userId, firstName, lastName,<br/>email, roles, permissions,<br/>status, createdAt, updatedAt}
    
    Note over User,DB: Password hash never returned<br/>Only non-sensitive data exposed
```

---

## 7. RBAC Authorization Check (AUTH-FR-007)

```mermaid
sequenceDiagram
    actor Client
    participant API as API Gateway
    participant Controller as Protected Controller
    participant RBACSvc as RBAC Service
    participant DB as PostgreSQL
    participant Audit as Audit Logger
    participant Metrics as Metrics
    
    Client->>API: Request to protected endpoint<br/>Authorization: Bearer <token>
    API->>API: Validate token<br/>(extract userId, roles)
    
    API->>Controller: handleRequest(<br/>userId, requestedResource)
    Controller->>RBACSvc: hasPermission(<br/>userId, resource, action)
    
    RBACSvc->>DB: SELECT r.name<br/>FROM role r<br/>JOIN user_role ur<br/>WHERE ur.userId = ?
    DB-->>RBACSvc: userRoles[]
    
    RBACSvc->>DB: SELECT p.name, p.resource, p.action<br/>FROM permission p<br/>JOIN role_permission rp<br/>WHERE rp.roleId IN (roleIds)
    DB-->>RBACSvc: permissions[]
    
    RBACSvc->>RBACSvc: Check if required permission<br/>exists in user's permissions
    
    alt Permission denied
        RBACSvc->>Audit: log(ACCESS_DENIED,<br/>userId, resource, action,<br/>requiredPermission)
        RBACSvc->>Metrics: increment(rbac_denial_total,<br/>{permission})
        RBACSvc-->>Controller: false
        Controller-->>Client: 403 Forbidden<br/>"Insufficient permissions"
    end
    
    RBACSvc-->>Controller: true
    Controller->>Controller: Process request
    
    Controller->>Audit: log(AUTHORIZED_ACCESS,<br/>userId, resource, action)
    
    Controller-->>Client: 200 OK<br/>{response data}
    
    Note over Client,DB: Authorization enforced<br/>at every protected endpoint<br/>Fail-safe deny by default
```

---

## 8. Rate Limiting Flow

```mermaid
sequenceDiagram
    actor User
    participant ALB as Load Balancer
    participant API as API Gateway
    participant RateLimiter as Rate Limiter
    participant Redis as Redis Cache
    participant Metrics as Metrics
    
    User->>ALB: Request to rate-limited endpoint
    ALB->>API: Forward request
    API->>RateLimiter: checkRateLimit(<br/>endpoint, userIP, userId)
    
    RateLimiter->>Redis: GET auth:ratelimit:<endpoint>:<key>
    Redis-->>RateLimiter: currentCount
    
    alt Rate limit exceeded
        RateLimiter->>RateLimiter: currentCount >= limit
        RateLimiter->>Metrics: increment(rate_limit_exceeded_total)
        RateLimiter-->>API: RateLimitExceededException<br/>{retryAfter}
        API-->>User: 429 Too Many Requests<br/>X-RateLimit-Limit: 5<br/>X-RateLimit-Remaining: 0<br/>X-RateLimit-Reset: <timestamp><br/>Retry-After: 900
    end
    
    RateLimiter->>Redis: INCR auth:ratelimit:<endpoint>:<key>
    RateLimiter->>Redis: EXPIRE auth:ratelimit:<endpoint>:<key><br/><window_seconds>
    
    RateLimiter-->>API: OK<br/>{remaining=limit-currentCount}
    API->>API: Add rate limit headers
    API-->>User: Response with headers:<br/>X-RateLimit-Limit: 5<br/>X-RateLimit-Remaining: 3<br/>X-RateLimit-Reset: <timestamp>
    
    Note over User,Redis: Sliding window rate limiting<br/>Per IP and per user<br/>Redis TTL for automatic cleanup
```

---

## Security Flow Summary

### Trust Boundaries Crossed

| Flow | Trust Boundaries | Security Controls |
|------|------------------|-------------------|
| Registration | Internet → DMZ → App → Data | WAF, Rate limit, Input validation, Password hashing |
| Login | Internet → DMZ → App → Data | WAF, Rate limit, Brute-force protection, bcrypt verification |
| Token Validation | Internet → DMZ → App → Data (Redis) | Signature verification, Blacklist check, Expiration check |
| Token Refresh | Internet → DMZ → App → Data | Token rotation, Replay detection, Session validation |
| Logout | Internet → DMZ → App → Data | Token blacklist, Session revocation |
| Get User | Internet → DMZ → App → Data | Token validation, RBAC check, Data filtering |

### Security Events Audited

1. **User Registration** - userId, email, timestamp
2. **Login Success** - userId, IP, userAgent, timestamp  
3. **Login Failure** - email, IP, reason, timestamp
4. **Token Refresh** - userId, sessionId, timestamp
5. **Refresh Token Replay** - userId, sessionId, tokenFamilyId, IP
6. **Logout** - userId, sessionId, timestamp
7. **Token Blacklist** - tokenId, userId, expiration
8. **RBAC Denial** - userId, requiredPermission, endpoint

### Error Handling Strategy

- **400 Bad Request**: Input validation failures with field-level errors
- **401 Unauthorized**: Authentication failures (generic message to prevent enumeration)
- **403 Forbidden**: Authorization failures, account locked/disabled
- **404 Not Found**: Resource not found
- **409 Conflict**: Duplicate resource (e.g., email already exists)
- **429 Too Many Requests**: Rate limit exceeded with retry-after header
- **500 Internal Server Error**: Unexpected server errors (logged, not exposed)

### Performance Targets

| Operation | Target Latency | SLA |
|-----------|----------------|-----|
| Login | < 300 ms | 99th percentile |
| Token Validation | < 50 ms | 99th percentile |
| Token Refresh | < 200 ms | 99th percentile |
| Registration | < 400 ms | 99th percentile |
| Get User | < 100 ms | 99th percentile |

---

## Observability Integration

All sequence flows include:
- **Correlation ID** propagated through all components
- **Structured logging** at key decision points
- **Metrics collection** for success/failure rates
- **Audit logging** for security-relevant events
- **Distributed tracing** for end-to-end request visibility

