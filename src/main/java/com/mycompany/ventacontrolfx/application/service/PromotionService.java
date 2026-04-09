package com.mycompany.ventacontrolfx.application.service;

import com.mycompany.ventacontrolfx.domain.model.Promotion;
import com.mycompany.ventacontrolfx.domain.model.PromotionScope;
import com.mycompany.ventacontrolfx.domain.model.PromotionType;
import com.mycompany.ventacontrolfx.domain.repository.IPromotionRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio encargado de la lÃ³gica de aplicaciÃ³n de promociones.
 */
public class PromotionService {
    private final IPromotionRepository promotionRepository;

    public PromotionService(IPromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    /**
     * Aplica promociones activas a un precio base.
     */
    public double applyPromotions(int productId, int categoryId, double basePrice) throws SQLException {
        List<Promotion> activePromotions = promotionRepository.getActive();
        double discountedPrice = basePrice;

        // 1. Promociones por PRODUCTO especÃ­fico
        List<Promotion> productPromos = activePromotions.stream()
                .filter(p -> p.getScope() == PromotionScope.PRODUCT
                        && p.getAffectedIds().contains(productId))
                .collect(Collectors.toList());

        for (Promotion p : productPromos) {
            discountedPrice = calculateDiscount(discountedPrice, p);
        }

        // 2. Promociones por CATEGORÃA
        List<Promotion> categoryPromos = activePromotions.stream()
                .filter(p -> p.getScope() == PromotionScope.CATEGORY
                        && p.getAffectedIds().contains(categoryId))
                .collect(Collectors.toList());

        for (Promotion p : categoryPromos) {
            discountedPrice = calculateDiscount(discountedPrice, p);
        }

        return discountedPrice;
    }

    /**
     * Aplica promociones de Ã¡mbito GLOBAL a los totales finales si cumple
     * condiciones.
     * (Simplificado: aplica a la suma de la base)
     */
    public double applyGlobalPromotions(double totalBase) throws SQLException {
        List<Promotion> globalPromos = promotionRepository.getActive().stream()
                .filter(p -> p.getScope() == PromotionScope.GLOBAL)
                .collect(Collectors.toList());

        double discountedBase = totalBase;
        for (Promotion p : globalPromos) {
            discountedBase = calculateDiscount(discountedBase, p);
        }
        return discountedBase;
    }

    private double calculateDiscount(double originalPrice, Promotion p) {
        if (p.getType() == PromotionType.PERCENTAGE) {
            return originalPrice * (1 - (p.getValue() / 100.0));
        } else if (p.getType() == PromotionType.FIXED_DISCOUNT) {
            return Math.max(0, originalPrice - p.getValue());
        }
        return originalPrice;
    }
}
