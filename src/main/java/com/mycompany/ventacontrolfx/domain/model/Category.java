package com.mycompany.ventacontrolfx.domain.model;

public class Category {
    private int id;
    private String name;
    private boolean visible;
    private boolean favorite;
    private double defaultIva = 21.0;
    private Integer taxGroupId; // Tax Engine V2

    public Category() {
        this.visible = true;
        this.favorite = false;
    }

    public Category(int id, String name, boolean visible, boolean favorite, double defaultIva) {
        this.id = id;
        this.name = name;
        this.visible = visible;
        this.favorite = favorite;
        this.defaultIva = defaultIva;
    }

    public Category(int id, String name, boolean visible, boolean favorite) {
        this(id, name, visible, favorite, 21.0);
    }

    public Category(int id, String name, boolean visible) {
        this(id, name, visible, false);
    }

    public Category(int id, String name) {
        this(id, name, true, false);
    }

    public Category(String name, boolean visible, boolean favorite) {
        this.name = name;
        this.visible = visible;
        this.favorite = favorite;
    }

    public Category(String name) {
        this(name, true, false);
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

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public double getDefaultIva() {
        return defaultIva;
    }

    public void setDefaultIva(double defaultIva) {
        this.defaultIva = defaultIva;
    }

    public Integer getTaxGroupId() {
        return taxGroupId;
    }

    public void setTaxGroupId(Integer taxGroupId) {
        this.taxGroupId = taxGroupId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Category category = (Category) o;
        return id == category.id;
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
