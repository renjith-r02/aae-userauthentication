# API Contracts - AuthService2

## Overview
This document provides the complete OpenAPI 3.0 specification for the Authentication Service API endpoints.

---

## OpenAPI Specification

```yaml
openapi: 3.0.3
info:
  title: Authentication Service API
  description: |
    Enterprise-grade Authentication Service providing secure user registration, 
    JWT-based authentication, refresh token rotation, session management, and 
    role-based access control (RBAC).
    
    ## Security Features
    - bcrypt password hashing
    - JWT with RS256 signing
    - Refresh token rotation with replay detection
    - Rate limiting and brute-force protection
    - Token blacklisting
    - RBAC authorization
    - Comprehensive audit logging
    
    ## Base URL
    - Production: `https://api.authservice.example.com`
    - Staging: `https://api-staging.authservice.example.com`
    - Development: `http://localhost:8080`
  version: 1.0.0
  contact:
    name: Platform Team
    email: platform@example.com
  license:
    name: Apache 2.0
    url: https://www.apache.org/licenses/LICENSE-2.0.html

servers:
  - url: https://api.authservice.example.com
    description: Production server
  - url: https://api-staging.authservice.example.com
    description: Staging server
  - url: http://localhost:8080
    description: Development server

tags:
  - name: Authentication
    description: User authentication and token management operations
  - name: User Management
    description: User profile and account management
  - name: Health
    description: Service health and readiness checks

security:
  - BearerAuth: []

