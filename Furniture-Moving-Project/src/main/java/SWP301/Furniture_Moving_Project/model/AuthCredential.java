package SWP301.Furniture_Moving_Project.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "authentication")
public class AuthCredential {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_id")
    private Integer authId;

    @Column(name = "user_id", nullable = false, unique = true)
    private Integer userId;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "mfa_enabled")
    private Boolean mfaEnabled;

    @Column(name = "mfa_method")
    private String mfaMethod;

    @Column(name = "failed_attempts")
    private Integer failedAttempts;

    // Trong schema là account_locked_until
    @Column(name = "account_locked_until")
    private LocalDateTime accountLockedUntil;

    public Integer getAuthId() { return authId; }
    public void setAuthId(Integer authId) { this.authId = authId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Boolean getMfaEnabled() { return mfaEnabled; }
    public void setMfaEnabled(Boolean mfaEnabled) { this.mfaEnabled = mfaEnabled; }
    public String getMfaMethod() { return mfaMethod; }
    public void setMfaMethod(String mfaMethod) { this.mfaMethod = mfaMethod; }
    public Integer getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(Integer failedAttempts) { this.failedAttempts = failedAttempts; }
    public LocalDateTime getAccountLockedUntil() { return accountLockedUntil; }
    public void setAccountLockedUntil(LocalDateTime accountLockedUntil) { this.accountLockedUntil = accountLockedUntil; }
}
