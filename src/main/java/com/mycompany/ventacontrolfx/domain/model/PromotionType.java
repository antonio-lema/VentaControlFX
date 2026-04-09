package com.mycompany.ventacontrolfx.domain.model;

/**
 * Define el tipo de c\u00e1lculo de la promoci\u00f3n.
 */
public enum PromotionType {
    PERCENTAGE("Porcentaje (%)"),
    FIXED_DISCOUNT("Descuento Fijo (\u20ac)"),
    VOLUME_DISCOUNT("Volumen (2x1, 3x2, etc.)");

    private final String displayName;

    PromotionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
