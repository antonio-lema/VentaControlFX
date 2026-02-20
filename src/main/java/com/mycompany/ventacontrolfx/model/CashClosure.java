package com.mycompany.ventacontrolfx.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CashClosure {
    private int closureId;
    private LocalDate closureDate;
    private int userId;
    private double totalCash;
    private double totalCard;
    private double totalAll;
    private LocalDateTime createdAt;

    public CashClosure() {
    }

    public CashClosure(int closureId, LocalDate closureDate, int userId, double totalCash, double totalCard,
            double totalAll, LocalDateTime createdAt) {
        this.closureId = closureId;
        this.closureDate = closureDate;
        this.userId = userId;
        this.totalCash = totalCash;
        this.totalCard = totalCard;
        this.totalAll = totalAll;
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
}
