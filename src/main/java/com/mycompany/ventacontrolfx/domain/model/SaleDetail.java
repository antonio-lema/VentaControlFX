package com.mycompany.ventacontrolfx.domain.model;

public class SaleDetail {
    private int detailId;
    private int saleId;
    private int productId;
    private int quantity;
    private double unitPrice;
    private double lineTotal;
    private double ivaRate;
    private double ivaAmount;

    // Auxiliary field for UI display
    private String productName;
    private int returnedQuantity;

    public SaleDetail() {
    }

    public int getDetailId() {
        return detailId;
    }

    public void setDetailId(int detailId) {
        this.detailId = detailId;
    }

    public int getSaleId() {
        return saleId;
    }

    public void setSaleId(int saleId) {
        this.saleId = saleId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public double getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(double lineTotal) {
        this.lineTotal = lineTotal;
    }

    public double getIvaRate() {
        return ivaRate;
    }

    public void setIvaRate(double ivaRate) {
        this.ivaRate = ivaRate;
    }

    public double getIvaAmount() {
        return ivaAmount;
    }

    public void setIvaAmount(double ivaAmount) {
        this.ivaAmount = ivaAmount;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public int getReturnedQuantity() {
        return returnedQuantity;
    }

    public void setReturnedQuantity(int returnedQuantity) {
        this.returnedQuantity = returnedQuantity;
    }
}