paths:
  /api/v1/auth/register:
    post:
      tags:
        - Authentication
      summary: Register a new user
      description: |
        Creates a new user account with the provided details. Password must meet
        security policy requirements (minimum 12 characters, uppercase, lowercase,
        number, special character). Email must be unique.
      operationId: registerUser
      security: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/RegisterRequest'
            examples:
              validRegistration:
                summary: Valid registration request
                value:
                  firstName: John
                  lastName: Doe
                  email: john.doe@example.com
                  password: SecurePassword@123
      responses:
        '201':
          description: User registered successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/RegisterResponse'
              examples:
                success:
                  value:
                    userId: 8f2b5f10-91c4-4c3f-84f0-392cbe4e2a21
                    email: john.doe@example.com
                    status: ACTIVE
                    message: User registered successfully
        '400':
          description: Invalid input or password policy violation
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              examples:
                invalidPassword:
                  value:
                    timestamp: '2026-05-13T12:00:00Z'
                    status: 400
                    error: Bad Request
                    message: Password does not meet policy requirements
                    path: /api/v1/auth/register
                    traceId: 7af92bc8e8a24c1d
        '409':
          description: Email already exists
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              examples:
                duplicateEmail:
                  value:
                    timestamp: '2026-05-13T12:00:00Z'
                    status: 409
                    error: Conflict
                    message: Email already registered
                    path: /api/v1/auth/register
                    traceId: 7af92bc8e8a24c1d
        '500':
          $ref: '#/components/responses/InternalServerError'

  /api/v1/auth/login:
    post:
      tags:
        - Authentication
      summary: Authenticate user and issue tokens
      description: |
        Authenticates a user with email and password. Returns JWT access token
        (short-lived, 15 minutes) and refresh token (long-lived, 30 days).
        Rate limiting is applied to prevent brute-force attacks (5 attempts per 15 minutes).
      operationId: loginUser
      security: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/LoginRequest'
            examples:
              validLogin:
                summary: Valid login request
                value:
                  email: john.doe@example.com
                  password: SecurePassword@123
      responses:
        '200':
          description: Authentication successful
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/LoginResponse'
              examples:
                success:
                  value:
                    accessToken: eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
                    refreshToken: 89ca9d6e-d1c3-4df3-b6c7-8d87a1b29e11
                    tokenType: Bearer
                    expiresIn: 900
                    user:
                      userId: 8f2b5f10-91c4-4c3f-84f0-392cbe4e2a21
                      email: john.doe@example.com
                      roles:
                        - USER
        '400':
          $ref: '#/components/responses/BadRequest'
        '401':
          description: Invalid credentials
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              examples:
                invalidCredentials:
                  value:
                    timestamp: '2026-05-13T12:00:00Z'
                    status: 401
                    error: Unauthorized
                    message: Invalid email or password
                    path: /api/v1/auth/login
                    traceId: 7af92bc8e8a24c1d
        '403':
          description: Account disabled or locked
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              examples:
                accountLocked:
                  value:
                    timestamp: '2026-05-13T12:00:00Z'
                    status: 403
                    error: Forbidden
                    message: Account temporarily locked due to multiple failed login attempts
                    path: /api/v1/auth/login
                    traceId: 7af92bc8e8a24c1d
        '429':
          description: Too many login attempts
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              examples:
                rateLimitExceeded:
                  value:
                    timestamp: '2026-05-13T12:00:00Z'
                    status: 429
                    error: Too Many Requests
                    message: Rate limit exceeded. Please try again later
                    path: /api/v1/auth/login
                    traceId: 7af92bc8e8a24c1d
          headers:
            Retry-After:
              description: Number of seconds to wait before retrying
              schema:
                type: integer
              example: 900
        '500':
          $ref: '#/components/responses/InternalServerError'

  /api/v1/auth/token/validate:
    post:
      tags:
        - Authentication
      summary: Validate JWT access token
      description: |
        Validates JWT access token for signature, expiration, issuer, audience,
        and blacklist status. Returns token claims if valid.
      operationId: validateToken
      security:
        - BearerAuth: []
      responses:
        '200':
          description: Token is valid
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TokenValidationResponse'
              examples:
                validToken:
                  value:
                    valid: true
                    userId: 8f2b5f10-91c4-4c3f-84f0-392cbe4e2a21
                    email: john.doe@example.com
                    roles:
                      - USER
                    permissions:
                      - PROFILE_READ
                    expiresAt: '2026-05-13T12:30:00Z'
        '400':
          description: Missing or malformed token
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '401':
          description: Invalid, expired, or blacklisted token
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              examples:
                expiredToken:
                  value:
                    timestamp: '2026-05-13T12:00:00Z'
                    status: 401
                    error: Unauthorized
                    message: Token has expired
                    path: /api/v1/auth/token/validate
                    traceId: 7af92bc8e8a24c1d
                blacklistedToken:
                  value:
                    timestamp: '2026-05-13T12:00:00Z'
                    status: 401
                    error: Unauthorized
                    message: Token has been revoked
                    path: /api/v1/auth/token/validate
                    traceId: 7af92bc8e8a24c1d
        '500':
          $ref: '#/components/responses/InternalServerError'

  /api/v1/auth/token/refresh:
    post:
      tags:
        - Authentication
      summary: Refresh access token
      description: |
        Generates a new access token using a valid refresh token. Implements
        refresh token rotation - each refresh operation generates a new refresh
        token and invalidates the old one. Detects and prevents refresh token
        replay attacks.
      operationId: refreshToken
      security: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/RefreshTokenRequest'
            examples:
              validRefresh:
                summary: Valid refresh request
                value:
                  refreshToken: 89ca9d6e-d1c3-4df3-b6c7-8d87a1b29e11
      responses:
        '200':
          description: Token refreshed successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TokenResponse'
              examples:
                success:
                  value:
                    accessToken: eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
                    refreshToken: f3a9b172-77cc-4c9f-8f44-92710e0a9d19
                    tokenType: Bearer
                    expiresIn: 900
        '400':
          description: Missing refresh token
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '401':
          description: Invalid, expired, or reused refresh token
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              examples:
                expiredToken:
                  value:
                    timestamp: '2026-05-13T12:00:00Z'
                    status: 401
                    error: Unauthorized
                    message: Refresh token has expired
                    path: /api/v1/auth/token/refresh
                    traceId: 7af92bc8e8a24c1d
                reusedToken:
                  value:
                    timestamp: '2026-05-13T12:00:00Z'
                    status: 401
                    error: Unauthorized
                    message: Refresh token has already been used
                    path: /api/v1/auth/token/refresh
                    traceId: 7af92bc8e8a24c1d
        '403':
          description: Session revoked due to security event
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              examples:
                sessionRevoked:
                  value:
                    timestamp: '2026-05-13T12:00:00Z'
                    status: 403
                    error: Forbidden
                    message: Session has been revoked. Please log in again
                    path: /api/v1/auth/token/refresh
                    traceId: 7af92bc8e8a24c1d
        '500':
          $ref: '#/components/responses/InternalServerError'

  /api/v1/auth/logout:
    post:
      tags:
        - Authentication
      summary: Invalidate session and tokens
      description: |
        Logs out the authenticated user by invalidating their session and tokens.
        Adds the access token to Redis blacklist and revokes the refresh token.
      operationId: logoutUser
      security:
        - BearerAuth: []
      responses:
        '204':
          description: Session invalidated successfully
        '401':
          description: Missing or invalid token
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '500':
          $ref: '#/components/responses/InternalServerError'

  /api/v1/auth/me:
    get:
      tags:
        - User Management
      summary: Retrieve authenticated user details
      description: |
        Returns the profile and authorization details of the currently
        authenticated user. Includes roles and permissions for RBAC.
      operationId: getCurrentUser
      security:
        - BearerAuth: []
      responses:
        '200':
          description: User details retrieved successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/UserResponse'
              examples:
                success:
                  value:
                    userId: 8f2b5f10-91c4-4c3f-84f0-392cbe4e2a21
                    firstName: John
                    lastName: Doe
                    email: john.doe@example.com
                    roles:
                      - USER
                    permissions:
                      - PROFILE_READ
                    status: ACTIVE
                    createdAt: '2026-05-01T10:00:00Z'
                    updatedAt: '2026-05-13T12:00:00Z'
        '401':
          description: Missing or invalid token
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '403':
          description: User does not have required permission
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '404':
          description: User not found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '500':
          $ref: '#/components/responses/InternalServerError'

  /actuator/health:
    get:
      tags:
        - Health
      summary: Service health check
      description: Returns overall health status of the service
      operationId: healthCheck
      security: []
      responses:
        '200':
          description: Service is healthy
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HealthResponse'
              examples:
                healthy:
                  value:
                    status: UP
                    components:
                      database:
                        status: UP
                        details:
                          database: PostgreSQL
                          validConnection: true
                      redis:
                        status: UP
                        details:
                          validConnection: true
                    timestamp: '2026-05-13T12:00:00Z'
        '503':
          description: Service is unhealthy
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/HealthResponse'
              examples:
                unhealthy:
                  value:
                    status: DOWN
                    components:
                      database:
                        status: DOWN
                        details:
                          error: Connection timeout
                    timestamp: '2026-05-13T12:00:00Z'

  /actuator/health/liveness:
    get:
      tags:
        - Health
      summary: Liveness probe
      description: Kubernetes liveness probe endpoint
      operationId: livenessCheck
      security: []
      responses:
        '200':
          description: Service is alive
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/LivenessResponse'
        '503':
          description: Service is not alive

  /actuator/health/readiness:
    get:
      tags:
        - Health
      summary: Readiness probe
      description: Kubernetes readiness probe endpoint
      operationId: readinessCheck
      security: []
      responses:
        '200':
          description: Service is ready to accept traffic
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ReadinessResponse'
        '503':
          description: Service is not ready

