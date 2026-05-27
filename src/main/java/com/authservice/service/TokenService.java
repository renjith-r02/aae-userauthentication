package com.authservice.service;

import com.authservice.domain.entity.RefreshToken;
import com.authservice.domain.entity.Session;
import com.authservice.domain.enums.RefreshTokenStatus;
import com.authservice.dto.TokenResponse;
import com.authservice.dto.TokenValidationResponse;
import com.authservice.exception.*;
import com.authservice.repository.RefreshTokenRepository;
import com.authservice.repository.SessionRepository;
import com.authservice.security.JWTConfig;
import com.authservice.security.JWTManager;
import com.authservice.security.TokenClaims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Token Service
 * Requirement: AUTH-FR-003 (Token Validation), AUTH-FR-004 (Token Refresh)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TokenService {
    
    private final RefreshTokenRepository refreshTokenRepository;
    private final SessionRepository sessionRepository;
    private final JWTManager jwtManager;
    private final JWTConfig jwtConfig;
    private final RBACService rbacService;
    private final AuditLogger auditLogger;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String BLACKLIST_PREFIX = "auth:blacklist:";
    
    public String createRefreshToken(UUID userId, UUID sessionId) {
        log.debug("Creating refresh token for user: {}, session: {}", userId, sessionId);
        
        // Generate random token
        String tokenValue = UUID.randomUUID().toString();
        String tokenHash = passwordEncoder.encode(tokenValue);
        
        // Calculate expiration
        LocalDateTime expiresAt = LocalDateTime.now()
                .plus(jwtConfig.getRefreshTokenExpiry());
        
        // Create token family ID (for rotation tracking)
        UUID tokenFamilyId = UUID.randomUUID();
        
        // Save to database
        RefreshToken refreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash(tokenHash)
                .sessionId(sessionId)
                .tokenFamilyId(tokenFamilyId)
                .status(RefreshTokenStatus.ACTIVE)
                .expiresAt(expiresAt)
                .build();
        
        refreshTokenRepository.save(refreshToken);
        
        return tokenValue;
    }
    
    @Transactional
    public TokenResponse refreshToken(String refreshTokenValue) {
        log.info("Refresh token request");
        
        // Find all refresh tokens and check against hash
        List<RefreshToken> allTokens = refreshTokenRepository.findAll();
        RefreshToken matchedToken = null;
        
        for (RefreshToken token : allTokens) {
            if (passwordEncoder.matches(refreshTokenValue, token.getTokenHash())) {
                matchedToken = token;
                break;
            }
        }
        
        if (matchedToken == null) {
            log.warn("Refresh token not found");
            throw new TokenInvalidException("Invalid refresh token");
        }
        
        // Check if token is expired
        if (matchedToken.isExpired()) {
            log.warn("Refresh token expired");
            throw new TokenExpiredException();
        }
        
        // Check token status - detect replay attacks
        if (matchedToken.getStatus() == RefreshTokenStatus.ROTATED) {
            // Token reuse detected - revoke entire token family
            log.error("Refresh token replay detected for family: {}", matchedToken.getTokenFamilyId());
            revokeTokenFamily(matchedToken.getTokenFamilyId());
            auditLogger.logReplayAttempt(matchedToken.getUserId(), matchedToken.getSessionId(), 
                    matchedToken.getTokenFamilyId());
            throw new RefreshTokenReusedException(matchedToken.getSessionId());
        }
        
        if (matchedToken.getStatus() == RefreshTokenStatus.REVOKED) {
            log.warn("Refresh token has been revoked");
            throw new TokenRevokedException();
        }
        
        // Check session is still active
        Session session = sessionRepository.findById(matchedToken.getSessionId())
                .orElseThrow(() -> new TokenInvalidException("Session not found"));
        
        if (!session.isActive()) {
            log.warn("Session is not active");
            throw new TokenInvalidException("Session expired or revoked");
        }
        
        // Mark current token as rotated
        matchedToken.rotate();
        refreshTokenRepository.save(matchedToken);
        
        // Get user roles and permissions
        List<String> roles = rbacService.getUserRoles(matchedToken.getUserId());
        List<String> permissions = rbacService.getUserPermissions(matchedToken.getUserId());
        
        // Generate new access token
        TokenClaims claims = new TokenClaims();
        claims.setSub(matchedToken.getUserId());
        claims.setRoles(roles);
        claims.setPermissions(permissions);
        
        String newAccessToken = jwtManager.createAccessToken(claims);
        
        // Generate new refresh token (rotation)
        String newRefreshTokenValue = UUID.randomUUID().toString();
        String newTokenHash = passwordEncoder.encode(newRefreshTokenValue);
        
        RefreshToken newRefreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(matchedToken.getUserId())
                .tokenHash(newTokenHash)
                .sessionId(matchedToken.getSessionId())
                .tokenFamilyId(matchedToken.getTokenFamilyId()) // Same family
                .status(RefreshTokenStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plus(jwtConfig.getRefreshTokenExpiry()))
                .build();
        
        refreshTokenRepository.save(newRefreshToken);
        
        // Update session last seen
        session.updateLastSeen();
        sessionRepository.save(session);
        
        // Audit log
        auditLogger.logTokenRefresh(matchedToken.getUserId(), matchedToken.getSessionId());
        
        log.info("Token refreshed successfully for user: {}", matchedToken.getUserId());
        
        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(900) // 15 minutes
                .build();
    }
    
    public TokenValidationResponse validateToken(String token) {
        log.debug("Validating token");
        
        // Validate JWT
        TokenClaims claims = jwtManager.validateToken(token);
        
        // Check if token is blacklisted
        if (isTokenBlacklisted(claims.getJti())) {
            throw new TokenRevokedException();
        }
        
        return TokenValidationResponse.builder()
                .valid(true)
                .userId(claims.getSub())
                .email(claims.getEmail())
                .roles(claims.getRoles())
                .permissions(claims.getPermissions())
                .expiresAt(claims.getExp())
                .build();
    }
    
    public void blacklistToken(UUID tokenId, UUID userId) {
        String key = BLACKLIST_PREFIX + tokenId.toString();
        
        // Store in Redis with TTL matching token expiration
        // For simplicity, using 15 minutes (access token lifetime)
        redisTemplate.opsForValue().set(key, userId.toString(), 15, TimeUnit.MINUTES);
        
        log.debug("Token blacklisted: {}", tokenId);
    }
    
    public boolean isTokenBlacklisted(UUID tokenId) {
        String key = BLACKLIST_PREFIX + tokenId.toString();
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
    
    @Transactional
    private void revokeTokenFamily(UUID tokenFamilyId) {
        log.warn("Revoking token family: {}", tokenFamilyId);
        
        List<RefreshToken> familyTokens = refreshTokenRepository.findByTokenFamilyId(tokenFamilyId);
        for (RefreshToken token : familyTokens) {
            if (token.getStatus() == RefreshTokenStatus.ACTIVE || 
                token.getStatus() == RefreshTokenStatus.ROTATED) {
                token.revoke();
                refreshTokenRepository.save(token);
            }
        }
        
        // Revoke associated sessions
        if (!familyTokens.isEmpty()) {
            UUID sessionId = familyTokens.get(0).getSessionId();
            sessionRepository.findById(sessionId).ifPresent(session -> {
                session.revoke();
                sessionRepository.save(session);
            });
        }
    }
}

