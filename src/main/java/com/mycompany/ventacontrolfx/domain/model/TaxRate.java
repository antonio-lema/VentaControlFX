package com.mycompany.ventacontrolfx.domain.model;

import java.time.LocalDateTime;

public class TaxRate {
    private int taxRateId;
    private String name;
    private double rate;
    private String country;
    private String region;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private boolean active;
    private TaxType taxType = TaxType.VAT;

    public enum TaxType {
        VAT, SURCHARGE, SPECIAL, OTHER
    }

    public TaxRate() {
    }

    public TaxType getTaxType() {
        return taxType;
    }

    public void setTaxType(TaxType taxType) {
        this.taxType = taxType;
    }

    public int getTaxRateId() {
        return taxRateId;
    }

    public void setTaxRateId(int taxRateId) {
        this.taxRateId = taxRateId;
    }

    public int getId() {
        return getTaxRateId();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDateTime validTo) {
        this.validTo = validTo;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}

