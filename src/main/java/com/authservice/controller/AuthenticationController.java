package com.authservice.controller;

import com.authservice.dto.*;
import com.authservice.exception.ErrorResponse;
import com.authservice.security.RateLimiter;
import com.authservice.security.TokenClaims;
import com.authservice.service.AuthenticationService;
import com.authservice.service.TokenService;
import com.authservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Authentication Controller
 * Implements all authentication endpoints
 * Requirements: AUTH-FR-001 through AUTH-FR-005
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthenticationController {
    
    private final UserService userService;
    private final AuthenticationService authenticationService;
    private final TokenService tokenService;
    private final RateLimiter rateLimiter;
    
    @PostMapping("/register")
    @Operation(
        summary = "Register new user",
        description = "Create a new user account with email and password",
        responses = {
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input", 
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Email already exists",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("Registration request for email: {}", request.getEmail());
        
        // Rate limiting by IP
        String clientIp = getClientIp(httpRequest);
        rateLimiter.checkRateLimit(clientIp, RateLimiter.RateLimitType.REGISTRATION);
        
        // Rate limiting by email
        rateLimiter.checkRateLimit(request.getEmail(), RateLimiter.RateLimitType.REGISTRATION);
        
        RegisterResponse response = userService.createUser(request);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @PostMapping("/login")
    @Operation(
        summary = "Authenticate user",
        description = "Login with email and password to receive JWT tokens",
        responses = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Account locked or disabled",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "429", description = "Too many login attempts",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("Login request for email: {}", request.getEmail());
        
        // Rate limiting by IP
        String clientIp = getClientIp(httpRequest);
        rateLimiter.checkRateLimit(clientIp, RateLimiter.RateLimitType.LOGIN);
        
        // Rate limiting by email
        rateLimiter.checkRateLimit(request.getEmail(), RateLimiter.RateLimitType.LOGIN);
        
        LoginResponse response = authenticationService.authenticate(request, httpRequest);
        
        // Reset rate limit on successful login
        rateLimiter.resetRateLimit(request.getEmail(), RateLimiter.RateLimitType.LOGIN);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/token/validate")
    @Operation(
        summary = "Validate JWT token",
        description = "Validate the provided JWT access token",
        responses = {
            @ApiResponse(responseCode = "200", description = "Token is valid"),
            @ApiResponse(responseCode = "401", description = "Token is invalid or expired",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    public ResponseEntity<TokenValidationResponse> validateToken(
            @RequestHeader("Authorization") String authHeader) {
        
        String token = extractToken(authHeader);
        TokenValidationResponse response = tokenService.validateToken(token);
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/token/refresh")
    @Operation(
        summary = "Refresh access token",
        description = "Generate new access token using refresh token",
        responses = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "Token reuse detected - session revoked",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    public ResponseEntity<TokenResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("Token refresh request");
        
        // Rate limiting by IP
        String clientIp = getClientIp(httpRequest);
        rateLimiter.checkRateLimit(clientIp, RateLimiter.RateLimitType.REFRESH);
        
        TokenResponse response = tokenService.refreshToken(request.getRefreshToken());
        
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/logout")
    @Operation(
        summary = "Logout user",
        description = "Invalidate current session and tokens",
        responses = {
            @ApiResponse(responseCode = "200", description = "Logout successful"),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        }
    )
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authHeader) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UUID userId = UUID.fromString(authentication.getName());
        
        String token = extractToken(authHeader);
        
        authenticationService.logout(userId, token);
        
        return ResponseEntity.ok().build();
    }
    
    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Invalid Authorization header");
        }
        return authHeader.substring(7);
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

