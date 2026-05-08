package com.mycompany.ventacontrolfx.domain.model;

/**
 * Define el tipo de c\u00e1lculo de la promoci\u00f3n.
 */
public enum PromotionType {
    PERCENTAGE("promotion.type.percentage"),
    FIXED_DISCOUNT("promotion.type.fixed_discount"),
    VOLUME_DISCOUNT("promotion.type.volume_discount");

    private final String key;

    PromotionType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}

