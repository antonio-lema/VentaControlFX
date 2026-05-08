package com.mycompany.ventacontrolfx.domain.model;

/**
 * Define el alcance de aplicaci\u00f3n de la promoci\u00f3n.
 */
public enum PromotionScope {
    GLOBAL("promotion.scope.global"),
    CATEGORY("promotion.scope.category"),
    PRODUCT("promotion.scope.product");

    private final String key;

    PromotionScope(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}

