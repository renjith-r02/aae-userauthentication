#!/bin/bash

# Comprehensive script to generate all Spring Boot application components
# Creates DTOs, Services, Controllers, Security Components, Exceptions, Configurations

BASE_PATH="/Users/renjithr/Downloads/Renjiths_Programming/GITHub/AuthService2/src/main/java/com/authservice"
TEST_PATH="/Users/renjithr/Downloads/Renjiths_Programming/GITHub/AuthService2/src/test/java/com/authservice"
RESOURCES_PATH="/Users/renjithr/Downloads/Renjiths_Programming/GITHub/AuthService2/src/main/resources"
K8S_PATH="/Users/renjithr/Downloads/Renjiths_Programming/GITHub/AuthService2/k8s"
JMETER_PATH="/Users/renjithr/Downloads/Renjiths_Programming/GITHub/AuthService2/jmeter"

# Create necessary directories
mkdir -p "$TEST_PATH/service" "$TEST_PATH/controller" "$TEST_PATH/security" "$TEST_PATH/integration"
mkdir -p "$RESOURCES_PATH/db/migration"
mkdir -p "$K8S_PATH" "$JMETER_PATH"

echo "🚀 Generating Spring Boot Application Components..."

##############################################################################
# PART 1: DTOs (Request/Response Models)
##############################################################################
echo "📦 Creating DTOs..."

cat > "$BASE_PATH/dto/RegisterRequest.java" << 'EOF'
package com.authservice.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Size(max = 255)
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 12, message = "Password must be at least 12 characters")
    private String password;
}
EOF

cat > "$BASE_PATH/dto/RegisterResponse.java" << 'EOF'
package com.authservice.dto;

import com.authservice.domain.enums.UserStatus;
import lombok.*;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterResponse {
    private UUID userId;
    private String email;
    private UserStatus status;
    private String message;
}
EOF

cat > "$BASE_PATH/dto/LoginRequest.java" << 'EOF'
package com.authservice.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;
}
EOF

cat > "$BASE_PATH/dto/LoginResponse.java" << 'EOF'
package com.authservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private int expiresIn;
    private UserInfo user;
}
EOF

cat > "$BASE_PATH/dto/UserInfo.java" << 'EOF'
package com.authservice.dto;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInfo {
    private UUID userId;
    private String email;
    private List<String> roles;
}
EOF

cat > "$BASE_PATH/dto/TokenValidationResponse.java" << 'EOF'
package com.authservice.dto;

import lombok.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenValidationResponse {
    private boolean valid;
    private UUID userId;
    private String email;
    private List<String> roles;
    private List<String> permissions;
    private Instant expiresAt;
}
EOF

cat > "$BASE_PATH/dto/RefreshTokenRequest.java" << 'EOF'
package com.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
EOF

cat > "$BASE_PATH/dto/TokenResponse.java" << 'EOF'
package com.authservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private int expiresIn;
}
EOF

cat > "$BASE_PATH/dto/UserResponse.java" << 'EOF'
package com.authservice.dto;

import com.authservice.domain.enums.UserStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private List<String> roles;
    private List<String> permissions;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
EOF

cat > "$BASE_PATH/dto/ErrorResponse.java" << 'EOF'
package com.authservice.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
    private String traceId;
    private List<FieldError> fieldErrors;
}
EOF

cat > "$BASE_PATH/dto/FieldError.java" << 'EOF'
package com.authservice.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldError {
    private String field;
    private String message;
}
EOF

cat > "$BASE_PATH/dto/HealthResponse.java" << 'EOF'
package com.authservice.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthResponse {
    private String status;
    private Map<String, ComponentHealth> components;
    private LocalDateTime timestamp;
}
EOF

cat > "$BASE_PATH/dto/ComponentHealth.java" << 'EOF'
package com.authservice.dto;

import lombok.*;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComponentHealth {
    private String status;
    private Map<String, Object> details;
}
EOF

echo "✅ DTOs created successfully!"

##############################################################################
# PART 2: Exception Classes
##############################################################################
echo "⚠️  Creating Exception classes..."

cat > "$BASE_PATH/exception/ServiceException.java" << 'EOF'
package com.authservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class ServiceException extends RuntimeException {
    private final String errorCode;
    private final HttpStatus httpStatus;
    private final String traceId;

    protected ServiceException(String message, String errorCode, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
        this.traceId = java.util.UUID.randomUUID().toString();
    }
}
EOF

cat > "$BASE_PATH/exception/AuthenticationException.java" << 'EOF'
package com.authservice.exception;

import org.springframework.http.HttpStatus;

