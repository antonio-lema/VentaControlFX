package com.mycompany.ventacontrolfx.domain.model;

import java.util.ArrayList;
import java.util.List;

public class TaxGroup {
    private int taxGroupId;
    private String name;
    private boolean isDefault;
    private List<TaxRate> taxRates = new ArrayList<>();

    public TaxGroup() {
    }

    public int getTaxGroupId() {
        return taxGroupId;
    }

    public Integer getId() {
        return getTaxGroupId();
    }

    public void setTaxGroupId(int taxGroupId) {
        this.taxGroupId = taxGroupId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public List<TaxRate> getTaxRates() {
        return taxRates;
    }

    public void setTaxRates(List<TaxRate> taxRates) {
        this.taxRates = taxRates;
    }

    public List<TaxRate> getRates() {
        return getTaxRates();
    }

    public void addTaxRate(TaxRate rate) {
        this.taxRates.add(rate);
    }
}
