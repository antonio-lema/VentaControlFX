package com.mycompany.ventacontrolfx.domain.model;

public class SaleDetail {
    private int detailId;
    private int saleId;
    private int productId;
    private int quantity;
    private double unitPrice;
    private double lineTotal;
    private double ivaRate; // Legacy
    private double ivaAmount; // Legacy

    // Tax Engine V2 (Snapshot Fiscal)
    private double netUnitPrice;
    private double taxBasis;
    private double taxAmount;
    private double grossTotal;
    private String appliedTaxGroup;

    // Product Snapshots (Inmutabilidad)
    private String skuSnapshot;
    private String categoryNameSnapshot;

    public String getSkuSnapshot() {
        return skuSnapshot;
    }

    public void setSkuSnapshot(String skuSnapshot) {
        this.skuSnapshot = skuSnapshot;
    }

    public String getCategoryNameSnapshot() {
        return categoryNameSnapshot;
    }

    public void setCategoryNameSnapshot(String categoryNameSnapshot) {
        this.categoryNameSnapshot = categoryNameSnapshot;
    }

    // Auxiliary field for UI display
    private String productName;
    private int returnedQuantity;
    private String observations;

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

    // Tax Engine V2 Getters and Setters
    public double getNetUnitPrice() {
        return netUnitPrice;
    }

    public void setNetUnitPrice(double netUnitPrice) {
        this.netUnitPrice = netUnitPrice;
    }

    public double getTaxBasis() {
        return taxBasis;
    }

    public void setTaxBasis(double taxBasis) {
        this.taxBasis = taxBasis;
    }

    public double getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(double taxAmount) {
        this.taxAmount = taxAmount;
    }

    public double getGrossTotal() {
        return grossTotal;
    }

    public void setGrossTotal(double grossTotal) {
        this.grossTotal = grossTotal;
    }

    public String getAppliedTaxGroup() {
        return appliedTaxGroup;
    }

    public void setAppliedTaxGroup(String appliedTaxGroup) {
        this.appliedTaxGroup = appliedTaxGroup;
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

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }
}
