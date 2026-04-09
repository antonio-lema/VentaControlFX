package com.mycompany.ventacontrolfx.domain.model;

/**
 * Define el tipo de cÃ¡lculo de la promociÃ³n.
 */
public enum PromotionType {
    PERCENTAGE("Porcentaje (%)"),
    FIXED_DISCOUNT("Descuento Fijo (â‚¬)"),
    VOLUME_DISCOUNT("Volumen (2x1, 3x2, etc.)");

    private final String displayName;

    PromotionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
