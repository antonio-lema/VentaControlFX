package com.mycompany.ventacontrolfx.domain.model;

/**
 * Define el alcance de aplicaci\u00f3n de la promoci\u00f3n.
 */
public enum PromotionScope {
    GLOBAL("Toda la Venta"),
    CATEGORY("Por Categor\u00eda"),
    PRODUCT("Producto Espec\u00edfico");

    private final String displayName;

    PromotionScope(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

