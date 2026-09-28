package com.algomentor.model;

import java.time.LocalDateTime;

/** A not-yet-real account, waiting on email verification. See DatabaseManager for the full flow. */
public record PendingRegistration(String username, String email, String passwordHash,
                                   String salt, String code, LocalDateTime expiresAt) {
}
