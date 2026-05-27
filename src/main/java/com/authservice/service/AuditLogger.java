package com.authservice.service;
import com.authservice.domain.entity.AuditLog;
import com.authservice.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.UUID;
/**
 * Audit Logger Service
 * Requirement: Section 11 (Audit Logging Requirements)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogger {
    private final AuditLogRepository auditLogRepository;
                                      stration(UUID userId, String email) {
        log(userId, "USER_REGISTRATION", "user", null, null, 
            String.format("User registered: %s", email), null);
    }
    @Async
    public void logLoginSuccess(UUID userId, String ip, String userAgent) {
        log(userId, "LOGIN_SUCCESS", "authentication", ip, userAgent,
            "User logged in successfully", null);
    }
    @Async
    public void logLoginFailure(String email, String ip, String userAgent, String reason) {
        log(null, "LOGIN_FAILURE", "authentication", ip, userAgent,
            String.format("Login failed for %s: %s", email, reason), null);
    }
    @Async
    public void logTokenRefresh(UUID userId, UUID sessionId) {
        log(userId, "TOKEN_REFRESH", "token", null, null,
            String.format("Token refreshed for session: %s", sessionId), null);
    }
    @Async
    public void logLogout(UUID userId    public void logLog      log(userId, "LOGOUT", "authentication", null, null,
            String.format("User logged out, session: %s", sessionId), null);
    }
    @Async
    public void logReplayAttempt(UUID userId, UUID sessionId, UUID tokenFamilyId) {
        log(userId, "REPLAY_ATTACK_DETECTED", "security", null, null,
            String.format("Refresh token replay detected - Session: %s, Family: %s", sessionId, tokenFamilyId), null);
    }
    private void log(UUID userId, String action, String resource, String ip, 
                     String userAgent, String details, String correlationId) {
        try {
            AuditLog auditLog = AuditLog.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .action(action)
                .resource(resource)
                .ipAddress(ip)
                .userAgent(userAgent)
                .timestamp(LocalDateTime.now())
                .details(details)
                .correlationId(correlationId)
                .build();
            auditLogRepository.save(auditLog);
            log.debug("Audit log created: action={}, userId={}", action, userId);
        } catch (Exception e) {
            log.error("Failed to create audit log", e);
        }
    }
}
