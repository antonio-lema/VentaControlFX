package com.mycompany.ventacontrolfx.domain.model;

/**
 * Define el alcance de aplicaciÃ³n de la promociÃ³n.
 */
public enum PromotionScope {
    GLOBAL("Toda la Venta"),
    CATEGORY("Por CategorÃ­a"),
    PRODUCT("Producto EspecÃ­fico");

    private final String displayName;

    PromotionScope(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
