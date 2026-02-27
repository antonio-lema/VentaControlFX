package com.mycompany.ventacontrolfx.domain.model;

public class ProductSummary {
    private String name;
    private int quantity;
    private double totalAmount;

    public ProductSummary(String name, int quantity, double totalAmount) {
        this.name = name;
        this.quantity = quantity;
        this.totalAmount = totalAmount;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }
}
