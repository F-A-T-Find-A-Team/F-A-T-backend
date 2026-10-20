package com.F_A_T.F_A_T.domain.auth.repository;

import com.F_A_T.F_A_T.domain.auth.entity.EmailVerification;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from EmailVerification v where v.id = :id")
    Optional<EmailVerification> findForUpdate(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update EmailVerification v set v.consumedAt = :now where v.signupTokenHash = :hash and v.verifiedAt is not null and v.consumedAt is null and v.signupTokenExpiresAt > :now")
    int consumeSignupToken(@Param("hash") String hash, @Param("now") LocalDateTime now);
}
