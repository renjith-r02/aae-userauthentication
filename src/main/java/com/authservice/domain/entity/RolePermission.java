package com.authservice.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * RolePermission entity representing the many-to-many relationship between Role and Permission
 * Requirement: AUTH-FR-007 (Role-Based Access Control)
 */
@Entity
@Table(name = "role_permissions", indexes = {
    @Index(name = "idx_role_permission_role_id", columnList = "role_id"),
    @Index(name = "idx_role_permission_permission_id", columnList = "permission_id")
}, uniqueConstraints = {
    @UniqueConstraint(columnNames = {"role_id", "permission_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    @Column(name = "permission_id", nullable = false)
    private UUID permissionId;
}

