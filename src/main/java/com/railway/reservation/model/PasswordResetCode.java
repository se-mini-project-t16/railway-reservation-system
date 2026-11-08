package com.railway.reservation.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** A password reset in progress for one account. The code is stored only as a BCrypt hash. */
@Entity
@Table(name = "password_reset_codes")
public class PasswordResetCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    /** Null once the code is used up by too many wrong guesses; a new code must be requested. */
    @Column(name = "code_hash")
    private String codeHash;

    @Column(name = "code_expires_at", nullable = false)
    private LocalDateTime codeExpiresAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "codes_sent", nullable = false)
    private int codesSent;

    @Column(name = "last_sent_at", nullable = false)
    private LocalDateTime lastSentAt;

    protected PasswordResetCode() {}

    public PasswordResetCode(Long userId, LocalDateTime now) {
        this.userId = userId;
        this.lastSentAt = now;
        this.codeExpiresAt = now;
    }

    public void newCode(String codeHash, LocalDateTime now, LocalDateTime expiresAt, boolean newWindow) {
        this.codeHash = codeHash;
        this.codeExpiresAt = expiresAt;
        this.failedAttempts = 0;
        this.codesSent = newWindow ? 1 : codesSent + 1;
        this.lastSentAt = now;
    }

    public void recordFailedAttempt(int maxAttempts) {
        failedAttempts++;
        if (failedAttempts >= maxAttempts) codeHash = null;
    }

    public Long getUserId() { return userId; }
    public String getCodeHash() { return codeHash; }
    public LocalDateTime getCodeExpiresAt() { return codeExpiresAt; }
    public int getCodesSent() { return codesSent; }
    public LocalDateTime getLastSentAt() { return lastSentAt; }
}
