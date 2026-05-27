package com.authservice.domain.entity;

import com.authservice.domain.enums.RefreshTokenStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * RefreshToken entity representing a refresh token with rotation support
 * Requirement: AUTH-FR-004 (Refresh Token Flow with rotation and replay detection)
 * Section 7.5: Refresh Token Table
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_token_hash", columnList = "token_hash", unique = true),
    @Index(name = "idx_refresh_token_user_id", columnList = "user_id"),
    @Index(name = "idx_refresh_token_session_id", columnList = "session_id"),
    @Index(name = "idx_refresh_token_family_id", columnList = "token_family_id"),
    @Index(name = "idx_refresh_token_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "token_family_id", nullable = false)
    private UUID tokenFamilyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RefreshTokenStatus status = RefreshTokenStatus.ACTIVE;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column
    private LocalDateTime rotatedAt;

    @Column
    private LocalDateTime revokedAt;

    // Business methods

    /**
     * Check if token is valid (active and not expired)
     * @return true if valid
     */
    public boolean isValid() {
        return status == RefreshTokenStatus.ACTIVE && !isExpired();
    }

    /**
     * Check if token has expired
     * @return true if expired
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Mark token as rotated (used in token rotation)
     */
    public void rotate() {
        this.status = RefreshTokenStatus.ROTATED;
        this.rotatedAt = LocalDateTime.now();
    }

    /**
     * Revoke the token (security event or explicit logout)
     */
    public void revoke() {
        this.status = RefreshTokenStatus.REVOKED;
        this.revokedAt = LocalDateTime.now();
    }
}

