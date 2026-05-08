package com.mycompany.ventacontrolfx.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Resultado de la ejecuci\u00f3n del motor de promociones.
 */
public class PromotionResult {
    private double totalDiscount = 0.0;
    private final Map<Integer, Double> itemDiscounts = new HashMap<>(); // productId -> discountAmount
    private final List<String> appliedPromos = new ArrayList<>();
    private final Map<Integer, String> itemPromoNames = new HashMap<>(); // productId -> promoName
    private final List<String> appliedPromoCodes = new ArrayList<>(); // Track actual codes for usage counting

    public void addDiscount(int productId, double amount, String promoName) {
        double currentAmount = this.itemDiscounts.getOrDefault(productId, 0.0);

        // REGLA DE NEGOCIO: El mayor descuento GANA (No es acumulable entre promos del mismo tipo)
        if (amount > currentAmount) {
            // Ajustar el total: quitar el viejo, poner el nuevo
            this.totalDiscount = (this.totalDiscount - currentAmount) + amount;
            this.itemDiscounts.put(productId, amount);
            this.itemPromoNames.put(productId, promoName);

            if (promoName != null && !this.appliedPromos.contains(promoName)) {
                this.appliedPromos.add(promoName);
            }
        }
    }

    /**
     * Suma un descuento forzosamente (para acumulaci\u00f3n de cupones).
     */
    public void forceAddDiscount(int productId, double amount, String promoName) {
        double previous = this.itemDiscounts.getOrDefault(productId, 0.0);
        this.itemDiscounts.put(productId, amount);
        this.totalDiscount = (this.totalDiscount - previous) + amount;
        if (promoName != null && !this.appliedPromos.contains(promoName)) {
            this.appliedPromos.add(promoName);
        }
    }

    public void addDiscountWithCode(int productId, double amount, String promoName, String actualCode) {
        addDiscount(productId, amount, promoName);
        if (actualCode != null && !this.appliedPromoCodes.contains(actualCode)) {
            this.appliedPromoCodes.add(actualCode);
        }
    }

    public List<String> getAppliedPromoCodes() {
        return appliedPromoCodes;
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

