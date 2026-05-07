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
    private double difference; // Diff between theoretical and actual
    private String notes; // Comments about the difference

    // NEW AUDIT FIELDS
    private LocalDateTime openingTime;
    private double initialFund;
    private double cashIn;
    private double cashOut;
    private double expectedCash;
    private String status;
    private Integer reviewedBy;
    private LocalDateTime reviewedAt;

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

    public LocalDateTime getOpeningTime() {
        return openingTime;
    }

    public void setOpeningTime(LocalDateTime openingTime) {
        this.openingTime = openingTime;
    }

    public double getInitialFund() {
        return initialFund;
    }

    public void setInitialFund(double initialFund) {
        this.initialFund = initialFund;
    }

    public double getCashIn() {
        return cashIn;
    }

    public void setCashIn(double cashIn) {
        this.cashIn = cashIn;
    }

    public double getCashOut() {
        return cashOut;
    }

    public void setCashOut(double cashOut) {
        this.cashOut = cashOut;
    }

    public double getExpectedCash() {
        return expectedCash;
    }

    public void setExpectedCash(double expectedCash) {
        this.expectedCash = expectedCash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Integer reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
}

