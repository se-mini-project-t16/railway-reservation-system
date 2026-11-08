package com.railway.reservation.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A sign-up waiting for its email to be confirmed. The account is created only when the
 * code sent to the address is entered; until then nothing exists in the users table.
 * The code is stored as a BCrypt hash, never in plain text.
 */
@Entity
@Table(name = "pending_registrations")
public class PendingRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /** Null once the code is used up by too many wrong guesses; a new code must be requested. */
    @Column(name = "code_hash")
    private String codeHash;

    @Column(name = "code_expires_at", nullable = false)
    private LocalDateTime codeExpiresAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    /** Codes sent in the current window, to stop an address being flooded. */
    @Column(name = "codes_sent", nullable = false)
    private int codesSent;

    @Column(name = "last_sent_at", nullable = false)
    private LocalDateTime lastSentAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected PendingRegistration() {}

    public PendingRegistration(String email, LocalDateTime now) {
        this.email = email;
        this.createdAt = now;
        this.lastSentAt = now;
        this.codeExpiresAt = now;
    }

    public void setDetails(String name, String phone, String passwordHash) {
        this.name = name;
        this.phone = phone;
        this.passwordHash = passwordHash;
    }

    public void newCode(String codeHash, LocalDateTime now, LocalDateTime expiresAt, boolean newWindow) {
        this.codeHash = codeHash;
        this.codeExpiresAt = expiresAt;
        this.failedAttempts = 0;
        this.codesSent = newWindow ? 1 : codesSent + 1;
        this.lastSentAt = now;
    }

    public int recordFailedAttempt(int maxAttempts) {
        failedAttempts++;
        if (failedAttempts >= maxAttempts) codeHash = null;
        return failedAttempts;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getPasswordHash() { return passwordHash; }
    public String getCodeHash() { return codeHash; }
    public LocalDateTime getCodeExpiresAt() { return codeExpiresAt; }
    public int getFailedAttempts() { return failedAttempts; }
    public int getCodesSent() { return codesSent; }
    public LocalDateTime getLastSentAt() { return lastSentAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
