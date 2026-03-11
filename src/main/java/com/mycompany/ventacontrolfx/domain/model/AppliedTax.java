package com.mycompany.ventacontrolfx.domain.model;

/**
 * Representa un impuesto calculado sobre una línea de venta o un total.
 * Contiene el detalle del impuesto aplicado (nombre, porcentaje) y el importe
 * económico.
 */
public class AppliedTax {
    private final int taxRateId;
    private final String taxName;
    private final double taxRate;
    private final double taxAmount;

    public AppliedTax(int taxRateId, String taxName, double taxRate, double taxAmount) {
        this.taxRateId = taxRateId;
        this.taxName = taxName;
        this.taxRate = taxRate;
        this.taxAmount = taxAmount;
    }

    public int getTaxRateId() {
        return taxRateId;
    }

    public String getTaxName() {
        return taxName;
    }

    public double getTaxRate() {
        return taxRate;
    }

    public double getTaxAmount() {
        return taxAmount;
    }

    @Override
    public String toString() {
        return String.format("%s (%.2f%%): %.2f", taxName, taxRate, taxAmount);
    }
}
