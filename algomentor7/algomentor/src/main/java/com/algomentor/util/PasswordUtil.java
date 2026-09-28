package com.algomentor.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/** Small helper for salted SHA-256 password hashing (no plaintext ever stored). */
public final class PasswordUtil {
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {}

    public static String generateSalt() {
        byte[] saltBytes = new byte[16];
        RANDOM.nextBytes(saltBytes);
        return Base64.getEncoder().encodeToString(saltBytes);
    }

    public static String hash(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(Base64.getDecoder().decode(salt));
            byte[] hashed = digest.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean verify(String password, String salt, String expectedHash) {
        return hash(password, salt).equals(expectedHash);
    }

    /** Generates a random 6-digit numeric code, e.g. "042951", for email verification. */
    public static String generateNumericCode() {
        int number = RANDOM.nextInt(1_000_000);
        return String.format("%06d", number);
    }

    /** Very small, deliberately permissive Gmail-address check (real verification happens by email). */
    public static boolean looksLikeGmailAddress(String email) {
        if (email == null) return false;
        String trimmed = email.trim().toLowerCase();
        return trimmed.matches("^[a-z0-9._%+-]+@gmail\\.com$");
    }
}
