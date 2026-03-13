package com.mycompany.ventacontrolfx.application.service;

import com.mycompany.ventacontrolfx.domain.model.CartItem;
import com.mycompany.ventacontrolfx.domain.model.Promotion;
import com.mycompany.ventacontrolfx.domain.model.PromotionScope;
import com.mycompany.ventacontrolfx.domain.model.PromotionType;
import com.mycompany.ventacontrolfx.domain.repository.IPromotionRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Motor avanzado de promociones que orquestra la aplicación de reglas
 * complejas.
 */
public class PromotionEngine {
    private final IPromotionRepository promotionRepository;

    public PromotionEngine(IPromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public PromotionResult process(List<CartItem> items) {
        PromotionResult result = new PromotionResult();
        try {
            List<Promotion> activePromos = promotionRepository.getActive();

            // 1. Aplicar promociones por PRODUCTO/CATEGORÍA (Nivel de ítem)
            for (CartItem item : items) {
                applyItemLevelPromos(item, activePromos, result);
            }

            // 2. Aplicar promociones de VOLUMEN (2x1, 3x2, etc.)
            applyVolumePromos(items, activePromos, result);

            // 3. Aplicar promociones GLOBALES
            applyGlobalPromos(items, activePromos, result);

        } catch (SQLException e) {
            System.err.println("[PromotionEngine] Error fetching promotions: " + e.getMessage());
        }
        return result;
    }

    private void applyItemLevelPromos(CartItem item, List<Promotion> promos, PromotionResult res) {
        int productId = item.getProduct().getId();
        int categoryId = item.getProduct().getCategoryId();

        for (Promotion p : promos) {
            if (p.getType() == PromotionType.VOLUME_DISCOUNT)
                continue; // Saltamos volumen aquí

            boolean appliesByProduct = p.getScope() == PromotionScope.PRODUCT && p.getAffectedIds().contains(productId);
            boolean appliesByCategory = p.getScope() == PromotionScope.CATEGORY
                    && p.getAffectedIds().contains(categoryId);

            if (appliesByProduct || appliesByCategory) {
                double discount = calculateDiscount(item.getUnitPrice(), p) * item.getQuantity();
                if (discount > 0) {
                    res.addDiscount(productId, discount, p.getName());
                }
            }
        }
    }

    private void applyVolumePromos(List<CartItem> items, List<Promotion> promos, PromotionResult res) {
        List<Promotion> volumePromos = promos.stream()
                .filter(p -> p.getType() == PromotionType.VOLUME_DISCOUNT)
                .collect(Collectors.toList());

        for (Promotion p : volumePromos) {
            // Filtrar items afectados por esta promo de volumen
            List<CartItem> affectedItems = items.stream()
                    .filter(it -> (p.getScope() == PromotionScope.PRODUCT
                            && p.getAffectedIds().contains(it.getProduct().getId()))
                            || (p.getScope() == PromotionScope.CATEGORY
                                    && p.getAffectedIds().contains(it.getProduct().getCategoryId())))
                    .collect(Collectors.toList());

            if (affectedItems.isEmpty())
                continue;

            // 1. Desglosar todas las unidades individuales para manejar precios distintos
            // (Mix and Match)
            List<UnitPriceRecord> allUnits = new java.util.ArrayList<>();
            for (CartItem it : affectedItems) {
                for (int i = 0; i < it.getQuantity(); i++) {
                    allUnits.add(new UnitPriceRecord(it.getProduct().getId(), it.getUnitPrice()));
                }
            }

            int totalQty = allUnits.size();
            int buyQty = p.getBuyQty();
            int freeQty = p.getFreeQty();

            if (buyQty > 0 && totalQty >= (buyQty + freeQty)) {
                // Cantidad de aplicaciones del pack (ej: en 3x2, pack de 3)
                int applications = totalQty / (buyQty + freeQty);
                int totalFreeUnits = applications * freeQty;

                // 2. Ordenar por precio ascendente (REGLA: el más barato es gratis)
                allUnits.sort(java.util.Comparator.comparingDouble(u -> u.price));

                // 3. Aplicar el descuento unitario a las N unidades más baratas
                for (int i = 0; i < totalFreeUnits; i++) {
                    UnitPriceRecord freeUnit = allUnits.get(i);
                    res.addDiscount(freeUnit.productId, freeUnit.price,
                            p.getName() + " (Unidad gratis)");
                }
            }
        }
    }

    /**
     * Helper para desglosar unidades en promociones de volumen.
     */
    private static record UnitPriceRecord(int productId, double price) {
    }

    private void applyGlobalPromos(List<CartItem> items, List<Promotion> promos, PromotionResult res) {
        double currentSubtotal = items.stream().mapToDouble(CartItem::getTotal).sum() - res.getTotalDiscount();

        for (Promotion p : promos) {
            if (p.getScope() == PromotionScope.GLOBAL) {
                double discount = calculateDiscount(currentSubtotal, p);
                if (discount > 0) {
                    res.addDiscount(-1, discount, p.getName());
                    currentSubtotal -= discount; // Aplicación cascada
                }
            }
        }
    }

    private double calculateDiscount(double basePrice, Promotion p) {
        if (p.getType() == PromotionType.PERCENTAGE) {
            return basePrice * (p.getValue() / 100.0);
        } else if (p.getType() == PromotionType.FIXED_DISCOUNT) {
            return p.getValue();
        }
        return 0;
    }
}
