package com.ledgerline.backend.repository;

import com.ledgerline.backend.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    /** Atomic single use: exactly one caller can flip an unused, unexpired token. Returns 1 on success. */
    @Modifying(clearAutomatically = true)
    @Query("update PasswordResetToken t set t.usedAt = :now where t.tokenHash = :h and t.usedAt is null and t.expiresAt > :now")
    int claim(@Param("h") String hash, @Param("now") Instant now);

    @Modifying(clearAutomatically = true)
    @Query("update PasswordResetToken t set t.usedAt = :now where t.user.id = :uid and t.usedAt is null")
    int invalidateOpen(@Param("uid") Long userId, @Param("now") Instant now);
}