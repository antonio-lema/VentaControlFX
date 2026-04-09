package com.mycompany.ventacontrolfx.domain.model;

/**
 * Define el alcance de aplicaci\u00c3\u00b3n de la promoci\u00c3\u00b3n.
 */
public enum PromotionScope {
    GLOBAL("Toda la Venta"),
    CATEGORY("Por Categor\u00c3\u00ada"),
    PRODUCT("Producto Espec\u00c3\u00adfico");

    private final String displayName;

    PromotionScope(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
