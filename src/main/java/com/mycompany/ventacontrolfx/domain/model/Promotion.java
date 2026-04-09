package com.mycompany.ventacontrolfx.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad de dominio que representa una promoci\u00c3\u00b3n o descuento.
 */
public class Promotion {
    private Integer id;
    private String name;
    private String description;
    private PromotionType type;
    private double value;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean active;
    private PromotionScope scope;

    // IDs de productos o categor\u00c3\u00adas afectados seg\u00c3\u00ban el scope
    private List<Integer> affectedIds = new ArrayList<>();

    // L\u00c3\u00b3gica de Volumen (2x1, 3x2, etc.)
    private int buyQty;
    private int freeQty;

    public Promotion() {
    }

    public Promotion(Integer id, String name, PromotionType type, double value, PromotionScope scope) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.value = value;
        this.scope = scope;
        this.active = true;
    }

    // Getters y Setters
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PromotionType getType() {
        return type;
    }

    public void setType(PromotionType type) {
        this.type = type;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public PromotionScope getScope() {
        return scope;
    }

    public void setScope(PromotionScope scope) {
        this.scope = scope;
    }

    public List<Integer> getAffectedIds() {
        return affectedIds;
    }

    public void setAffectedIds(List<Integer> affectedIds) {
        this.affectedIds = affectedIds;
    }

    public int getBuyQty() {
        return buyQty;
    }

    public void setBuyQty(int buyQty) {
        this.buyQty = buyQty;
    }

    public int getFreeQty() {
        return freeQty;
    }

    public void setFreeQty(int freeQty) {
        this.freeQty = freeQty;
    }

    /**
     * Verifica si la promoci\u00c3\u00b3n es v\u00c3\u00a1lida en el momento actual.
     */
    public boolean isValidNow() {
        if (!active)
            return false;
        LocalDateTime now = LocalDateTime.now();
        if (startDate != null && now.isBefore(startDate))
            return false;
        if (endDate != null && now.isAfter(endDate))
            return false;
        return true;
    }
}
