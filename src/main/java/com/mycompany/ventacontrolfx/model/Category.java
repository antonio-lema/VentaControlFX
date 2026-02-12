package com.mycompany.ventacontrolfx.model;

public class Category {
    private int id;
    private String name;
    private boolean visible;
    private boolean favorite;

    public Category() {
        this.visible = true;
        this.favorite = false;
    }

    public Category(int id, String name, boolean visible, boolean favorite) {
        this.id = id;
        this.name = name;
        this.visible = visible;
        this.favorite = favorite;
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
