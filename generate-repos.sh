#!/bin/bash

# Script to generate all remaining Spring Boot application files
# This creates repositories, services, controllers, DTOs, security components, etc.

BASE_PATH="/Users/renjithr/Downloads/Renjiths_Programming/GITHub/AuthService2/src/main/java/com/authservice"

echo "Generating Spring Boot application files..."

# Create remaining repositories
cat > "$BASE_PATH/repository/RoleRepository.java" << 'EOF'
package com.authservice.repository;

import com.authservice.domain.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
    Optional<Role> findByName(String name);
    boolean existsByName(String name);
}
EOF

cat > "$BASE_PATH/repository/PermissionRepository.java" << 'EOF'
package com.authservice.repository;

import com.authservice.domain.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {
    Optional<Permission> findByName(String name);
    List<Permission> findByResource(String resource);
}
EOF

cat > "$BASE_PATH/repository/UserRoleRepository.java" << 'EOF'
package com.authservice.repository;

import com.authservice.domain.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {
    List<UserRole> findByUserId(UUID userId);
    List<UserRole> findByRoleId(UUID roleId);
    boolean existsByUserIdAndRoleId(UUID userId, UUID roleId);
    void deleteByUserIdAndRoleId(UUID userId, UUID roleId);

    @Query("SELECT ur FROM UserRole ur WHERE ur.userId = :userId")
    List<UserRole> findAllByUserId(@Param("userId") UUID userId);
}
EOF

cat > "$BASE_PATH/repository/RolePermissionRepository.java" << 'EOF'
package com.authservice.repository;

import com.authservice.domain.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, UUID> {
    List<RolePermission> findByRoleId(UUID roleId);
    List<RolePermission> findByPermissionId(UUID permissionId);
}
EOF

cat > "$BASE_PATH/repository/RefreshTokenRepository.java" << 'EOF'
package com.authservice.repository;

import com.authservice.domain.entity.RefreshToken;
import com.authservice.domain.enums.RefreshTokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    List<RefreshToken> findByUserId(UUID userId);
    List<RefreshToken> findBySessionId(UUID sessionId);
    List<RefreshToken> findByTokenFamilyId(UUID tokenFamilyId);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.status = :status WHERE rt.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") RefreshTokenStatus status);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}
EOF

cat > "$BASE_PATH/repository/SessionRepository.java" << 'EOF'
package com.authservice.repository;

import com.authservice.domain.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {
    List<Session> findByUserId(UUID userId);

    @Modifying
    @Query("UPDATE Session s SET s.lastSeenAt = :timestamp WHERE s.id = :id")
    void updateLastSeen(@Param("id") UUID id, @Param("timestamp") LocalDateTime timestamp);

    @Modifying
    @Query("UPDATE Session s SET s.status = 'REVOKED', s.revokedAt = :revokedAt WHERE s.id = :id")
    void revoke(@Param("id") UUID id, @Param("revokedAt") LocalDateTime revokedAt);

    @Modifying
    @Query("DELETE FROM Session s WHERE s.status = 'EXPIRED'")
    int deleteExpired();
}
EOF

cat > "$BASE_PATH/repository/AuditLogRepository.java" << 'EOF'
package com.authservice.repository;

import com.authservice.domain.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    Page<AuditLog> findByUserId(UUID userId, Pageable pageable);
    Page<AuditLog> findByAction(String action, Pageable pageable);
    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    @Modifying
    @Query("DELETE FROM AuditLog al WHERE al.timestamp < :date")
    int deleteOlderThan(@Param("date") LocalDateTime date);
}
EOF

echo "✅ All repositories created successfully!"