components:
  securitySchemes:
    BearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
      description: |
        JWT access token obtained from the login endpoint.
        Format: `Authorization: Bearer <access_token>`

  schemas:
    RegisterRequest:
      type: object
      required:
        - firstName
        - lastName
        - email
        - password
      properties:
        firstName:
          type: string
          minLength: 1
          maxLength: 100
          description: User's first name
          example: John
        lastName:
          type: string
          minLength: 1
          maxLength: 100
          description: User's last name
          example: Doe
        email:
          type: string
          format: email
          maxLength: 255
          description: Unique email address
          example: john.doe@example.com
        password:
          type: string
          format: password
          minLength: 12
          description: |
            Password must meet security policy:
            - Minimum 12 characters
            - At least one uppercase letter
            - At least one lowercase letter
            - At least one number
            - At least one special character
          example: SecurePassword@123

    RegisterResponse:
      type: object
      properties:
        userId:
          type: string
          format: uuid
          description: Unique user identifier
          example: 8f2b5f10-91c4-4c3f-84f0-392cbe4e2a21
        email:
          type: string
          format: email
          description: User's email address
          example: john.doe@example.com
        status:
          type: string
          enum: [ACTIVE, PENDING, LOCKED, DISABLED]
          description: Account status
          example: ACTIVE
        message:
          type: string
          description: Success message
          example: User registered successfully

    LoginRequest:
      type: object
      required:
        - email
        - password
      properties:
        email:
          type: string
          format: email
          description: User's email address
          example: john.doe@example.com
        password:
          type: string
          format: password
          description: User's password
          example: SecurePassword@123

    LoginResponse:
      type: object
      properties:
        accessToken:
          type: string
          description: JWT access token (short-lived, 15 minutes)
          example: eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
        refreshToken:
          type: string
          format: uuid
          description: Refresh token (long-lived, 30 days)
          example: 89ca9d6e-d1c3-4df3-b6c7-8d87a1b29e11
        tokenType:
          type: string
          description: Token type (always "Bearer")
          example: Bearer
        expiresIn:
          type: integer
          description: Access token expiration time in seconds
          example: 900
        user:
          $ref: '#/components/schemas/UserInfo'

    UserInfo:
      type: object
      properties:
        userId:
          type: string
          format: uuid
          example: 8f2b5f10-91c4-4c3f-84f0-392cbe4e2a21
        email:
          type: string
          format: email
          example: john.doe@example.com
        roles:
          type: array
          items:
            type: string
          description: User roles for RBAC
          example: [USER]

    TokenValidationResponse:
      type: object
      properties:
        valid:
          type: boolean
          description: Whether the token is valid
          example: true
        userId:
          type: string
          format: uuid
          example: 8f2b5f10-91c4-4c3f-84f0-392cbe4e2a21
        email:
          type: string
          format: email
          example: john.doe@example.com
        roles:
          type: array
          items:
            type: string
          example: [USER]
        permissions:
          type: array
          items:
            type: string
          example: [PROFILE_READ]
        expiresAt:
          type: string
          format: date-time
          description: Token expiration timestamp
          example: '2026-05-13T12:30:00Z'

    RefreshTokenRequest:
      type: object
      required:
        - refreshToken
      properties:
        refreshToken:
          type: string
          format: uuid
          description: Refresh token obtained from login
          example: 89ca9d6e-d1c3-4df3-b6c7-8d87a1b29e11

    TokenResponse:
      type: object
      properties:
        accessToken:
          type: string
          description: New JWT access token
          example: eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...
        refreshToken:
          type: string
          format: uuid
          description: New refresh token (rotated)
          example: f3a9b172-77cc-4c9f-8f44-92710e0a9d19
        tokenType:
          type: string
          example: Bearer
        expiresIn:
          type: integer
          description: Access token expiration time in seconds
          example: 900

    UserResponse:
      type: object
      properties:
        userId:
          type: string
          format: uuid
          example: 8f2b5f10-91c4-4c3f-84f0-392cbe4e2a21
        firstName:
          type: string
          example: John
        lastName:
          type: string
          example: Doe
        email:
          type: string
          format: email
          example: john.doe@example.com
        roles:
          type: array
          items:
            type: string
          description: User roles
          example: [USER]
        permissions:
          type: array
          items:
            type: string
          description: User permissions
          example: [PROFILE_READ]
        status:
          type: string
          enum: [ACTIVE, PENDING, LOCKED, DISABLED]
          example: ACTIVE
        createdAt:
          type: string
          format: date-time
          example: '2026-05-01T10:00:00Z'
        updatedAt:
          type: string
          format: date-time
          example: '2026-05-13T12:00:00Z'

    ErrorResponse:
      type: object
      properties:
        timestamp:
          type: string
          format: date-time
          description: Error timestamp
          example: '2026-05-13T12:00:00Z'
        status:
          type: integer
          description: HTTP status code
          example: 401
        error:
          type: string
          description: Error type
          example: Unauthorized
        message:
          type: string
          description: Human-readable error message
          example: Invalid or expired token
        path:
          type: string
          description: Request path that caused the error
          example: /api/v1/auth/me
        traceId:
          type: string
          description: Correlation ID for tracing
          example: 7af92bc8e8a24c1d
        fieldErrors:
          type: array
          items:
            $ref: '#/components/schemas/FieldError'
          description: Field-level validation errors (if applicable)

    FieldError:
      type: object
      properties:
        field:
          type: string
          description: Field name that failed validation
          example: password
        message:
          type: string
          description: Validation error message
          example: Password must be at least 12 characters

    HealthResponse:
      type: object
      properties:
        status:
          type: string
          enum: [UP, DOWN, UNKNOWN]
          example: UP
        components:
          type: object
          additionalProperties:
            $ref: '#/components/schemas/ComponentHealth'
        timestamp:
          type: string
          format: date-time
          example: '2026-05-13T12:00:00Z'

    ComponentHealth:
      type: object
      properties:
        status:
          type: string
          enum: [UP, DOWN, UNKNOWN]
          example: UP
        details:
          type: object
          additionalProperties: true

    LivenessResponse:
      type: object
      properties:
        status:
          type: string
          enum: [UP, DOWN]
          example: UP

    ReadinessResponse:
      type: object
      properties:
        status:
          type: string
          enum: [UP, DOWN]
          example: UP

  responses:
    BadRequest:
      description: Invalid request
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
    
    Unauthorized:
      description: Authentication required or failed
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
    
    Forbidden:
      description: Insufficient permissions
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
    
    NotFound:
      description: Resource not found
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
    
    InternalServerError:
      description: Internal server error
      content:
        application/json:
          schema:
            $ref: '#/components/schemas/ErrorResponse'
          examples:
            serverError:
              value:
                timestamp: '2026-05-13T12:00:00Z'
                status: 500
                error: Internal Server Error
                message: An unexpected error occurred
                path: /api/v1/auth/me
                traceId: 7af92bc8e8a24c1d
