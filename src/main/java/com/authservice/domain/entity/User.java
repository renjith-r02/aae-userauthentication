package com.authservice.domain.entity;

import com.authservice.domain.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User entity representing a user account in the system
 * Requirement: AUTH-FR-001 (User Registration), AUTH-FR-002 (User Authentication)
 * Section 7.1: User Table
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email", unique = true),
    @Index(name = "idx_user_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column
    private LocalDateTime lockedUntil;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime lastLoginAt;

    // Business methods

    /**
     * Check if user account is active
     * @return true if status is ACTIVE
     */
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    /**
     * Check if user account is currently locked
     * @return true if locked and lock time has not expired
     */
    public boolean isLocked() {
        if (lockedUntil == null) {
            return false;
        }
        if (LocalDateTime.now().isAfter(lockedUntil)) {
            // Auto-unlock if lock period has expired
            this.lockedUntil = null;
            this.failedLoginAttempts = 0;
            return false;
        }
        return true;
    }

    /**
     * Increment failed login attempts
     * Locks account after 5 failed attempts (AUTH-FR-002 requirement)
     */
    public void incrementFailedAttempts() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            lock(Duration.ofMinutes(30));
        }
    }

    /**
     * Reset failed login attempts to 0
     * Called after successful authentication
     */
    public void resetFailedAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    /**
     * Lock the user account for specified duration
     * @param duration Duration to lock the account
     */
    public void lock(Duration duration) {
        this.status = UserStatus.LOCKED;
        this.lockedUntil = LocalDateTime.now().plus(duration);
    }

    /**
     * Manually unlock the user account
     */
    public void unlock() {
        this.status = UserStatus.ACTIVE;
        this.lockedUntil = null;
        this.failedLoginAttempts = 0;
    }

    /**
     * Update last login timestamp
     * Called after successful authentication
     */
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }
}

