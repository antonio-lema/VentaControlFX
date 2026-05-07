package com.mycompany.ventacontrolfx.domain.model;

import java.time.LocalDateTime;

public class ClientSaleSummary {
    private final int clientId;
    private final int totalOrders;
    private final double totalSpent;
    private final LocalDateTime lastPurchase;

    public ClientSaleSummary(int clientId, int totalOrders, double totalSpent, LocalDateTime lastPurchase) {
        this.clientId = clientId;
        this.totalOrders = totalOrders;
        this.totalSpent = totalSpent;
        this.lastPurchase = lastPurchase;
    }

    public int getClientId() {
        return clientId;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public double getTotalSpent() {
        return totalSpent;
    }

    public LocalDateTime getLastPurchase() {
        return lastPurchase;
    }
}

