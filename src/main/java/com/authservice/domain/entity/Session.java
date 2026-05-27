package com.authservice.domain.entity;

import com.authservice.domain.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Session entity representing a user session
 * Requirement: AUTH-FR-005 (Session Invalidation)
 * Section 7.6: Session Table
 */
@Entity
@Table(name = "sessions", indexes = {
    @Index(name = "idx_session_user_id", columnList = "user_id"),
    @Index(name = "idx_session_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SessionStatus status = SessionStatus.ACTIVE;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime lastSeenAt;

    @Column
    private LocalDateTime revokedAt;

    // Business methods

    /**
     * Check if session is currently active
     * @return true if status is ACTIVE
     */
    public boolean isActive() {
        return status == SessionStatus.ACTIVE;
    }

    /**
     * Update last seen timestamp
     */
    public void updateLastSeen() {
        this.lastSeenAt = LocalDateTime.now();
    }

    /**
     * Revoke the session
     */
    public void revoke() {
        this.status = SessionStatus.REVOKED;
        this.revokedAt = LocalDateTime.now();
    }
}

