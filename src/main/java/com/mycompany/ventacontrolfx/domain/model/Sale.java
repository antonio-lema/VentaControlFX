package com.mycompany.ventacontrolfx.domain.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Sale {
    private int saleId;
    private LocalDateTime saleDateTime;
    private int userId;
    private Integer clientId;
    private double total;
    private String paymentMethod;
    private double iva;
    private boolean isReturn;
    private double returnedAmount;
    private double cashAmount; // Split amount for Mixed payment
    private double cardAmount; // Split amount for Mixed payment
    private String promoCode;
    private String rewardPromoCode;
    private int totalItems; // Transient count for UI performance optimization

    // Campos Fiscales (Documento)
    private String docType;
    private String docSeries;
    private Integer docNumber;
    private String docStatus;
    private String controlHash;
    private String prevHash;
    private String signature;
    private String fiscalStatus;
    private String fiscalMsg;
    private String aeatSubmissionId;
    private String genTimestamp;
    private boolean isCorrection;
    private String correctionType;

    // Global Snapshots (Inmutabilidad)
    private String customerNameSnapshot;
    private String customerNifSnapshot;
    private double totalNet;
    private double totalTax;
    private double discountAmount;
    private String discountReason;
    private String observations; // General sale-level observation

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getDiscountReason() {
        return discountReason;
    }

    public void setDiscountReason(String discountReason) {
        this.discountReason = discountReason;
    }

    public String getCustomerNameSnapshot() {
        return customerNameSnapshot;
    }

    public void setCustomerNameSnapshot(String customerNameSnapshot) {
        this.customerNameSnapshot = customerNameSnapshot;
    }

    public String getCustomerNifSnapshot() {
        return customerNifSnapshot;
    }

    public void setCustomerNifSnapshot(String customerNifSnapshot) {
        this.customerNifSnapshot = customerNifSnapshot;
    }

    public double getTotalNet() {
        return totalNet;
    }

    public void setTotalNet(double totalNet) {
        this.totalNet = totalNet;
    }

    public double getTotalTax() {
        return totalTax;
    }

    public void setTotalTax(double totalTax) {
        this.totalTax = totalTax;
    }

    public double getReturnedAmount() {
        return returnedAmount;
    }

    public void setReturnedAmount(double returnedAmount) {
        this.returnedAmount = returnedAmount;
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

    private Integer closureId; // Connection to cash closure
    private String returnReason;
    private String userName; // New field
    private List<SaleDetail> details = new ArrayList<>();
    private List<SaleTaxSummary> taxSummaries = new ArrayList<>(); // Tax Engine V2

    public Sale() {
    }

    public Integer getClosureId() {
        return closureId;
    }

    public void setClosureId(Integer closureId) {
        this.closureId = closureId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getSaleId() {
        return saleId;
    }

    public void setSaleId(int saleId) {
        this.saleId = saleId;
    }

    public LocalDateTime getSaleDateTime() {
        return saleDateTime;
    }

    public void setSaleDateTime(LocalDateTime saleDateTime) {
        this.saleDateTime = saleDateTime;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Integer getClientId() {
        return clientId;
    }

    public void setClientId(Integer clientId) {
        this.clientId = clientId;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public double getIva() {
        return iva;
    }

    public void setIva(double iva) {
        this.iva = iva;
    }

    public boolean isReturn() {
        return isReturn;
    }

    public void setReturn(boolean isReturn) {
        this.isReturn = isReturn;
    }

    public String getReturnReason() {
        return returnReason;
    }

    public void setReturnReason(String returnReason) {
        this.returnReason = returnReason;
    }

    public List<SaleDetail> getDetails() {
        return details;
    }

    public void setDetails(List<SaleDetail> details) {
        this.details = details;
    }

    public List<SaleTaxSummary> getTaxSummaries() {
        return taxSummaries;
    }

    public void setTaxSummaries(List<SaleTaxSummary> taxSummaries) {
        this.taxSummaries = taxSummaries;
    }

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

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public String getPrevHash() {
        return prevHash;
    }

    public void setPrevHash(String prevHash) {
        this.prevHash = prevHash;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getFiscalStatus() {
        return fiscalStatus;
    }

    public void setFiscalStatus(String fiscalStatus) {
        this.fiscalStatus = fiscalStatus;
    }

    public String getFiscalMsg() {
        return fiscalMsg;
    }

    public void setFiscalMsg(String fiscalMsg) {
        this.fiscalMsg = fiscalMsg;
    }

    public String getAeatSubmissionId() {
        return aeatSubmissionId;
    }

    public void setAeatSubmissionId(String aeatSubmissionId) {
        this.aeatSubmissionId = aeatSubmissionId;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }

    public String getRewardPromoCode() {
        return rewardPromoCode;
    }

    public void setRewardPromoCode(String rewardPromoCode) {
        this.rewardPromoCode = rewardPromoCode;
    }

    public int getTotalItems() {
        return totalItems;
    }

    public void setTotalItems(int totalItems) {
        this.totalItems = totalItems;
    }

    public String getGenTimestamp() {
        return genTimestamp;
    }

    public void setGenTimestamp(String genTimestamp) {
        this.genTimestamp = genTimestamp;
    }

    public boolean isCorrection() {
        return isCorrection;
    }

    public void setCorrection(boolean isCorrection) {
        this.isCorrection = isCorrection;
    }

    public String getCorrectionType() {
        return correctionType;
    }

    public void setCorrectionType(String correctionType) {
        this.correctionType = correctionType;
    }
}
