package qlsv.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public abstract class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String username;
    private String passwordHash;
    private int failedAttempts;
    private LocalDateTime lockedUntil;
    private LocalDateTime lastLogin;
    private LocalDateTime previousLogin;
    private boolean mustChangePassword;

    protected User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public abstract String getRoleName();

    public abstract String getDisplayName();

    public abstract boolean isAdmin();

    public String getUsername() { return username; }

    public String getPasswordHash() { return passwordHash; }

    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public int getFailedAttempts() { return failedAttempts; }

    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public LocalDateTime getLockedUntil() { return lockedUntil; }

    public void setLockedUntil(LocalDateTime lockedUntil) { this.lockedUntil = lockedUntil; }

    public boolean isLocked(LocalDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public LocalDateTime getLastLogin() { return lastLogin; }

    public LocalDateTime getPreviousLogin() { return previousLogin; }

    public void recordLogin(LocalDateTime now) {
        this.previousLogin = this.lastLogin;
        this.lastLogin = now;
    }

    public boolean isMustChangePassword() { return mustChangePassword; }

    public void setMustChangePassword(boolean mustChangePassword) { this.mustChangePassword = mustChangePassword; }
}
