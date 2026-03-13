package com.mycompany.ventacontrolfx.domain.model;

/**
 * Define el tipo de cálculo de la promoción.
 */
public enum PromotionType {
    PERCENTAGE("Porcentaje (%)"),
    FIXED_DISCOUNT("Descuento Fijo (€)"),
    VOLUME_DISCOUNT("Volumen (2x1, 3x2, etc.)");

    private final String displayName;

    PromotionType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
