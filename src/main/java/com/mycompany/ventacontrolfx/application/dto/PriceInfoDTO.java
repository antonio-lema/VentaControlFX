package com.mycompany.ventacontrolfx.application.dto;

import java.time.LocalDateTime;

/**
 * DTO para transportar informaci\u00f3n de precio a la capa de presentaci\u00f3n.
 * A\u00edsla a la UI de las reglas internas de la entidad Price.
 */
public class PriceInfoDTO {
    private final double value;
    private final String priceListName;
    private final LocalDateTime effectiveDate;
    private final String reason;
    private final boolean active;

    public PriceInfoDTO(double value, String priceListName, LocalDateTime effectiveDate, String reason,
            boolean active) {
        this.value = value;
        this.priceListName = priceListName;
        this.effectiveDate = effectiveDate;
        this.reason = reason;
        this.active = active;
    }

    public double getValue() {
        return value;
    }

    public String getPriceListName() {
        return priceListName;
    }

    public LocalDateTime getEffectiveDate() {
        return effectiveDate;
    }

    public String getReason() {
        return reason;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return String.format("%.2f \u20ac (%s)", value, priceListName);
    }
}

