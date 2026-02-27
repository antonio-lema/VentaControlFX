package com.mycompany.ventacontrolfx.domain.model;

import java.time.LocalDateTime;
import java.util.List;

public class Return {
    private int returnId;
    private int saleId;
    private int userId;
    private LocalDateTime returnDatetime;
    private double totalRefunded;
    private String reason;
    private List<ReturnDetail> details;

    public Return() {
    }

    public Return(int saleId, int userId, double totalRefunded, String reason) {
        this.saleId = saleId;
        this.userId = userId;
        this.totalRefunded = totalRefunded;
        this.reason = reason;
        this.returnDatetime = LocalDateTime.now();
    }

    public int getReturnId() {
        return returnId;
    }

    public void setReturnId(int returnId) {
        this.returnId = returnId;
    }

    public int getSaleId() {
        return saleId;
    }

    public void setSaleId(int saleId) {
        this.saleId = saleId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public LocalDateTime getReturnDatetime() {
        return returnDatetime;
    }

    public void setReturnDatetime(LocalDateTime returnDatetime) {
        this.returnDatetime = returnDatetime;
    }

    public double getTotalRefunded() {
        return totalRefunded;
    }

    public void setTotalRefunded(double totalRefunded) {
        this.totalRefunded = totalRefunded;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<ReturnDetail> getDetails() {
        return details;
    }

    public void setDetails(List<ReturnDetail> details) {
        this.details = details;
    }
}
