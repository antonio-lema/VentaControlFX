package com.mycompany.ventacontrolfx.domain.model;

public class HistoryStats {
    private int count;
    private double totalRevenue;
    private double totalCash;
    private double totalCard;

    public HistoryStats(int count, double totalRevenue, double totalCash, double totalCard) {
        this.count = count;
        this.totalRevenue = totalRevenue;
        this.totalCash = totalCash;
        this.totalCard = totalCard;
    }

    public int getCount() {
        return count;
    }

    public double getTotalRevenue() {
        return totalRevenue;
    }

    public double getTotalCash() {
        return totalCash;
    }

    public double getTotalCard() {
        return totalCard;
    }
}

