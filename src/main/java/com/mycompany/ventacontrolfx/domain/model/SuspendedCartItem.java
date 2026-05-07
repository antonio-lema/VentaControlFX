package com.mycompany.ventacontrolfx.domain.model;

public class SuspendedCartItem {
    private int id;
    private int cartId;
    private Product product;
    private int quantity;
    private double priceAtSuspension;

    public SuspendedCartItem() {
    }

    public SuspendedCartItem(Product product, int quantity, double priceAtSuspension) {
        this.product = product;
        this.quantity = quantity;
        this.priceAtSuspension = priceAtSuspension;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCartId() {
        return cartId;
    }

    public void setCartId(int cartId) {
        this.cartId = cartId;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public double getPriceAtSuspension() {
        return priceAtSuspension;
    }

    public void setPriceAtSuspension(double priceAtSuspension) {
        this.priceAtSuspension = priceAtSuspension;
    }
}

