package com.authservice.repository;

import com.authservice.domain.entity.User;
import com.authservice.domain.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for User entity
 * Requirement: AUTH-FR-001 (User Registration), AUTH-FR-002 (User Authentication)
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Find user by email address
     * @param email Email address
     * @return Optional containing user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Check if email already exists
     * @param email Email address
     * @return true if exists
     */
    boolean existsByEmail(String email);

    /**
     * Find users by status
     * @param status User status
     * @return List of users
     */
    List<User> findByStatus(UserStatus status);

    /**
     * Update failed login attempts
     * @param id User ID
     * @param attempts Number of attempts
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = :attempts WHERE u.id = :id")
    void updateFailedLoginAttempts(@Param("id") UUID id, @Param("attempts") Integer attempts);

    /**
     * Update last login timestamp
     * @param id User ID
     * @param timestamp Last login timestamp
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :timestamp WHERE u.id = :id")
    void updateLastLogin(@Param("id") UUID id, @Param("timestamp") LocalDateTime timestamp);
}

