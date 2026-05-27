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
