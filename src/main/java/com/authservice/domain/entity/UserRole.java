package com.authservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * UserRole entity representing the many-to-many relationship between User and Role
 * Requirement: AUTH-FR-007 (Role-Based Access Control)
 * Section 7.3: User Role Table
 */
@Entity
@Table(name = "user_roles", indexes = {
    @Index(name = "idx_user_role_user_id", columnList = "user_id"),
    @Index(name = "idx_user_role_role_id", columnList = "role_id")
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "role_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime assignedAt;
}

