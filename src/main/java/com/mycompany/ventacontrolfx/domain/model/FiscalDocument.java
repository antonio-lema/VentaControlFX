package com.mycompany.ventacontrolfx.domain.model;

import java.time.LocalDateTime;

/**
 * Documento fiscal inmutable que representa un Ticket o Factura emitida.
 * Una vez en estado EMITIDO, sus importes no pueden modificarse.
 * Clean Architecture — Capa de Dominio.
 */
public class FiscalDocument {

    public enum Type {
        TICKET, FACTURA
    }

    public enum Status {
        EMITIDO, ANULADO
    }

    private int saleId;
    private Type docType;
    private String docSeries; // "T", "F", "R"
    private int docNumber; // correlativo dentro de la serie
    private Status docStatus;

    // Snapshot del emisor en el momento de la emisión
    private String issuerName;
    private String issuerTaxId;
    private String issuerAddress;
    private String issuerPhone; // Snapshot del teléfono del emisor

    // Snapshot del receptor (solo en FACTURA)
    private String receiverName;
    private String receiverTaxId;
    private String receiverAddress;

    private LocalDateTime issuedAt;
    private double baseAmount; // importe sin IVA
    private double vatAmount; // total IVA
    private double totalAmount; // base + iva
    private String controlHash; // SHA-256 para integridad

    public FiscalDocument() {
    }

    // ── Getters & Setters ──────────────────────────────────────────────

    public int getSaleId() {
        return saleId;
    }

    public void setSaleId(int saleId) {
        this.saleId = saleId;
    }

    public Type getDocType() {
        return docType;
    }

    public void setDocType(Type docType) {
        this.docType = docType;
    }

    public String getDocSeries() {
        return docSeries;
    }

    public void setDocSeries(String docSeries) {
        this.docSeries = docSeries;
    }

    public int getDocNumber() {
        return docNumber;
    }

    public void setDocNumber(int docNumber) {
        this.docNumber = docNumber;
    }

    public Status getDocStatus() {
        return docStatus;
    }

    public void setDocStatus(Status docStatus) {
        this.docStatus = docStatus;
    }

    public String getIssuerName() {
        return issuerName;
    }

    public void setIssuerName(String issuerName) {
        this.issuerName = issuerName;
    }

    public String getIssuerTaxId() {
        return issuerTaxId;
    }

    public void setIssuerTaxId(String issuerTaxId) {
        this.issuerTaxId = issuerTaxId;
    }

    public String getIssuerAddress() {
        return issuerAddress;
    }

    public void setIssuerAddress(String issuerAddress) {
        this.issuerAddress = issuerAddress;
    }

    public String getIssuerPhone() {
        return issuerPhone;
    }

    public void setIssuerPhone(String issuerPhone) {
        this.issuerPhone = issuerPhone;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverTaxId() {
        return receiverTaxId;
    }

    public void setReceiverTaxId(String receiverTaxId) {
        this.receiverTaxId = receiverTaxId;
    }

    public String getReceiverAddress() {
        return receiverAddress;
    }

    public void setReceiverAddress(String receiverAddress) {
        this.receiverAddress = receiverAddress;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public double getBaseAmount() {
        return baseAmount;
    }

    public void setBaseAmount(double baseAmount) {
        this.baseAmount = baseAmount;
    }

    public double getVatAmount() {
        return vatAmount;
    }

    public void setVatAmount(double vatAmount) {
        this.vatAmount = vatAmount;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getControlHash() {
        return controlHash;
    }

    public void setControlHash(String controlHash) {
        this.controlHash = controlHash;
    }

    /** Genera la referencia legible del documento. Ej: "2026-T-00042" */
    public String getFullReference() {
        return String.format("%d-%s-%05d", issuedAt != null ? issuedAt.getYear() : 0, docSeries, docNumber);
    }

    /**
     * Invariante de dominio: un EMITIDO sólo puede pasar a ANULADO.
     */
    public boolean canTransitionTo(Status newStatus) {
        if (this.docStatus == Status.EMITIDO) {
            return newStatus == Status.ANULADO;
        }
        return false; // estados terminales no admiten transición
    }
}