```

---

## JWT Claims Structure

The access token includes the following claims:

```json
{
  "sub": "8f2b5f10-91c4-4c3f-84f0-392cbe4e2a21",
  "email": "john.doe@example.com",
  "roles": ["USER"],
  "permissions": ["PROFILE_READ"],
  "iss": "auth-service",
  "aud": "application-api",
  "iat": 1778670000,
  "exp": 1778670900,
  "jti": "82ad7d51-fc44-4f8a-9be6-59a95e71dca9"
}
```

| Claim | Description |
|-------|-------------|
| sub | User ID (subject) |
| email | User email address |
| roles | Array of user roles |
| permissions | Array of user permissions |
| iss | Token issuer (auth-service) |
| aud | Token audience (application-api) |
| iat | Issued at timestamp (Unix epoch) |
| exp | Expiration timestamp (Unix epoch) |
| jti | JWT ID (unique token identifier) |

---

## Security Headers

### Required Request Headers

| Header | Value | Description |
|--------|-------|-------------|
| Authorization | Bearer {access_token} | JWT access token for authenticated endpoints |
| Content-Type | application/json | Request content type |
| X-Correlation-ID | {uuid} | Optional correlation ID for request tracing |

### Response Headers

| Header | Description |
|--------|-------------|
| X-Correlation-ID | Request correlation ID for tracing |
| X-RateLimit-Limit | Maximum number of requests allowed |
| X-RateLimit-Remaining | Number of requests remaining |
| X-RateLimit-Reset | Unix timestamp when rate limit resets |
| Retry-After | Seconds to wait before retrying (for 429 responses) |

---

## Rate Limiting

| Endpoint | Limit | Window |
|----------|-------|--------|
| /api/v1/auth/login | 5 requests | 15 minutes |
| /api/v1/auth/register | 3 requests | 1 hour |
| /api/v1/auth/token/refresh | 10 requests | 5 minutes |
| Other endpoints | 100 requests | 1 minute |

---

## Error Codes

| HTTP Status | Error Code | Description |
|-------------|------------|-------------|
| 400 | BAD_REQUEST | Invalid request format or parameters |
| 400 | VALIDATION_ERROR | Input validation failed |
| 400 | PASSWORD_POLICY_VIOLATION | Password doesn't meet policy requirements |
| 401 | UNAUTHORIZED | Authentication failed or token invalid |
| 401 | TOKEN_EXPIRED | Access token has expired |
| 401 | TOKEN_REVOKED | Token has been revoked/blacklisted |
| 401 | INVALID_CREDENTIALS | Invalid email or password |
| 401 | REFRESH_TOKEN_REUSED | Refresh token replay detected |
| 403 | FORBIDDEN | Insufficient permissions |
| 403 | ACCOUNT_LOCKED | Account temporarily locked |
| 403 | ACCOUNT_DISABLED | Account is disabled |
| 403 | SESSION_REVOKED | Session revoked due to security event |
| 404 | NOT_FOUND | Resource not found |
| 409 | CONFLICT | Resource already exists (duplicate email) |
| 429 | RATE_LIMIT_EXCEEDED | Too many requests |
| 500 | INTERNAL_SERVER_ERROR | Unexpected server error |
| 503 | SERVICE_UNAVAILABLE | Service temporarily unavailable |

---

## Usage Examples

### 1. User Registration Flow

```bash
# Register new user
curl -X POST https://api.authservice.example.com/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@example.com",
    "password": "SecurePassword@123"
  }'

