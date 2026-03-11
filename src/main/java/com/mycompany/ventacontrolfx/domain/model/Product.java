/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.ventacontrolfx.domain.model;

/**
 *
 * @author PracticasSoftware1
 */
public class Product {
    private int id;
    private int categoryId;
    private String name;
    private double price;
    private boolean isFavorite;
    private String imagePath;
    private boolean visible;
    private String categoryName;
    private Double iva; // Legacy fallback
    private Double categoryIva; // Legacy fallback
    private Integer taxGroupId; // Tax Engine V2
    private double currentPrice; // Precio calculado por la vista según tarifa activa actual

    public Product() {
        this.visible = true;
    }

    // Modified constructor to include categoryName
    public Product(int id, int categoryId, String name, double price, boolean isFavorite, boolean visible,
            String imagePath, String categoryName, Double iva, Double categoryIva) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.price = price;
        this.isFavorite = isFavorite;
        this.visible = visible;
        this.imagePath = imagePath;
        this.categoryName = categoryName;
        this.iva = iva;
        this.categoryIva = categoryIva;
    }

    public Product(int id, int categoryId, String name, double price, boolean isFavorite, boolean visible,
            String imagePath, String categoryName) {
        this(id, categoryId, name, price, isFavorite, visible, imagePath, categoryName, null, null);
    }

    // Kept for backward compatibility
    public Product(int id, int categoryId, String name, double price, boolean isFavorite, boolean visible,
            String imagePath) {
        this(id, categoryId, name, price, isFavorite, visible, imagePath, null);
    }

    // Keep existing constructor for backward compatibility or refactor usage
    public Product(int id, int categoryId, String name, double price, boolean isFavorite) {
        this(id, categoryId, name, price, isFavorite, true, null);
    }

    public Product(int categoryId, String name, double price, boolean isFavorite, boolean visible, String imagePath) {
        this.categoryId = categoryId;
        this.name = name;
        this.price = price;
        this.isFavorite = isFavorite;
        this.visible = visible;
        this.imagePath = imagePath;
    }

    public Product(int categoryId, String name, double price, boolean isFavorite) {
        this(categoryId, name, price, isFavorite, true, null);
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Double getIva() {
        return iva;
    }

    public void setIva(Double iva) {
        this.iva = iva;
    }

    public Integer getTaxGroupId() {
        return taxGroupId;
    }

    public void setTaxGroupId(Integer taxGroupId) {
        this.taxGroupId = taxGroupId;
    }

    // Keep legacy support for templates/older code if needed
    public Double getTaxRate() {
        return iva;
    }

    public void setTaxRate(Double taxRate) {
        this.iva = taxRate;
    }

    public Double getCategoryIva() {
        return categoryIva;
    }

    public void setCategoryIva(Double categoryIva) {
        this.categoryIva = categoryIva;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    /**
     * Resolves the effective IVA for this product based on the priority:
     * 1. Product IVA (if set)
     * 2. Category IVA (if set)
     * 3. Global IVA (provided as fallback)
     */
    public double resolveEffectiveIva(double globalIva) {
        if (iva != null)
            return iva;
        if (categoryIva != null && categoryIva > 0)
            return categoryIva;
        return globalIva;
    }

    @Override
    public String toString() {
        return name;
    }
}
