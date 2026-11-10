package com.F_A_T.F_A_T.domain.auth.entity;

import jakarta.persistence.*;
import com.F_A_T.F_A_T.global.exception.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "email_verifications")
@Getter
@NoArgsConstructor
public class EmailVerification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_id")
    private Long id;
    @Column(nullable = false)
    private String email;
    @Column(nullable = false, length = 32)
    private String purpose = "SIGNUP";
    @Column(name = "code_hash", nullable = false, columnDefinition = "CHAR(64)")
    private String codeHash;
    @Column(name = "code_expires_at", nullable = false)
    private LocalDateTime codeExpiresAt;
    @Column(nullable = false)
    private int attempts;
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    @Column(name = "signup_token_hash", unique = true, columnDefinition = "CHAR(64)")
    private String signupTokenHash;
    @Column(name = "signup_token_expires_at")
    private LocalDateTime signupTokenExpiresAt;
    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    public EmailVerification(String email, String codeHash, LocalDateTime codeExpiresAt) {
        this.email = email;
        this.codeHash = codeHash;
        this.codeExpiresAt = codeExpiresAt;
    }

    public EmailVerification(String email,String codeHash,LocalDateTime expiry,String purpose) { this(email,codeHash,expiry);this.purpose=purpose; }

    // Caller must compare a keyed digest and hold the repository's row lock.
    // These are persistence operations, not an email authentication endpoint.
    public void recordFailedAttempt() {
        if (attempts >= 5) throw new ApiException(ErrorCode.TOO_MANY_ATTEMPTS);
        attempts++;
    }

    public void markVerified(String signupTokenHash, LocalDateTime expiresAt, LocalDateTime now) {
        if (!codeExpiresAt.isAfter(now) || attempts >= 5 || verifiedAt != null) {
            throw new ApiException(ErrorCode.INVALID_VERIFICATION_CODE);
        }
        this.verifiedAt = now;
        this.signupTokenHash = signupTokenHash;
        this.signupTokenExpiresAt = expiresAt;
    }
}
