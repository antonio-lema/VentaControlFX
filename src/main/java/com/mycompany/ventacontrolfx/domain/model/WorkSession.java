package com.mycompany.ventacontrolfx.domain.model;

import java.time.LocalDateTime;

public class WorkSession {
    public enum SessionType {
        SHIFT, BREAK
    }

    public enum SessionStatus {
        ACTIVE, COMPLETED
    }

    private Integer sessionId;
    private Integer userId;
    private SessionType type;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private SessionStatus status;
    private String notes;

    public WorkSession() {
    }

    public WorkSession(Integer userId, SessionType type) {
        this.userId = userId;
        this.type = type;
        this.startTime = LocalDateTime.now();
        this.status = SessionStatus.ACTIVE;
    }

    // Getters and Setters
    public Integer getSessionId() {
        return sessionId;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public SessionType getType() {
        return type;
    }

    public void setType(SessionType type) {
        this.type = type;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
