package com.F_A_T.F_A_T.domain.auth.entity;

import jakarta.persistence.*;
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

    // Caller must compare a keyed digest and hold the repository's row lock.
    // These are persistence operations, not an email authentication endpoint.
    public void recordFailedAttempt() {
        if (attempts >= 5) throw new IllegalStateException("인증 시도 횟수를 초과했습니다.");
        attempts++;
    }

    public void markVerified(String signupTokenHash, LocalDateTime expiresAt, LocalDateTime now) {
        if (!codeExpiresAt.isAfter(now) || attempts >= 5 || verifiedAt != null) {
            throw new IllegalStateException("사용할 수 없는 인증 요청입니다.");
        }
        this.verifiedAt = now;
        this.signupTokenHash = signupTokenHash;
        this.signupTokenExpiresAt = expiresAt;
    }
}
