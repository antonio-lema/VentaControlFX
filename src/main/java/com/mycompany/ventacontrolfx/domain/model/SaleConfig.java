package com.mycompany.ventacontrolfx.domain.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelo que representa la configuraci\u00f3n general de venta del TPV.
 */
public class SaleConfig {

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac Empresa
    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac
    private String companyName = "";
    private String cif = "";
    private String address = "";
    private String phone = "";
    private String email = "";
    private String logoPath = "";
    private String appIconPath = "";
    private String appName = "GestionTPV";
    private String smtpHost = "";
    private String smtpPort = "";
    private String emailFrom = "";
    private String emailPassword = "";

    // --- Verifactu AEAT ---
    private String verifactuNif = "";
    private String verifactuCertName = "";
    private String verifactuCertPath = "";
    private String verifactuCertPass = "";
    private String verifactuUrl = "";

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac Fiscal
    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac
    private double taxRate = 21.0;
    private String taxType = "IVA General (21%)";
    private boolean pricesIncludeTax = false;
    private String currency = "EUR \u2014 Euro (\u20ac)";
    private String decimals = "2 decimales";
    private String roundingMethod = "LINE"; // Tax Engine V2: 'LINE' o 'GLOBAL'

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac Ticket
    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac
    private boolean showLogo = true;
    private boolean showAddress = true;
    private boolean showPhone = true;
    private boolean showCif = true;
    private String footerMessage = "\u00a1Gracias por su compra!";
    private String ticketCopies = "1 copia";
    private String ticketFormat = "80mm";
    private boolean autoPrint = false;

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac M\u00e9todos de pago
    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac
    private boolean cash = true;
    private boolean card = true;
    private boolean transfer = false;
    private boolean check = false;
    private boolean credit = false;
    private String rounding = "Sin redondeo";

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac Opciones generales
    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac
    private boolean allowDiscount = false;
    private boolean allowGlobalDiscount = false;
    private boolean requireClient = false;
    private boolean stockAlert = true;
    private boolean negativeStock = false;
    private boolean showBarcode = false;
    private boolean soundOnAdd = true;
    private boolean confirmDelete = true;
    private String terminalName = "Caja 01";

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac Horarios
    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac
    private List<BusinessDay> schedule = new ArrayList<>();
    private List<SpecialDay> specialDays = new ArrayList<>();
    private int scheduleGracePeriodMins = 10;

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac Getters & Setters
    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u2500

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String v) {
        this.companyName = v;
    }

    public String getCif() {
        return cif;
    }

    public void setCif(String v) {
        this.cif = v;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String v) {
        this.address = v;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String v) {
        this.phone = v;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String v) {
        this.email = v;
    }

    public String getLogoPath() {
        return logoPath;
    }

    public void setLogoPath(String logoPath) {
        this.logoPath = logoPath;
    }

    public String getAppIconPath() {
        return appIconPath;
    }

    public void setAppIconPath(String appIconPath) {
        this.appIconPath = appIconPath;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public String getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(String smtpPort) {
        this.smtpPort = smtpPort;
    }

    public String getEmailFrom() {
        return emailFrom;
    }

    public void setEmailFrom(String emailFrom) {
        this.emailFrom = emailFrom;
    }

    public String getEmailPassword() {
        return emailPassword;
    }

    public void setEmailPassword(String emailPassword) {
        this.emailPassword = emailPassword;
    }

    public String getVerifactuNif() { return verifactuNif; }
    public void setVerifactuNif(String v) { this.verifactuNif = v; }

    public String getVerifactuCertName() { return verifactuCertName; }
    public void setVerifactuCertName(String v) { this.verifactuCertName = v; }

    public String getVerifactuCertPath() { return verifactuCertPath; }
    public void setVerifactuCertPath(String v) { this.verifactuCertPath = v; }

    public String getVerifactuCertPass() { return verifactuCertPass; }
    public void setVerifactuCertPass(String v) { this.verifactuCertPass = v; }

    public String getVerifactuUrl() { return verifactuUrl; }
    public void setVerifactuUrl(String v) { this.verifactuUrl = v; }

    public double getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(double v) {
        this.taxRate = v;
    }

    public String getTaxType() {
        return taxType;
    }

    public void setTaxType(String v) {
        this.taxType = v;
    }

    public boolean isPricesIncludeTax() {
        return pricesIncludeTax;
    }

    public void setPricesIncludeTax(boolean v) {
        this.pricesIncludeTax = v;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String v) {
        this.currency = v;
    }

    public String getDecimals() {
        return decimals;
    }

    public void setDecimals(String v) {
        this.decimals = v;
    }

    public String getRoundingMethod() {
        return roundingMethod;
    }

    public void setRoundingMethod(String roundingMethod) {
        this.roundingMethod = roundingMethod;
    }

    public boolean isShowLogo() {
        return showLogo;
    }

    public void setShowLogo(boolean v) {
        this.showLogo = v;
    }

    public boolean isShowAddress() {
        return showAddress;
    }

    public void setShowAddress(boolean v) {
        this.showAddress = v;
    }

    public boolean isShowPhone() {
        return showPhone;
    }

    public void setShowPhone(boolean v) {
        this.showPhone = v;
    }

    public boolean isShowCif() {
        return showCif;
    }

    public void setShowCif(boolean v) {
        this.showCif = v;
    }

    public String getFooterMessage() {
        return footerMessage;
    }

    public void setFooterMessage(String v) {
        this.footerMessage = v;
    }

    public String getTicketCopies() {
        return ticketCopies;
    }

    public void setTicketCopies(String v) {
        this.ticketCopies = v;
    }

    public boolean isAutoPrint() {
        return autoPrint;
    }

    public void setAutoPrint(boolean v) {
        this.autoPrint = v;
    }

    public String getTicketFormat() {
        return ticketFormat;
    }

    public void setTicketFormat(String ticketFormat) {
        this.ticketFormat = ticketFormat;
    }

    public boolean isCash() {
        return cash;
    }

    public void setCash(boolean v) {
        this.cash = v;
    }

    public boolean isCard() {
        return card;
    }

    public void setCard(boolean v) {
        this.card = v;
    }

    public boolean isTransfer() {
        return transfer;
    }

    public void setTransfer(boolean v) {
        this.transfer = v;
    }

    public boolean isCheck() {
        return check;
    }

    public void setCheck(boolean v) {
        this.check = v;
    }

    public boolean isCredit() {
        return credit;
    }

    public void setCredit(boolean v) {
        this.credit = v;
    }

    public String getRounding() {
        return rounding;
    }

    public void setRounding(String v) {
        this.rounding = v;
    }

    public boolean isAllowDiscount() {
        return allowDiscount;
    }

    public void setAllowDiscount(boolean v) {
        this.allowDiscount = v;
    }

    public boolean isAllowGlobalDiscount() {
        return allowGlobalDiscount;
    }

    public void setAllowGlobalDiscount(boolean v) {
        this.allowGlobalDiscount = v;
    }

    public boolean isRequireClient() {
        return requireClient;
    }

    public void setRequireClient(boolean v) {
        this.requireClient = v;
    }

    public boolean isStockAlert() {
        return stockAlert;
    }

    public void setStockAlert(boolean v) {
        this.stockAlert = v;
    }

    public boolean isNegativeStock() {
        return negativeStock;
    }

    public void setNegativeStock(boolean v) {
        this.negativeStock = v;
    }

    public boolean isShowBarcode() {
        return showBarcode;
    }

    public void setShowBarcode(boolean v) {
        this.showBarcode = v;
    }

    public boolean isSoundOnAdd() {
        return soundOnAdd;
    }

    public void setSoundOnAdd(boolean v) {
        this.soundOnAdd = v;
    }

    public boolean isConfirmDelete() {
        return confirmDelete;
    }

    public void setConfirmDelete(boolean v) {
        this.confirmDelete = v;
    }

    public List<BusinessDay> getSchedule() {
        return schedule;
    }

    public void setSchedule(List<BusinessDay> schedule) {
        this.schedule = schedule;
    }

    public List<SpecialDay> getSpecialDays() {
        return specialDays;
    }

    public void setSpecialDays(List<SpecialDay> specialDays) {
        this.specialDays = specialDays;
    }

    public int getScheduleGracePeriodMins() {
        return scheduleGracePeriodMins;
    }

    public void setScheduleGracePeriodMins(int mins) {
        this.scheduleGracePeriodMins = mins;
    }

    public String getTerminalName() {
        return terminalName != null ? terminalName : "Caja 01";
    }

    public void setTerminalName(String terminalName) {
        this.terminalName = terminalName;
    }

    public static class SpecialDay {
        private java.time.LocalDate date;
        private List<BusinessDay.TimeRange> shifts = new ArrayList<>();
        private boolean closed;
        private String reason;

        public SpecialDay() {
        }

        public SpecialDay(java.time.LocalDate date, boolean closed, String reason) {
            this.date = date;
            this.closed = closed;
            this.reason = reason;
        }

        public java.time.LocalDate getDate() {
            return date;
        }

        public void setDate(java.time.LocalDate date) {
            this.date = date;
        }

        public List<BusinessDay.TimeRange> getShifts() {
            return shifts;
        }

        public void setShifts(List<BusinessDay.TimeRange> shifts) {
            this.shifts = shifts;
        }

        public boolean isClosed() {
            return closed;
        }

        public void setClosed(boolean closed) {
            this.closed = closed;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac Helpers
    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac

    /**
     * Devuelve el s\u00edmbolo de moneda (\u20ac, $, \u00a3) seg\u00fan la moneda
     * configurada.
     */
    public String getCurrencySymbol() {
        if (currency == null)
            return "\u20ac";
        if (currency.startsWith("USD"))
            return "$";
        if (currency.startsWith("GBP"))
            return "\u00a3";
        return "\u20ac"; // EUR por defecto
    }

    /** Devuelve el n\u00famero de decimales configurado (0, 1, 2 o 3). */
    public int getDecimalCount() {
        if (decimals == null)
            return 2;
        if (decimals.startsWith("0"))
            return 0;
        if (decimals.startsWith("1"))
            return 1;
        if (decimals.startsWith("3"))
            return 3;
        return 2; // por defecto
    }

    /**
     * Calcula el divisor para extraer la base imponible del total con IVA incluido.
     * Si pricesIncludeTax es true equivale a: base = total / (1 + taxRate/100)
     */
    public double getTaxDivisor() {
        return 1.0 + (taxRate / 100.0);
    }
}