public class AuthenticationException extends ServiceException {
    public AuthenticationException(String message) {
        super(message, "AUTH_001", HttpStatus.UNAUTHORIZED);
    }

    public AuthenticationException(String message, String errorCode) {
        super(message, errorCode, HttpStatus.UNAUTHORIZED);
    }
}
EOF

cat > "$BASE_PATH/exception/InvalidCredentialsException.java" << 'EOF'
package com.authservice.exception;

public class InvalidCredentialsException extends AuthenticationException {
    public InvalidCredentialsException() {
        super("Invalid email or password", "AUTH_002");
    }
}
EOF

cat > "$BASE_PATH/exception/TokenInvalidException.java" << 'EOF'
package com.authservice.exception;

public class TokenInvalidException extends AuthenticationException {
    public TokenInvalidException(String reason) {
        super("Invalid token: " + reason, "AUTH_003");
    }
}
EOF

cat > "$BASE_PATH/exception/TokenExpiredException.java" << 'EOF'
package com.authservice.exception;

public class TokenExpiredException extends AuthenticationException {
    public TokenExpiredException() {
        super("Token has expired", "AUTH_004");
    }
}
EOF

cat > "$BASE_PATH/exception/TokenRevokedException.java" << 'EOF'
package com.authservice.exception;

public class TokenRevokedException extends AuthenticationException {
    public TokenRevokedException() {
        super("Token has been revoked", "AUTH_005");
    }
}
EOF

cat > "$BASE_PATH/exception/AccountLockedException.java" << 'EOF'
package com.authservice.exception;

import lombok.Getter;
import java.time.LocalDateTime;

@Getter
public class AccountLockedException extends AuthenticationException {
    private final LocalDateTime lockedUntil;

    public AccountLockedException(LocalDateTime lockedUntil) {
        super("Account temporarily locked due to multiple failed login attempts", "AUTH_006");
        this.lockedUntil = lockedUntil;
    }
}
EOF

cat > "$BASE_PATH/exception/RefreshTokenReusedException.java" << 'EOF'
package com.authservice.exception;

import java.util.UUID;

public class RefreshTokenReusedException extends AuthenticationException {
    public RefreshTokenReusedException(UUID sessionId) {
        super("Refresh token has already been used. Session: " + sessionId, "AUTH_007");
    }
}
EOF

cat > "$BASE_PATH/exception/AuthorizationException.java" << 'EOF'
package com.authservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AuthorizationException extends ServiceException {
    private final String requiredPermission;

    public AuthorizationException(String permission) {
        super("Insufficient permissions. Required: " + permission, "AUTHZ_001", HttpStatus.FORBIDDEN);
        this.requiredPermission = permission;
    }
}
EOF

cat > "$BASE_PATH/exception/ValidationException.java" << 'EOF'
package com.authservice.exception;

import com.authservice.dto.FieldError;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import java.util.List;

@Getter
public class ValidationException extends ServiceException {
    private final List<FieldError> fieldErrors;

    public ValidationException(List<FieldError> fieldErrors) {
        super("Validation failed", "VALID_001", HttpStatus.BAD_REQUEST);
        this.fieldErrors = fieldErrors;
    }
}
EOF

cat > "$BASE_PATH/exception/ConflictException.java" << 'EOF'
package com.authservice.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends ServiceException {
    public ConflictException(String message) {
        super(message, "CONFLICT_001", HttpStatus.CONFLICT);
    }
}
EOF

cat > "$BASE_PATH/exception/ResourceNotFoundException.java" << 'EOF'
package com.authservice.exception;

import org.springframework.http.HttpStatus;
import java.util.UUID;

public class ResourceNotFoundException extends ServiceException {
    public ResourceNotFoundException(String resource, UUID id) {
        super(resource + " not found with id: " + id, "NOT_FOUND_001", HttpStatus.NOT_FOUND);
    }
}
EOF

cat > "$BASE_PATH/exception/RateLimitExceededException.java" << 'EOF'
package com.authservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class RateLimitExceededException extends ServiceException {
    private final int retryAfterSeconds;

    public RateLimitExceededException(int retryAfter) {
        super("Rate limit exceeded. Please try again later", "RATE_LIMIT_001", HttpStatus.TOO_MANY_REQUESTS);
        this.retryAfterSeconds = retryAfter;
    }
}
EOF

echo "✅ Exception classes created successfully!"

echo "🎉 All core application components generated successfully!"
echo "Next steps:"
echo "  1. Review generated files"
echo "  2. Run: chmod +x generate-services.sh"
echo "  3. Run: ./generate-services.sh (creates services, controllers, security)"