# Response
{
  "userId": "8f2b5f10-91c4-4c3f-84f0-392cbe4e2a21",
  "email": "john.doe@example.com",
  "status": "ACTIVE",
  "message": "User registered successfully"
}
```

### 2. User Login Flow

```bash
# Login
curl -X POST https://api.authservice.example.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john.doe@example.com",
    "password": "SecurePassword@123"
  }'

# Response
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "89ca9d6e-d1c3-4df3-b6c7-8d87a1b29e11",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "userId": "8f2b5f10-91c4-4c3f-84f0-392cbe4e2a21",
    "email": "john.doe@example.com",
    "roles": ["USER"]
  }
}
```

### 3. Access Protected Resource

```bash
# Get current user details
curl -X GET https://api.authservice.example.com/api/v1/auth/me \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."

# Response
{
  "userId": "8f2b5f10-91c4-4c3f-84f0-392cbe4e2a21",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "roles": ["USER"],
  "permissions": ["PROFILE_READ"],
  "status": "ACTIVE",
  "createdAt": "2026-05-01T10:00:00Z",
  "updatedAt": "2026-05-13T12:00:00Z"
}
```

### 4. Refresh Token Flow

```bash
# Refresh access token
curl -X POST https://api.authservice.example.com/api/v1/auth/token/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "89ca9d6e-d1c3-4df3-b6c7-8d87a1b29e11"
  }'

