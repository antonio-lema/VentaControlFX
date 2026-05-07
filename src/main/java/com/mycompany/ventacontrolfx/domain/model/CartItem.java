package com.mycompany.ventacontrolfx.domain.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class CartItem {
    private Product product;
    private IntegerProperty quantity;
    private final DoubleProperty unitPrice = new SimpleDoubleProperty(0.0);
    private final DoubleProperty discountAmount = new SimpleDoubleProperty(0.0);
    private final DoubleProperty manualDiscountAmount = new SimpleDoubleProperty(0.0);
    private final javafx.beans.property.StringProperty observations = new javafx.beans.property.SimpleStringProperty(
            "");

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
     * Al llamar a este m\u00e9todo, la UI se refresca autom\u00e1ticamente.
     */
    public void updateUnitPrice(double newPrice) {
        // Forzamos una notificaci\u00f3n aunque el valor sea el mismo
        // (evita el bug de JavaFX donde set() no dispara si el valor no cambia)
        double prev = this.unitPrice.get();
        if (prev == newPrice) {
            // Truco: cambiar a NaN y luego al valor para forzar la notificaci\u00f3n
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

    public double getDiscountAmount() {
        return discountAmount.get();
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount.set(discountAmount);
    }

    public DoubleProperty discountAmountProperty() {
        return discountAmount;
    }

    public double getManualDiscountAmount() {
        return manualDiscountAmount.get();
    }

    public void setManualDiscountAmount(double amount) {
        this.manualDiscountAmount.set(amount);
    }

    public DoubleProperty manualDiscountAmountProperty() {
        return manualDiscountAmount;
    }

    public String getObservations() {
        return observations.get();
    }

    public void setObservations(String observations) {
        this.observations.set(observations);
    }

    public javafx.beans.property.StringProperty observationsProperty() {
        return observations;
    }

    public double getTotal() {
        // Total = (Precio * Cantidad) - Descuento Automático - Descuento Manual
        double rawTotal = (unitPrice.get() * quantity.get()) - discountAmount.get() - manualDiscountAmount.get();
        // Redondeo profesional a 2 decimales para evitar imprecisiones de coma flotante
        return Math.max(0, Math.round(rawTotal * 100.0) / 100.0);
    }
}

