package com.mycompany.ventacontrolfx.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Resultado de la ejecuciÃ³n del motor de promociones.
 */
public class PromotionResult {
    private double totalDiscount = 0.0;
    private final Map<Integer, Double> itemDiscounts = new HashMap<>(); // productId -> discountAmount
    private final List<String> appliedPromos = new ArrayList<>();
    private final Map<Integer, String> itemPromoNames = new HashMap<>(); // productId -> promoName

    public void addDiscount(int productId, double amount, String promoName) {
        this.totalDiscount += amount;
        this.itemDiscounts.put(productId, this.itemDiscounts.getOrDefault(productId, 0.0) + amount);
        this.itemPromoNames.put(productId, promoName);
        if (promoName != null && !this.appliedPromos.contains(promoName)) {
            this.appliedPromos.add(promoName);
        }
    }

    public double getTotalDiscount() {
        return totalDiscount;
    }

    public Map<Integer, Double> getItemDiscounts() {
        return itemDiscounts;
    }

    public Map<Integer, String> getItemPromoNames() {
        return itemPromoNames;
    }

    public List<String> getAppliedPromos() {
        return appliedPromos;
    }
}
