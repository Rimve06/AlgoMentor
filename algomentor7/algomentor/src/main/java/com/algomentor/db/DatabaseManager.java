package com.algomentor.db;

import com.algomentor.model.Attempt;
import com.algomentor.model.PendingRegistration;
import com.algomentor.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Owns the single SQLite connection and every SQL statement in the app.
 *
 * Schema:
 *   pending_registrations(id PK, username UNIQUE, email UNIQUE, password_hash,
 *       salt, code, expires_at) - a NOT-YET-REAL account. Nothing here ever
 *       blocks a real login or a real duplicate-username check; rows here
 *       are only promoted into `users` once the emailed code is verified.
 *   users(id PK, username UNIQUE, email UNIQUE, password_hash, salt, created_at)
 *       - only ever contains real, verified accounts.
 *   attempts(id PK, user_id FK -> users(id) ON DELETE CASCADE, ...)
 *
 * This fixes a real bug from the previous version: a user row used to be
 * created immediately at registration time (before the code was verified),
 * so an abandoned or mistyped verification permanently reserved that
 * username/email forever with no way to redo it. Now nothing is written to
 * `users` until the code actually checks out.
 */
public class DatabaseManager {
    private final Connection connection;

    public DatabaseManager(String dbFilePath) throws SQLException {
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
        }
        createSchema();
    }

    private void createSchema() throws SQLException {
        String pending = """
            CREATE TABLE IF NOT EXISTS pending_registrations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                email TEXT NOT NULL UNIQUE,
                password_hash TEXT NOT NULL,
                salt TEXT NOT NULL,
                code TEXT NOT NULL,
                expires_at TEXT NOT NULL
            )
        """;

        String users = """
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL UNIQUE,
                email TEXT UNIQUE,
                password_hash TEXT NOT NULL,
                salt TEXT NOT NULL,
                created_at TEXT NOT NULL
            )
        """;

        String attempts = """
            CREATE TABLE IF NOT EXISTS attempts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                algorithm_name TEXT NOT NULL,
                input_size INTEGER NOT NULL,
                duration_millis INTEGER NOT NULL,
                note TEXT,
                created_at TEXT NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
            )
        """;

        try (Statement st = connection.createStatement()) {
            st.execute(pending);
            st.execute(users);
            st.execute(attempts);
        }
    }

    // ---------------------------------------------------------------
    // PENDING REGISTRATIONS (not yet real accounts)
    // ---------------------------------------------------------------

    /** CREATE/REPLACE - always overwrites any previous pending row for this username/email with a fresh code. */
    public void savePendingRegistration(String username, String email, String passwordHash, String salt,
                                        String code, LocalDateTime expiresAt) throws SQLException {
        deletePendingRegistration(username, email);
        String sql = "INSERT INTO pending_registrations(username, email, password_hash, salt, code, expires_at) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.setString(4, salt);
            ps.setString(5, code);
            ps.setString(6, expiresAt.toString());
            ps.executeUpdate();
        }
    }

    public PendingRegistration findPendingByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM pending_registrations WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapPending(rs);
            }
        }
    }

    public void deletePendingRegistration(String username, String email) throws SQLException {
        String sql = "DELETE FROM pending_registrations WHERE username = ? OR email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.executeUpdate();
        }
    }

    /**
     * The actual moment an account becomes real: verifies the code against
     * the pending row and, only if it matches and hasn't expired, inserts
     * into `users` and deletes the pending row. Returns the new User, or
     * null if the code was wrong/expired/missing (nothing is created).
     */
    public User completeRegistration(String username, String submittedCode) throws SQLException {
        PendingRegistration pending = findPendingByUsername(username);
        if (pending == null) return null;
        boolean codeOk = pending.code().equals(submittedCode.trim());
        boolean notExpired = LocalDateTime.now().isBefore(pending.expiresAt());
        if (!codeOk || !notExpired) return null;

        User user = createUser(pending.username(), pending.email(), pending.passwordHash(), pending.salt());
        deletePendingRegistration(pending.username(), pending.email());
        return user;
    }

    private PendingRegistration mapPending(ResultSet rs) throws SQLException {
        return new PendingRegistration(
                rs.getString("username"), rs.getString("email"),
                rs.getString("password_hash"), rs.getString("salt"),
                rs.getString("code"), LocalDateTime.parse(rs.getString("expires_at"))
        );
    }

    // ---------------------------------------------------------------
    // USER CRUD (real, verified accounts only)
    // ---------------------------------------------------------------

    /** CREATE - only ever called from completeRegistration(), once a code has actually been verified. */
    public User createUser(String username, String email, String passwordHash, String salt) throws SQLException {
        String sql = "INSERT INTO users(username, email, password_hash, salt, created_at) VALUES (?, ?, ?, ?, ?)";
        String now = LocalDateTime.now().toString();
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setString(3, passwordHash);
            ps.setString(4, salt);
            ps.setString(5, now);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                int id = keys.getInt(1);
                return new User(id, username, email, passwordHash, salt, true, LocalDateTime.parse(now));
            }
        }
    }

    /** READ (by username, used at login and for duplicate checks) */
    public User findUserByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapUser(rs);
            }
        }
    }

    /** READ (by email, used for duplicate checks) */
    public User findUserByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return mapUser(rs);
            }
        }
    }

    /** UPDATE (e.g. changing password) */
    public void updateUserPassword(int userId, String newHash, String newSalt) throws SQLException {
        String sql = "UPDATE users SET password_hash = ?, salt = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setString(2, newSalt);
            ps.setInt(3, userId);
            ps.executeUpdate();
        }
    }

    /** DELETE (cascades to attempts via FK) */
    public void deleteUser(int userId) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"), rs.getString("username"), rs.getString("email"),
                rs.getString("password_hash"), rs.getString("salt"), true,
                LocalDateTime.parse(rs.getString("created_at"))
        );
    }

    // ---------------------------------------------------------------
    // ATTEMPT CRUD
    // ---------------------------------------------------------------

    public Attempt createAttempt(int userId, String algorithmName, int inputSize,
                                 long durationMillis, String note) throws SQLException {
        String sql = """
            INSERT INTO attempts(user_id, algorithm_name, input_size, duration_millis, note, created_at)
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        String now = LocalDateTime.now().toString();
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userId);
            ps.setString(2, algorithmName);
            ps.setInt(3, inputSize);
            ps.setLong(4, durationMillis);
            ps.setString(5, note);
            ps.setString(6, now);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                int id = keys.getInt(1);
                return new Attempt(id, userId, algorithmName, inputSize, durationMillis, note, LocalDateTime.parse(now));
            }
        }
    }

    public List<Attempt> getAttemptsForUser(int userId) throws SQLException {
        String sql = "SELECT * FROM attempts WHERE user_id = ? ORDER BY created_at DESC";
        List<Attempt> results = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapAttempt(rs));
            }
        }
        return results;
    }

    public void updateAttemptNote(int attemptId, String newNote) throws SQLException {
        String sql = "UPDATE attempts SET note = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newNote);
            ps.setInt(2, attemptId);
            ps.executeUpdate();
        }
    }

    public void deleteAttempt(int attemptId) throws SQLException {
        String sql = "DELETE FROM attempts WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, attemptId);
            ps.executeUpdate();
        }
    }

    private Attempt mapAttempt(ResultSet rs) throws SQLException {
        return new Attempt(
                rs.getInt("id"), rs.getInt("user_id"), rs.getString("algorithm_name"),
                rs.getInt("input_size"), rs.getLong("duration_millis"), rs.getString("note"),
                LocalDateTime.parse(rs.getString("created_at"))
        );
    }

    public void close() {
        try { connection.close(); } catch (SQLException ignored) {}
    }
}
