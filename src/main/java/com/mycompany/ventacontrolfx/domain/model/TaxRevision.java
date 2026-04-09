package com.mycompany.ventacontrolfx.domain.model;

import java.time.LocalDateTime;

/**
 * Entidad de dominio: Representa una vigencia de tasa de IVA.
 *
 * Cada vez que cambia el IVA de un producto, categorÃ­a o de forma global,
 * se crea un nuevo TaxRevision con start_date y se cierra el anterior con
 * end_date. Esto garantiza trazabilidad total sin modificar datos histÃ³ricos.
 *
 * JerarquÃ­a de aplicaciÃ³n (de mayor a menor prioridad):
 * PRODUCT > CATEGORY > GLOBAL
 */
public class TaxRevision {

    public enum Scope {
        /** Aplica a un producto especÃ­fico. */
        PRODUCT,
        /** Aplica a todos los productos de una categorÃ­a. */
        CATEGORY,
        /** Aplica a todos los productos sin IVA propio o de categorÃ­a. */
        GLOBAL
    }

    private int id;
    private Integer productId; // null si scope es CATEGORY o GLOBAL
    private Integer categoryId; // null si scope es PRODUCT o GLOBAL
    private Scope scope;
    private double rate; // Porcentaje: 21.0, 10.0, 4.0
    private String label; // "IVA General", "IVA Reducido", "IVA Superreducido"
    private LocalDateTime startDate;
    private LocalDateTime endDate; // null = vigente actualmente
    private String reason;

    public TaxRevision() {
        this.startDate = LocalDateTime.now();
    }

    public TaxRevision(Integer productId, Integer categoryId, Scope scope,
            double rate, String label, LocalDateTime startDate, String reason) {
        this.productId = productId;
        this.categoryId = categoryId;
        this.scope = scope;
        this.rate = rate;
        this.label = label;
        this.startDate = (startDate != null) ? startDate : LocalDateTime.now();
        this.reason = reason;
        // Validaciones de dominio al crear
        validateRate(rate);
        validateScopeConsistency();
    }

    // --- Reglas de negocio ---

    /**
     * Regla: Â¿EstÃ¡ esta tasa vigente en la fecha indicada?
     */
    public boolean isActiveAt(LocalDateTime dateTime) {
        if (dateTime == null)
            return false;
        return !dateTime.isBefore(startDate)
                && (endDate == null || dateTime.isBefore(endDate));
    }

    /**
     * Regla: Â¿Es esta la tasa actualmente vigente?
     */
    public boolean isCurrentlyActive() {
        return isActiveAt(LocalDateTime.now());
    }

    /**
     * Regla: Cierra la vigencia de esta tasa.
     * No se puede cerrar con una fecha anterior a la de inicio.
     */
    public void close(LocalDateTime at) {
        if (at == null)
            throw new IllegalArgumentException("La fecha de cierre no puede ser null.");
        if (at.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "La fecha de cierre no puede ser anterior a la de inicio del IVA.");
        }
        this.endDate = at;
    }

    // --- Validaciones privadas ---

    private void validateRate(double rate) {
        if (rate < 0) {
            throw new IllegalArgumentException("La tasa de IVA no puede ser negativa. Valor: " + rate);
        }
        if (rate > 100) {
            throw new IllegalArgumentException("La tasa de IVA no puede superar el 100%. Valor: " + rate);
        }
    }

    private void validateScopeConsistency() {
        if (scope == Scope.PRODUCT && productId == null) {
            throw new IllegalArgumentException("Un TaxRevision de tipo PRODUCT requiere un productId.");
        }
        if (scope == Scope.CATEGORY && categoryId == null) {
            throw new IllegalArgumentException("Un TaxRevision de tipo CATEGORY requiere un categoryId.");
        }
    }

    // --- Getters & Setters ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        validateRate(rate);
        this.rate = rate;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
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

    @Override
    public String toString() {
        return String.format("TaxRevision{scope=%s, rate=%.2f%%, label='%s', active=%b}",
                scope, rate, label, isCurrentlyActive());
    }
}
