package com.mycompany.ventacontrolfx.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad de dominio que representa una promoci\u00f3n o descuento.
 */
public class Promotion {
    private Integer id;
    private String name;
    private String description;
    private PromotionType type;
    private double value;
    private String code;
    private int maxUses;
    private int currentUses;
    private int usesPerCustomer = 1;
    private Integer customerId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean active;
    private PromotionScope scope;

    // IDs de productos o categor\u00edas afectados seg\u00fan el scope
    private List<Integer> affectedIds = new ArrayList<>();

    // Lógica de Volumen (2x1, 3x2, etc.)
    private int buyQty;
    private int freeQty;

    // Compra mínima para aplicar
    private double minOrderValue;

    public Promotion() {
    }

    public Promotion(Integer id, String name, String code, PromotionType type, double value, PromotionScope scope) {
        this.id = id;
        this.name = name;
        this.code = code;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getMaxUses() {
        return maxUses;
    }

    public void setMaxUses(int maxUses) {
        this.maxUses = maxUses;
    }

    public int getCurrentUses() {
        return currentUses;
    }

    public void setCurrentUses(int currentUses) {
        this.currentUses = currentUses;
    }

    public int getUsesPerCustomer() {
        return usesPerCustomer;
    }

    public void setUsesPerCustomer(int usesPerCustomer) {
        this.usesPerCustomer = usesPerCustomer;
    }

    public Integer getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Integer customerId) {
        this.customerId = customerId;
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

    public double getMinOrderValue() {
        return minOrderValue;
    }

    public void setMinOrderValue(double minOrderValue) {
        this.minOrderValue = minOrderValue;
    }

    /**
     * Verifica si la promoci\u00f3n es v\u00e1lida en el momento actual.
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

