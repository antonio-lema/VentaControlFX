package com.mycompany.ventacontrolfx.domain.model;

import java.time.LocalDateTime;

/**
 * Documento fiscal inmutable que representa un Ticket o Factura emitida.
 * Una vez en estado EMITIDO, sus importes no pueden modificarse.
 * Clean Architecture — Capa de Dominio.
 */
public class FiscalDocument {

    public enum Type {
        TICKET, FACTURA, RECTIFICATIVA
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

    private FiscalDocument() {
    }

    public static class Builder {
        private final FiscalDocument document = new FiscalDocument();

        public Builder saleId(int saleId) {
            document.saleId = saleId;
            return this;
        }

        public Builder type(Type type) {
            document.docType = type;
            return this;
        }

        public Builder series(String series) {
            document.docSeries = series;
            return this;
        }

        public Builder number(int number) {
            document.docNumber = number;
            return this;
        }

        public Builder status(Status status) {
            document.docStatus = status;
            return this;
        }

        public Builder issuer(String name, String taxId, String address, String phone) {
            document.issuerName = name;
            document.issuerTaxId = taxId;
            document.issuerAddress = address;
            document.issuerPhone = phone;
            return this;
        }

        public Builder receiver(String name, String taxId, String address) {
            document.receiverName = name;
            document.receiverTaxId = taxId;
            document.receiverAddress = address;
            return this;
        }

        public Builder issuedAt(LocalDateTime dateTime) {
            document.issuedAt = dateTime;
            return this;
        }

        public Builder amounts(double base, double vat, double total) {
            document.baseAmount = base;
            document.vatAmount = vat;
            document.totalAmount = total;
            return this;
        }

        public Builder controlHash(String hash) {
            document.controlHash = hash;
            return this;
        }

        public FiscalDocument build() {
            if (document.docType == null)
                throw new IllegalStateException("DocType must be set");
            if (document.docStatus == null)
                document.docStatus = Status.EMITIDO;
            if (document.issuedAt == null)
                document.issuedAt = LocalDateTime.now();
            return document;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    // --- Getters & Protective Setters ---

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
        checkImmutable();
        this.docType = docType;
    }

    public String getDocSeries() {
        return docSeries;
    }

    public void setDocSeries(String docSeries) {
        checkImmutable();
        this.docSeries = docSeries;
    }

    public int getDocNumber() {
        return docNumber;
    }

    public void setDocNumber(int docNumber) {
        checkImmutable();
        this.docNumber = docNumber;
    }

    public Status getDocStatus() {
        return docStatus;
    }

    public void setDocStatus(Status status) {
        if (!canTransitionTo(status)) {
            throw new IllegalStateException("Invalid status transition from " + this.docStatus + " to " + status);
        }
        this.docStatus = status;
    }

    public String getIssuerName() {
        return issuerName;
    }

    public void setIssuerName(String issuerName) {
        checkImmutable();
        this.issuerName = issuerName;
    }

    public String getIssuerTaxId() {
        return issuerTaxId;
    }

    public void setIssuerTaxId(String issuerTaxId) {
        checkImmutable();
        this.issuerTaxId = issuerTaxId;
    }

    public String getIssuerAddress() {
        return issuerAddress;
    }

    public void setIssuerAddress(String issuerAddress) {
        checkImmutable();
        this.issuerAddress = issuerAddress;
    }

    public String getIssuerPhone() {
        return issuerPhone;
    }

    public void setIssuerPhone(String issuerPhone) {
        checkImmutable();
        this.issuerPhone = issuerPhone;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        checkImmutable();
        this.receiverName = receiverName;
    }

    public String getReceiverTaxId() {
        return receiverTaxId;
    }

    public void setReceiverTaxId(String receiverTaxId) {
        checkImmutable();
        this.receiverTaxId = receiverTaxId;
    }

    public String getReceiverAddress() {
        return receiverAddress;
    }

    public void setReceiverAddress(String receiverAddress) {
        checkImmutable();
        this.receiverAddress = receiverAddress;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        checkImmutable();
        this.issuedAt = issuedAt;
    }

    public double getBaseAmount() {
        return baseAmount;
    }

    public void setBaseAmount(double baseAmount) {
        checkImmutable();
        this.baseAmount = baseAmount;
    }

    public double getVatAmount() {
        return vatAmount;
    }

    public void setVatAmount(double vatAmount) {
        checkImmutable();
        this.vatAmount = vatAmount;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        checkImmutable();
        this.totalAmount = totalAmount;
    }

    public String getControlHash() {
        return controlHash;
    }

    public void setControlHash(String controlHash) {
        checkImmutable();
        this.controlHash = controlHash;
    }

    private void checkImmutable() {
        if (this.docStatus == Status.EMITIDO) {
            throw new IllegalStateException("Cannot modify an EMITIDO fiscal document. Field is immutable.");
        }
    }

    public String getFullReference() {
        return String.format("%d-%s-%05d", issuedAt != null ? issuedAt.getYear() : 0, docSeries, docNumber);
    }

    public boolean canTransitionTo(Status newStatus) {
        if (this.docStatus == null)
            return true; // Initial creation
        if (this.docStatus == Status.EMITIDO) {
            return newStatus == Status.ANULADO;
        }
        return false;
    }
}
