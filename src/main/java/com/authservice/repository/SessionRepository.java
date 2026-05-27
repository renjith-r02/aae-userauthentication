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
