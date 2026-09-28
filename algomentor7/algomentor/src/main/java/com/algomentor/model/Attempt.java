package com.algomentor.model;

import java.time.LocalDateTime;

/**
 * Maps to the `attempts` table. Each Attempt has a foreign key back to the
 * User that created it (users.id -> attempts.user_id), establishing a
 * one-to-many relationship: one user can have many recorded attempts.
 */
public class Attempt {
    private int id;
    private int userId;            // FK -> users.id
    private String algorithmName;
    private int inputSize;
    private long durationMillis;
    private String note;
    private LocalDateTime createdAt;

    public Attempt() {}

    public Attempt(int id, int userId, String algorithmName, int inputSize,
                   long durationMillis, String note, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.algorithmName = algorithmName;
        this.inputSize = inputSize;
        this.durationMillis = durationMillis;
        this.note = note;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getAlgorithmName() { return algorithmName; }
    public void setAlgorithmName(String algorithmName) { this.algorithmName = algorithmName; }
    public int getInputSize() { return inputSize; }
    public void setInputSize(int inputSize) { this.inputSize = inputSize; }
    public long getDurationMillis() { return durationMillis; }
    public void setDurationMillis(long durationMillis) { this.durationMillis = durationMillis; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}