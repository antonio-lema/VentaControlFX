package com.mycompany.ventacontrolfx.domain.model;

import java.time.LocalDateTime;

/**
 * Entidad que representa un valor monetario asignado a un producto
 * en un periodo de tiempo determinado.
 */
public class Price {
    private int id;
    private int productId;
    private int priceListId;
    private double value;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String reason;
    private Integer updateLogId;

    public Price() {
        this.startDate = LocalDateTime.now();
    }

    public Price(int productId, int priceListId, double value, String reason) {
        setProductId(productId);
        setPriceListId(priceListId);
        setValue(value);
        this.reason = reason;
        this.startDate = LocalDateTime.now();
    }

    /**
     * Regla de negocio: Verifica si el precio est\u00e1 vigente en una fecha dada.
     */
    public boolean isActiveAt(LocalDateTime dateTime) {
        if (dateTime == null)
            return false;
        return (dateTime.isEqual(startDate) || dateTime.isAfter(startDate)) &&
                (endDate == null || dateTime.isBefore(endDate));
    }

    /**
     * Regla de negocio: Verifica si el precio es el actualmente activo.
     */
    public boolean isCurrentlyActive() {
        return isActiveAt(LocalDateTime.now());
    }

    /**
     * Regla de negocio: Define el cierre de vigencia de este precio.
     */
    public void close(LocalDateTime terminationDate) {
        if (terminationDate != null && terminationDate.isBefore(startDate)) {
            throw new IllegalArgumentException("La fecha de fin no puede ser anterior a la de inicio");
        }
        this.endDate = terminationDate;
    }

    // --- Getters & Validating Setters ---

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
        if (productId <= 0)
            throw new IllegalArgumentException("ID de producto inv\u00e1lido");
        this.productId = productId;
    }

    public int getPriceListId() {
        return priceListId;
    }

    public void setPriceListId(int priceListId) {
        if (priceListId <= 0)
            throw new IllegalArgumentException("ID de lista de precios inv\u00e1lido");
        this.priceListId = priceListId;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        if (value < 0)
            throw new IllegalArgumentException("El precio no puede ser negativo");
        this.value = value;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getUpdateLogId() {
        return updateLogId;
    }

    public void setUpdateLogId(Integer updateLogId) {
        this.updateLogId = updateLogId;
    }
}
