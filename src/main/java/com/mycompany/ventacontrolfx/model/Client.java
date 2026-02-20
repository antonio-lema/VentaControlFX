package com.mycompany.ventacontrolfx.model;

import javafx.beans.property.*;

public class Client {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty taxId = new SimpleStringProperty(); // NIF/CIF
    private final StringProperty address = new SimpleStringProperty();
    private final StringProperty postalCode = new SimpleStringProperty();
    private final StringProperty city = new SimpleStringProperty();
    private final StringProperty province = new SimpleStringProperty();
    private final StringProperty email = new SimpleStringProperty();
    private final StringProperty phone = new SimpleStringProperty();
    private final BooleanProperty isCompany = new SimpleBooleanProperty(false);
    private final StringProperty country = new SimpleStringProperty("España");

    public Client() {
    }

    public Client(int id, String name, boolean isCompany, String taxId, String address, String postalCode, String city,
            String province,
            String country, String email, String phone) {
        setId(id);
        setName(name);
        setIsCompany(isCompany);
        setTaxId(taxId);
        setAddress(address);
        setPostalCode(postalCode);
        setCity(city);
        setProvince(province);
        setCountry(country);
        setEmail(email);
        setPhone(phone);
    }

    // ID
    public int getId() {
        return id.get();
    }

    public void setId(int value) {
        id.set(value);
    }

    public IntegerProperty idProperty() {
        return id;
    }

    // Name
    public String getName() {
        return name.get();
    }

    public void setName(String value) {
        name.set(value);
    }

    public StringProperty nameProperty() {
        return name;
    }

    // TaxID
    public String getTaxId() {
        return taxId.get();
    }

    public void setTaxId(String value) {
        taxId.set(value);
    }

    public StringProperty taxIdProperty() {
        return taxId;
    }

    // Address
    public String getAddress() {
        return address.get();
    }

    public void setAddress(String value) {
        address.set(value);
    }

    public StringProperty addressProperty() {
        return address;
    }

    // Postal Code
    public String getPostalCode() {
        return postalCode.get();
    }

    public void setPostalCode(String value) {
        postalCode.set(value);
    }

    public StringProperty postalCodeProperty() {
        return postalCode;
    }

    // City
    public String getCity() {
        return city.get();
    }

    public void setCity(String value) {
        city.set(value);
    }

    public StringProperty cityProperty() {
        return city;
    }

    // Province
    public String getProvince() {
        return province.get();
    }

    public void setProvince(String value) {
        province.set(value);
    }

    public StringProperty provinceProperty() {
        return province;
    }

    // Email
    public String getEmail() {
        return email.get();
    }

    public void setEmail(String value) {
        email.set(value);
    }

    public StringProperty emailProperty() {
        return email;
    }

    // Phone
    public String getPhone() {
        return phone.get();
    }

    public void setPhone(String value) {
        phone.set(value);
    }

    public StringProperty phoneProperty() {
        return phone;
    }

    // IsCompany
    public boolean isIsCompany() {
        return isCompany.get();
    }

    public void setIsCompany(boolean value) {
        isCompany.set(value);
    }

    public BooleanProperty isCompanyProperty() {
        return isCompany;
    }

    // Country
    public String getCountry() {
        return country.get();
    }

    public void setCountry(String value) {
        country.set(value);
    }

    public StringProperty countryProperty() {
        return country;
    }
}
