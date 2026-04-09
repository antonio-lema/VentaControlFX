package com.mycompany.ventacontrolfx.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado completo del cÃ¡lculo fiscal de una lÃ­nea de venta o de todo el
 * carrito.
 */
public class TaxCalculationResult {
    private final double netTotal;
    private final double grossTotal;
    private final List<AppliedTax> appliedTaxes;

    public TaxCalculationResult(double netTotal, double grossTotal, List<AppliedTax> appliedTaxes) {
        this.netTotal = netTotal;
        this.grossTotal = grossTotal;
        this.appliedTaxes = new ArrayList<>(appliedTaxes);
    }

    public double getNetTotal() {
        return netTotal;
    }

    public double getGrossTotal() {
        return grossTotal;
    }

    public List<AppliedTax> getAppliedTaxes() {
        return appliedTaxes;
    }

    public double getTotalTaxAmount() {
        return appliedTaxes.stream().mapToDouble(AppliedTax::getTaxAmount).sum();
    }
}
