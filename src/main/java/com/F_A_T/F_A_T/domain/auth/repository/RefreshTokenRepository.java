package com.F_A_T.F_A_T.domain.auth.repository;

import com.F_A_T.F_A_T.domain.auth.entity.RefreshToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    @Query("select t from RefreshToken t where t.tokenHash = :hash and t.revokedAt is null and t.expiresAt > :now")
    Optional<RefreshToken> findActive(@Param("hash") String hash, @Param("now") LocalDateTime now);

    // A return value of 1 claims this token once for rotation/logout; 0 rejects reuse.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update RefreshToken t set t.revokedAt = :now where t.tokenHash = :hash and t.revokedAt is null and t.expiresAt > :now")
    int revokeActive(@Param("hash") String hash, @Param("now") LocalDateTime now);
}
