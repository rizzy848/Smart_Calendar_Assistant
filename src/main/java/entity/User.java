package entity;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Represents a user of the calendar system.
 * Each user has their own Google Calendar authentication.
 */
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    private String userId;              // Unique identifier
    private String username;            // Display name
    private String email;               // Google account email
    private String calendarId;          // Google Calendar ID (usually "primary")
    private LocalDateTime lastLogin;    // Track last access
    private boolean isAuthenticated;    // OAuth status
    private String tokensDirectory;     // Where user's tokens are stored

    // ===== CONSTRUCTORS =====

    public User() {
        this.calendarId = "primary";
        this.isAuthenticated = false;
    }

    public User(String userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.calendarId = "primary";
        this.tokensDirectory = "tokens/" + userId;
        this.isAuthenticated = false;
    }

    // ===== BUSINESS LOGIC =====

    /**
     * Mark user as logged in
     */
    public void login() {
        this.isAuthenticated = true;
        this.lastLogin = LocalDateTime.now();
    }

    /**
     * Mark user as logged out
     */
    public void logout() {
        this.isAuthenticated = false;
    }

    /**
     * Check if user needs to re-authenticate
     * @return true if last login was more than 30 days ago
     */
    public boolean needsReauthentication() {
        if (lastLogin == null) return true;
        return lastLogin.plusDays(30).isBefore(LocalDateTime.now());
    }

    /**
     * Get display name for UI
     */
    public String getDisplayName() {
        return username != null ? username : email;
    }

    // ===== GETTERS AND SETTERS =====

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
        this.tokensDirectory = "tokens/" + userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCalendarId() {
        return calendarId;
    }

    public void setCalendarId(String calendarId) {
        this.calendarId = calendarId;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        isAuthenticated = authenticated;
    }

    public String getTokensDirectory() {
        return tokensDirectory;
    }

    public void setTokensDirectory(String tokensDirectory) {
        this.tokensDirectory = tokensDirectory;
    }

    // ===== OBJECT METHODS =====

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(userId, user.userId) &&
                Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, email);
    }

    @Override
    public String toString() {
        return String.format("User{id='%s', name='%s', email='%s', authenticated=%s}",
                userId, username, email, isAuthenticated);
    }
}