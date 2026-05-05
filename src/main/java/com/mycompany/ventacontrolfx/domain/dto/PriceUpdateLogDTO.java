package com.mycompany.ventacontrolfx.domain.dto;

import java.time.LocalDateTime;

public class PriceUpdateLogDTO {
    private final int logId;
    private final String updateType;
    private final String scope;
    private final Double value;
    private final int productsUpdated;
    private final String reason;
    private final Integer priceListId;
    private final LocalDateTime appliedAt;

    public PriceUpdateLogDTO(int logId, String updateType, String scope, Double value, int productsUpdated,
            String reason, Integer priceListId, LocalDateTime appliedAt) {
        this.logId = logId;
        this.updateType = updateType;
        this.scope = scope;
        this.value = value;
        this.productsUpdated = productsUpdated;
        this.reason = reason;
        this.priceListId = priceListId;
        this.appliedAt = appliedAt;
    }

    public int getLogId() {
        return logId;
    }

    public String getUpdateType() {
        return updateType;
    }

    public String getScope() {
        return scope;
    }

    public Double getValue() {
        return value;
    }

    public int getProductsUpdated() {
        return productsUpdated;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }

    public Integer getPriceListId() {
        return priceListId;
    }
}
