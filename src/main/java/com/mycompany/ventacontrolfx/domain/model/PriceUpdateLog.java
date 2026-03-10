package com.mycompany.ventacontrolfx.domain.model;

import java.time.LocalDateTime;

/**
 * Registro histórico de una actualización masiva de precios.
 */
public class PriceUpdateLog {
    private int logId;
    private String updateType;   // "percentage", "fixed", "rounding"
    private String scope;        // "GLOBAL" o "CATEGORY"
    private Integer categoryId;
    private double value;
    private int productsUpdated;
    private String reason;
    private LocalDateTime appliedAt;

    // Helpers para display
    private String categoryName;

    public PriceUpdateLog() {
        this.appliedAt = LocalDateTime.now();
    }

    public int getLogId() { return logId; }
    public void setLogId(int logId) { this.logId = logId; }

    public String getUpdateType() { return updateType; }
    public void setUpdateType(String updateType) { this.updateType = updateType; }

    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public int getProductsUpdated() { return productsUpdated; }
    public void setProductsUpdated(int productsUpdated) { this.productsUpdated = productsUpdated; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
}
