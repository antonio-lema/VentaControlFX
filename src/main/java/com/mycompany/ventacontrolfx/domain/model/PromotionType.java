package com.mycompany.ventacontrolfx.domain.model;

/**
 * Define el tipo de c\u00c3\u00a1lculo de la promoci\u00c3\u00b3n.
 */
public enum PromotionType {
    PERCENTAGE("Porcentaje (%)"),
    FIXED_DISCOUNT("Descuento Fijo (\u20AC)"),
    VOLUME_DISCOUNT("Volumen (2x1, 3x2, etc.)");

    private final String displayName;

    PromotionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
