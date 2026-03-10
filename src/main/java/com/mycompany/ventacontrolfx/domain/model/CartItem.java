package com.mycompany.ventacontrolfx.domain.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class CartItem {
    private Product product;
    private IntegerProperty quantity;
    private final DoubleProperty unitPrice = new SimpleDoubleProperty(0.0);

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = new SimpleIntegerProperty(quantity);
        this.unitPrice.set(product.getPrice());
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
        this.unitPrice.set(product.getPrice());
    }

    public int getQuantity() {
        return quantity.get();
    }

    public void setQuantity(int quantity) {
        this.quantity.set(quantity);
    }

    public IntegerProperty quantityProperty() {
        return quantity;
    }

    /**
     * Actualiza el precio unitario reactivamente.
     * Al llamar a este método, la UI se refresca automáticamente.
     */
    public void updateUnitPrice(double newPrice) {
        this.product.setPrice(newPrice);
        // Forzamos una notificación aunque el valor sea el mismo
        // (evita el bug de JavaFX donde set() no dispara si el valor no cambia)
        double prev = this.unitPrice.get();
        if (prev == newPrice) {
            // Truco: cambiar a NaN y luego al valor para forzar la notificación
            this.unitPrice.set(Double.NaN);
        }
        this.unitPrice.set(newPrice);
    }

    public double getUnitPrice() {
        return unitPrice.get();
    }

    public DoubleProperty unitPriceProperty() {
        return unitPrice;
    }

    public double getTotal() {
        return unitPrice.get() * quantity.get();
    }
}
