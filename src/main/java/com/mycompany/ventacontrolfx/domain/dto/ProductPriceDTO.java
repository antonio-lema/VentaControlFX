package com.mycompany.ventacontrolfx.domain.dto;

public class ProductPriceDTO {
    private final int productId;
    private final String productName;
    private final String productCategory;
    private final double taxRate;
    private double price;
    private double defaultPrice;

    public ProductPriceDTO(int productId, String productName, String productCategory,
            double price, double defaultPrice, double taxRate) {
        this.productId = productId;
        this.productName = productName;
        this.productCategory = productCategory;
        this.price = price;
        this.defaultPrice = defaultPrice;
        this.taxRate = taxRate;
    }

    public int getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getProductCategory() {
        return productCategory;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public double getDefaultPrice() {
        return defaultPrice;
    }

    public void setDefaultPrice(double defaultPrice) {
        this.defaultPrice = defaultPrice;
    }

    public double getTaxRate() {
        return taxRate;
    }

    public double getListPvp() {
        return Math.round(price * (1.0 + (taxRate / 100.0)) * 100.0) / 100.0;
    }

    public double getDefaultPvp() {
        return Math.round(defaultPrice * (1.0 + (taxRate / 100.0)) * 100.0) / 100.0;
    }

    public String getDiffPercentFormatted() {
        if (defaultPrice <= 0)
            return "";
        double diff = ((price - defaultPrice) / defaultPrice) * 100.0;
        if (Math.abs(diff) < 0.01)
            return "Igual =";
        if (diff > 0)
            return String.format("+%.1f%%", diff);
        return String.format("%.1f%%", diff);
    }
}
