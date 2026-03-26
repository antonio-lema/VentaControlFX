package com.mycompany.ventacontrolfx.domain.model;

import java.time.LocalDateTime;
import java.util.List;

public class Return {
    private int returnId;
    private int saleId;
    private int userId;
    private LocalDateTime returnDatetime;
    private double totalRefunded;
    private String reason;
    private String userName;
    private Integer closureId;
    private String paymentMethod;
    private double cashAmount;
    private double cardAmount;
    private List<ReturnDetail> details;

    // Fiscal Snapshots (Added for unique return invoice)
    private String docType; // RECTIFICATIVA
    private String docSeries; // "R"
    private Integer docNumber;
    private String docStatus;
    private String controlHash;
    private String customerNameSnapshot;
    private String issuerName;
    private String issuerTaxId;
    private String issuerAddress;

    public String getDocType() {
        return docType;
    }

    public void setDocType(String docType) {
        this.docType = docType;
    }

    public String getDocSeries() {
        return docSeries;
    }

    public void setDocSeries(String docSeries) {
        this.docSeries = docSeries;
    }

    public Integer getDocNumber() {
        return docNumber;
    }

    public void setDocNumber(Integer docNumber) {
        this.docNumber = docNumber;
    }

    public String getDocStatus() {
        return docStatus;
    }

    public void setDocStatus(String docStatus) {
        this.docStatus = docStatus;
    }

    public String getControlHash() {
        return controlHash;
    }

    public void setControlHash(String controlHash) {
        this.controlHash = controlHash;
    }

    public String getCustomerNameSnapshot() {
        return customerNameSnapshot;
    }

    public void setCustomerNameSnapshot(String customerNameSnapshot) {
        this.customerNameSnapshot = customerNameSnapshot;
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

    public String getFullReference() {
        if (docSeries == null || docNumber == null)
            return "DEV-" + returnId;
        return String.format("%d-%s-%05d", returnDatetime != null ? returnDatetime.getYear() : 0, docSeries, docNumber);
    }

    public Return() {
    }

    public Return(int saleId, int userId, double totalRefunded, String reason) {
        this.saleId = saleId;
        this.userId = userId;
        this.totalRefunded = totalRefunded;
        this.reason = reason;
        this.returnDatetime = LocalDateTime.now();
    }

    public int getReturnId() {
        return returnId;
    }

    public void setReturnId(int returnId) {
        this.returnId = returnId;
    }

    public int getSaleId() {
        return saleId;
    }

    public void setSaleId(int saleId) {
        this.saleId = saleId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public LocalDateTime getReturnDatetime() {
        return returnDatetime;
    }

    public void setReturnDatetime(LocalDateTime returnDatetime) {
        this.returnDatetime = returnDatetime;
    }

    public double getTotalRefunded() {
        return totalRefunded;
    }

    public void setTotalRefunded(double totalRefunded) {
        this.totalRefunded = totalRefunded;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<ReturnDetail> getDetails() {
        return details;
    }

    public void setDetails(List<ReturnDetail> details) {
        this.details = details;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Integer getClosureId() {
        return closureId;
    }

    public void setClosureId(Integer closureId) {
        this.closureId = closureId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public double getCashAmount() {
        return cashAmount;
    }

    public void setCashAmount(double cashAmount) {
        this.cashAmount = cashAmount;
    }

    public double getCardAmount() {
        return cardAmount;
    }

    public void setCardAmount(double cardAmount) {
        this.cardAmount = cardAmount;
    }

    public static class Builder {
        private final int saleId;
        private int userId;
        private LocalDateTime returnDatetime = LocalDateTime.now();
        private double totalRefunded;
        private String reason;
        private double cashAmount;
        private double cardAmount;
        private String docType;
        private String docSeries;
        private Integer docNumber;
        private String docStatus;
        private String customerNameSnapshot;
        private String issuerName;
        private String issuerTaxId;
        private String issuerAddress;

        public Builder(int saleId) {
            this.saleId = saleId;
        }

        public Builder userId(int userId) {
            this.userId = userId;
            return this;
        }

        public Builder returnDatetime(LocalDateTime dt) {
            this.returnDatetime = dt;
            return this;
        }

        public Builder totalRefunded(double amount) {
            this.totalRefunded = amount;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public Builder cashAmount(double amount) {
            this.cashAmount = amount;
            return this;
        }

        public Builder cardAmount(double amount) {
            this.cardAmount = amount;
            return this;
        }

        public Builder docType(String type) {
            this.docType = type;
            return this;
        }

        public Builder docSeries(String series) {
            this.docSeries = series;
            return this;
        }

        public Builder docNumber(Integer num) {
            this.docNumber = num;
            return this;
        }

        public Builder docStatus(String status) {
            this.docStatus = status;
            return this;
        }

        public Builder customerNameSnapshot(String name) {
            this.customerNameSnapshot = name;
            return this;
        }

        public Builder issuerName(String name) {
            this.issuerName = name;
            return this;
        }

        public Builder issuerTaxId(String taxId) {
            this.issuerTaxId = taxId;
            return this;
        }

        public Builder issuerAddress(String addr) {
            this.issuerAddress = addr;
            return this;
        }

        public Return build() {
            Return r = new Return();
            r.saleId = this.saleId;
            r.userId = this.userId;
            r.returnDatetime = this.returnDatetime;
            r.totalRefunded = this.totalRefunded;
            r.reason = this.reason;
            r.cashAmount = this.cashAmount;
            r.cardAmount = this.cardAmount;
            r.docType = this.docType;
            r.docSeries = this.docSeries;
            r.docNumber = this.docNumber;
            r.docStatus = this.docStatus;
            r.customerNameSnapshot = this.customerNameSnapshot;
            r.issuerName = this.issuerName;
            r.issuerTaxId = this.issuerTaxId;
            r.issuerAddress = this.issuerAddress;
            return r;
        }
    }
}
