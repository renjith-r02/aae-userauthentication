package com.authservice.service;

import com.authservice.domain.entity.RefreshToken;
import com.authservice.domain.entity.Session;
import com.authservice.domain.entity.User;
import com.authservice.domain.enums.RefreshTokenStatus;
import com.authservice.domain.enums.SessionStatus;
import com.authservice.dto.*;
import com.authservice.exception.*;
import com.authservice.repository.RefreshTokenRepository;
import com.authservice.repository.SessionRepository;
import com.authservice.repository.UserRepository;
import com.authservice.security.JWTManager;
import com.authservice.security.PasswordManager;
import com.authservice.security.TokenClaims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Authentication Service
 * Requirement: AUTH-FR-002 (User Authentication), AUTH-FR-005 (Session Invalidation)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuthenticationService {
    
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordManager passwordManager;
    private final JWTManager jwtManager;
    private final RBACService rbacService;
    private final AuditLogger auditLogger;
    private final TokenService tokenService;
    
    @Transactional
    public LoginResponse authenticate(LoginRequest request, HttpServletRequest httpRequest) {
        log.info("Authentication attempt for email: {}", request.getEmail());
        
        String ipAddress = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        // Find user
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    auditLogger.logLoginFailure(request.getEmail(), ipAddress, userAgent, "User not found");
                    return new InvalidCredentialsException();
                });
        
        // Check if account is locked
        if (user.isLocked()) {
            auditLogger.logLoginFailure(request.getEmail(), ipAddress, userAgent, "Account locked");
            throw new AccountLockedException(user.getLockedUntil());
        }
        
        // Check if account is active
        if (!user.isActive()) {
            auditLogger.logLoginFailure(request.getEmail(), ipAddress, userAgent, "Account not active");
            throw new AuthenticationException("Account is not active");
        }
        
        // Verify password
        if (!passwordManager.verifyPassword(request.getPassword(), user.getPasswordHash())) {
            user.incrementFailedAttempts();
            userRepository.save(user);
            auditLogger.logLoginFailure(request.getEmail(), ipAddress, userAgent, "Invalid password");
            throw new InvalidCredentialsException();
        }
        
        // Reset failed attempts on successful authentication
        user.resetFailedAttempts();
        user.updateLastLogin();
        userRepository.save(user);
        
        // Create session
        Session session = Session.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .status(SessionStatus.ACTIVE)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        sessionRepository.save(session);
        
        // Get roles and permissions
        List<String> roles = rbacService.getUserRoles(user.getId());
        List<String> permissions = rbacService.getUserPermissions(user.getId());
        
        // Generate tokens
        TokenClaims claims = new TokenClaims();
        claims.setSub(user.getId());
        claims.setEmail(user.getEmail());
        claims.setRoles(roles);
        claims.setPermissions(permissions);
        
        String accessToken = jwtManager.createAccessToken(claims);
        String refreshToken = tokenService.createRefreshToken(user.getId(), session.getId());
        
        // Audit log
        auditLogger.logLoginSuccess(user.getId(), ipAddress, userAgent);
        
        log.info("User authenticated successfully: {}", user.getId());
        
        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(900) // 15 minutes in seconds
                .user(UserInfo.builder()
                        .userId(user.getId())
                        .email(user.getEmail())
                        .roles(roles)
                        .build())
                .build();
    }
    
    @Transactional
    public void logout(UUID userId, String accessToken) {
        log.info("Logout request for user: {}", userId);
        
        // Extract token ID
        UUID tokenId = jwtManager.extractTokenId(accessToken);
        
        // Find active sessions for user
        List<Session> sessions = sessionRepository.findByUserId(userId);
        UUID sessionId = null;
        
        for (Session session : sessions) {
            if (session.isActive()) {
                session.revoke();
                sessionRepository.save(session);
                sessionId = session.getId();
                
                // Revoke all refresh tokens for this session
                List<RefreshToken> refreshTokens = refreshTokenRepository.findBySessionId(session.getId());
                for (RefreshToken rt : refreshTokens) {
                    if (rt.getStatus() == RefreshTokenStatus.ACTIVE) {
                        rt.revoke();
                        refreshTokenRepository.save(rt);
                    }
                }
            }
        }
        
        // Blacklist the access token
        tokenService.blacklistToken(tokenId, userId);
        
        // Audit log
        auditLogger.logLogout(userId, sessionId);
        
        log.info("User logged out successfully: {}", userId);
    }
    
    public void validateCredentials(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException());
        
        if (!passwordManager.verifyPassword(password, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

