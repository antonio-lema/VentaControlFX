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

    public Product() {
    }

    public Product(int id, int categoryId, String name, double price, boolean isFavorite) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.price = price;
        this.isFavorite = isFavorite;
    }

    public Product(int categoryId, String name, double price, boolean isFavorite) {
        this.categoryId = categoryId;
        this.name = name;
        this.price = price;
        this.isFavorite = isFavorite;
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

    public boolean isIsFavorite() {
        return isFavorite;
    }

    public void setIsFavorite(boolean isFavorite) {
        this.isFavorite = isFavorite;
    }

    @Override
    public String toString() {
        return name;
    }
}
