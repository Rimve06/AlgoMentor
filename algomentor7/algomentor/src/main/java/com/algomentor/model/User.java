package com.algomentor.model;

import java.time.LocalDateTime;

/** Maps to the `users` table. Passwords are stored as salted SHA-256 hashes. */
public class User {
    private int id;
    private String username;
    private String email;
    private String passwordHash;
    private String salt;
    private boolean emailVerified;
    private LocalDateTime createdAt;

    public User() {}

    public User(int id, String username, String email, String passwordHash, String salt,
                boolean emailVerified, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.emailVerified = emailVerified;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }
    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
