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
