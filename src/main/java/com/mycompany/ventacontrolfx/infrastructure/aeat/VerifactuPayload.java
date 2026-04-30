package com.mycompany.ventacontrolfx.infrastructure.aeat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Data Transfer Object que representa de forma neutral un registro fiscal a ser
 * tramitado a la AEAT. Esto desacopla el modelo estricto de BBDD
 * (Sales/Returns)
 * respecto al generador de la envolvente XML.
 */
public class VerifactuPayload {

    private final String nifEmisor;
    private final String razonSocialEmisor;

    private final String tipoFactura; // F1, F2, R1, R2, R3, R4, R5
    private final String numSerieFactura;
    private final String fechaExpedicion; // DD-MM-YYYY
    private final double importeTotal;

    private final String hashControl;
    private final String prevHash;
    private final String prevNumSerie;
    private final String prevFechaExpedicion;

    private final boolean isAnulacion;
    private final boolean isCorrection;

    private final String genTimestamp; // ISO with offset
    private final String idRegistro;

    private final double totalNet;
    private final double totalTax;

    private String customerName;
    private String customerNif;
    
    // Desglose de IVA por tasa (Rate -> [Base, Cuota])
    private java.util.Map<Double, Double[]> vatBreakdown = new java.util.HashMap<>();

    // Campos específicos para Facturas Rectificativas
    private String originalNumSerie;
    private String originalFechaExp;
    private double baseRectificada;
    private double cuotaRectificada;
    private String incidentReason;

    public VerifactuPayload(String nifEmisor, String razonSocialEmisor, String tipoFactura, String numSerieFactura,
            String fechaExpedicion, double importeTotal, double totalNet, double totalTax, 
            String hashControl, String prevHash,
            String prevNumSerie, String prevFechaExpedicion,
            boolean isAnulacion, boolean isCorrection, String idRegistro, String genTimestamp,
            String customerName, String customerNif) {
        this.nifEmisor = nifEmisor;
        this.razonSocialEmisor = razonSocialEmisor;
        this.tipoFactura = tipoFactura;
        this.numSerieFactura = numSerieFactura;
        this.fechaExpedicion = fechaExpedicion;
        this.importeTotal = importeTotal;
        this.totalNet = totalNet;
        this.totalTax = totalTax;
        this.hashControl = hashControl;
        this.prevHash = prevHash;
        this.prevNumSerie = prevNumSerie;
        this.prevFechaExpedicion = prevFechaExpedicion;
        this.isAnulacion = isAnulacion;
        this.isCorrection = isCorrection;
        this.idRegistro = idRegistro;
        this.genTimestamp = genTimestamp;
        this.customerName = customerName;
        this.customerNif = customerNif;
    }

    public String getIncidentReason() {
        return incidentReason;
    }

    public void setIncidentReason(String incidentReason) {
        this.incidentReason = incidentReason;
    }

    // Setters para rectificativas
    public void setRectificacion(String originalNumSerie, String originalFechaExp, double base, double cuota) {
        this.originalNumSerie = originalNumSerie;
        this.originalFechaExp = originalFechaExp;
        this.baseRectificada = base;
        this.cuotaRectificada = cuota;
    }

    public String getNifEmisor() {
        return nifEmisor;
    }

    public String getRazonSocialEmisor() {
        return razonSocialEmisor;
    }

    public String getTipoFactura() {
        return tipoFactura;
    }

    public String getNumSerieFactura() {
        return numSerieFactura;
    }

    public String getFechaExpedicion() {
        return fechaExpedicion;
    }

    public double getImporteTotal() {
        return importeTotal;
    }

    public String getHashControl() {
        return hashControl;
    }

    public String getPrevHash() {
        return prevHash;
    }

    public String getPrevNumSerie() {
        return prevNumSerie;
    }

    public String getPrevFechaExpedicion() {
        return prevFechaExpedicion;
    }

    public boolean isAnulacion() {
        return isAnulacion;
    }

    public String getIdRegistro() {
        return idRegistro;
    }

    public String getGenTimestamp() {
        return genTimestamp;
    }

    public String getOriginalNumSerie() {
        return originalNumSerie;
    }

    public String getOriginalFechaExp() {
        return originalFechaExp;
    }

    public double getBaseRectificada() {
        return baseRectificada;
    }

    public double getCuotaRectificada() {
        return cuotaRectificada;
    }

    public boolean isCorrection() {
        return isCorrection;
    }

    public boolean isRectificativa() {
        return tipoFactura != null && tipoFactura.startsWith("R");
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerNif() {
        return customerNif;
    }

    public double getTotalNet() {
        return totalNet;
    }

    public double getTotalTax() {
        return totalTax;
    }

    public java.util.Map<Double, Double[]> getVatBreakdown() {
        return vatBreakdown;
    }

    public void setVatBreakdown(java.util.Map<Double, Double[]> vatBreakdown) {
        this.vatBreakdown = vatBreakdown;
    }
}