# Response
{
  "accessToken": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "f3a9b172-77cc-4c9f-8f44-92710e0a9d19",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

### 5. Logout Flow

```bash
# Logout and invalidate session
curl -X POST https://api.authservice.example.com/api/v1/auth/logout \
  -H "Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."

# Response: 204 No Content
```

---

## API Versioning Strategy

The API uses URI versioning with the `/api/v1/` prefix. Breaking changes will result in a new version (`/api/v2/`).

### Backward Compatibility Rules

1. New optional fields can be added to responses
2. New optional parameters can be added to requests
3. New endpoints can be added
4. Existing fields cannot be removed or renamed
5. Field types cannot be changed
6. Breaking changes require a new API version

### Version Support

- **Current Version**: v1 (fully supported)
- **Support Policy**: Each version supported for minimum 12 months after new version release
- **Deprecation Notice**: 6 months' advance notice before version retirement

---

## Security Best Practices

### For Client Applications

1. **Store tokens securely**
   - Use HTTP-only, Secure, SameSite cookies for web applications
   - Use secure storage (Keychain/Keystore) for mobile applications
   - Never store tokens in localStorage or sessionStorage

2. **Implement token refresh logic**
   - Automatically refresh token before expiration
   - Handle refresh failures with re-authentication

3. **Handle security errors**
   - Clear stored tokens on 401 responses
   - Redirect to login on authentication failures
   - Log out user on session revocation

4. **Use HTTPS only**
   - Never send tokens over unencrypted connections

5. **Add request correlation IDs**
   - Include X-Correlation-ID header for better debugging

### For Backend Services

1. **Validate tokens on every request**
   - Check signature, expiration, and blacklist status
   - Extract and validate claims

2. **Implement RBAC checks**
   - Verify user has required roles/permissions
   - Fail securely when authorization fails

3. **Use provided error codes**
   - Handle specific error codes appropriately
   - Implement proper retry logic with exponential backoff

---

## Compliance

This API complies with:

- **OWASP Top 10**: Protection against common web vulnerabilities
- **OWASP API Security Top 10**: API-specific security controls
- **OAuth 2.0 Best Current Practices**: Token management and security
- **GDPR**: Data privacy and right to erasure
- **SOC 2 Type II**: Security, availability, and confidentiality controls

---

## Support

For API support, contact:
- **Email**: platform@example.com
- **Documentation**: https://docs.authservice.example.com
- **Status Page**: https://status.authservice.example.com

