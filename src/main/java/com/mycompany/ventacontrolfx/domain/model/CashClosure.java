package com.mycompany.ventacontrolfx.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CashClosure {
    private int closureId;
    private LocalDate closureDate;
    private int userId;
    private double totalCash;
    private double totalCard;
    private double totalAll;
    private double actualCash; // NEW: Real cash counted
    private double difference; // NEW: Diff between theoretical and actual
    private String notes; // NEW: Comments about the difference
    private LocalDateTime createdAt;
    private String username; // Added for display in UI

    public CashClosure() {
    }

    public CashClosure(int closureId, LocalDate closureDate, int userId, double totalCash, double totalCard,
            double totalAll, double actualCash, double difference, LocalDateTime createdAt) {
        this.closureId = closureId;
        this.closureDate = closureDate;
        this.userId = userId;
        this.totalCash = totalCash;
        this.totalCard = totalCard;
        this.totalAll = totalAll;
        this.actualCash = actualCash;
        this.difference = difference;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getClosureId() {
        return closureId;
    }

    public void setClosureId(int closureId) {
        this.closureId = closureId;
    }

    public LocalDate getClosureDate() {
        return closureDate;
    }

    public void setClosureDate(LocalDate closureDate) {
        this.closureDate = closureDate;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public double getTotalCash() {
        return totalCash;
    }

    public void setTotalCash(double totalCash) {
        this.totalCash = totalCash;
    }

    public double getTotalCard() {
        return totalCard;
    }

    public void setTotalCard(double totalCard) {
        this.totalCard = totalCard;
    }

    public double getTotalAll() {
        return totalAll;
    }

    public void setTotalAll(double totalAll) {
        this.totalAll = totalAll;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public double getActualCash() {
        return actualCash;
    }

    public void setActualCash(double actualCash) {
        this.actualCash = actualCash;
    }

    public double getDifference() {
        return difference;
    }

    public void setDifference(double difference) {
        this.difference = difference;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
