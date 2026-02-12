/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.ventacontrolfx.model;

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

    public Product() {
        this.visible = true;
    }

    // Modified constructor to include categoryName
    public Product(int id, int categoryId, String name, double price, boolean isFavorite, boolean visible,
            String imagePath, String categoryName) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.price = price;
        this.isFavorite = isFavorite;
        this.visible = visible;
        this.imagePath = imagePath;
        this.categoryName = categoryName;
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

    @Override
    public String toString() {
        return name;
    }
}
