package com.mycompany.ventacontrolfx.domain.model;

/**
 * Define el alcance de aplicación de la promoción.
 */
public enum PromotionScope {
    GLOBAL("Toda la Venta"),
    CATEGORY("Por Categoría"),
    PRODUCT("Producto Específico");

    private final String displayName;

    PromotionScope(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
