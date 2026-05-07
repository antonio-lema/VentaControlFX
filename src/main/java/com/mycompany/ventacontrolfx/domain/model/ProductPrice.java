package com.mycompany.ventacontrolfx.domain.model;

import java.time.LocalDateTime;

public class ProductPrice {
    private int id;
    private int productId;
    private int priceListId;
    private double price;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private String reason;

    public ProductPrice() {
    }

    public ProductPrice(int id, int productId, int priceListId, double price, LocalDateTime validFrom,
            LocalDateTime validTo) {
        this.id = id;
        this.productId = productId;
        this.priceListId = priceListId;
        this.price = price;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getPriceListId() {
        return priceListId;
    }

    public void setPriceListId(int priceListId) {
        this.priceListId = priceListId;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDateTime validTo) {
        this.validTo = validTo;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

