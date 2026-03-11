package com.mycompany.ventacontrolfx.domain.model;

/**
 * Representa el resumen de un impuesto específico aplicado a una venta
 * completa.
 * Corresponde a la tabla `sale_tax_summary`.
 * 
 * Este modelo es esencial para cumplir con normativas fiscales que exigen
 * un desglose claro de las Bases Imponibles y Cuotas de IVA por cada tipo
 * impositivo.
 */
public class SaleTaxSummary {
    private int id;
    private int saleId;
    private int taxRateId;
    private String taxName;
    private double taxRate;
    private double taxBasis;
    private double taxAmount;

    public SaleTaxSummary() {
    }

    public SaleTaxSummary(int saleId, int taxRateId, String taxName, double taxRate, double taxBasis,
            double taxAmount) {
        this.saleId = saleId;
        this.taxRateId = taxRateId;
        this.taxName = taxName;
        this.taxRate = taxRate;
        this.taxBasis = taxBasis;
        this.taxAmount = taxAmount;
    }

    // Getters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSaleId() {
        return saleId;
    }

    public void setSaleId(int saleId) {
        this.saleId = saleId;
    }

    public int getTaxRateId() {
        return taxRateId;
    }

    public void setTaxRateId(int taxRateId) {
        this.taxRateId = taxRateId;
    }

    public String getTaxName() {
        return taxName;
    }

    public void setTaxName(String taxName) {
        this.taxName = taxName;
    }

    public double getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(double taxRate) {
        this.taxRate = taxRate;
    }

    public double getTaxBasis() {
        return taxBasis;
    }

    public void setTaxBasis(double taxBasis) {
        this.taxBasis = taxBasis;
    }

    public double getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(double taxAmount) {
        this.taxAmount = taxAmount;
    }
}
