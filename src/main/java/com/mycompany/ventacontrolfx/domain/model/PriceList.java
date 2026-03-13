package com.mycompany.ventacontrolfx.domain.model;

public class PriceList {
    private int id;
    private String name;
    private String description;
    private boolean isDefault;
    private boolean isActive;
    private int priority;

    public PriceList() {
        this.isActive = true;
    }

    public PriceList(int id, String name, boolean isDefault) {
        this(id, name, "", isDefault, true, 0);
    }

    public PriceList(int id, String name, String description, boolean isDefault, boolean isActive, int priority) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.isDefault = isDefault;
        this.isActive = isActive;
        this.priority = priority;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        PriceList priceList = (PriceList) o;
        return id == priceList.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return name;
    }
}
