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
 * Motor avanzado de promociones que orquestra la aplicaci\u00f3n de reglas
 * complejas.
 */
public class PromotionEngine {
    private final IPromotionRepository promotionRepository;

    public PromotionEngine(IPromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public PromotionResult process(List<CartItem> items) {
        return process(items, null, null);
    }

    public PromotionResult process(List<CartItem> items, String appliedCode,
            com.mycompany.ventacontrolfx.domain.model.Client client) {
        PromotionResult result = new PromotionResult();
        try {
            List<Promotion> activePromos = promotionRepository.getActive();

            // Filtrar promociones: Las autom\u00e1ticas (sin c\u00f3digo) se aplican
            // siempre.
            // Las con c\u00f3digo solo si coinciden.
            List<Promotion> promosToApply = activePromos.stream()
                    .filter(p -> {
                        // 1. Verificar c\u00f3digo
                        boolean codeMatches = p.getCode() == null || p.getCode().isEmpty()
                                || p.getCode().equalsIgnoreCase(appliedCode);
                        if (!codeMatches)
                            return false;

                        // 2. Verificar l\u00edmite de usos globales
                        if (p.getMaxUses() > 0 && p.getCurrentUses() >= p.getMaxUses())
                            return false;

                        // 3. Verificar vinculaci\u00f3n a cliente espec\u00edfico
                        if (p.getCustomerId() != null) {
                            if (client == null || !p.getCustomerId().equals(client.getId()))
                                return false;
                        }

                        // 4. Verificar compra mínima (Min Order Value)
                        if (p.getMinOrderValue() > 0) {
                            double currentCartTotal = items.stream()
                                    .mapToDouble(it -> it.getUnitPrice() * it.getQuantity()).sum();
                            if (currentCartTotal < (p.getMinOrderValue() - 0.01)) { // Margen para redondeo
                                return false;
                            }
                        }

                        return true;
                    })
                    .collect(Collectors.toList());

            // 1. Simular promociones por PRODUCTO/CATEGOR\u00cda
            PromotionResult itemResult = new PromotionResult();
            for (CartItem item : items) {
                applyItemLevelPromos(item, promosToApply, itemResult);
            }

            // 2. Simular promociones de VOLUMEN
            PromotionResult volumeResult = new PromotionResult();
            applyVolumePromos(items, promosToApply, volumeResult);

            // 3. Fusionar: El mayor descuento por producto GANA (No acumulable)
            java.util.Set<Integer> allProductIds = new java.util.HashSet<>();
            allProductIds.addAll(itemResult.getItemDiscounts().keySet());
            allProductIds.addAll(volumeResult.getItemDiscounts().keySet());

            for (int prodId : allProductIds) {
                double itemD = itemResult.getItemDiscounts().getOrDefault(prodId, 0.0);
                double volD = volumeResult.getItemDiscounts().getOrDefault(prodId, 0.0);

                if (itemD >= volD && itemD > 0) {
                    result.addDiscount(prodId, itemD, itemResult.getItemPromoNames().get(prodId));
                } else if (volD > itemD) {
                    result.addDiscount(prodId, volD, volumeResult.getItemPromoNames().get(prodId));
                }
            }

            // 4. Aplicar promociones GLOBALES
            applyGlobalPromos(items, promosToApply, result);

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
                continue; // Saltamos volumen aqu\u00ed

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
                    .sorted(java.util.Comparator.comparingDouble(CartItem::getUnitPrice))
                    .collect(Collectors.toList());

            if (affectedItems.isEmpty())
                continue;

            int totalQty = affectedItems.stream().mapToInt(CartItem::getQuantity).sum();
            int buyQty = p.getBuyQty();
            int freeQty = p.getFreeQty();
            int packSize = buyQty + freeQty;

            if (packSize > 0 && totalQty >= packSize) {
                int applications = totalQty / packSize;
                int remainingFreeUnits = applications * freeQty;

                // Aplicar descuento a las unidades más baratas primero (recorriendo la lista ya
                // ordenada)
                for (CartItem it : affectedItems) {
                    if (remainingFreeUnits <= 0)
                        break;

                    int take = Math.min(it.getQuantity(), remainingFreeUnits);
                    double lineDiscount = take * it.getUnitPrice();

                    // Nota: addDiscount ahora es no-acumulativo, pero aquí estamos en el nivel de
                    // volumen
                    // Queremos guardar el descuento total de la promo de volumen para este producto
                    res.addDiscount(it.getProduct().getId(), lineDiscount, p.getName() + " (Unidad gratis)");
                    remainingFreeUnits -= take;
                }
            }
        }
    }

    private void applyGlobalPromos(List<CartItem> items, List<Promotion> promos, PromotionResult res) {
        double currentSubtotal = items.stream().mapToDouble(CartItem::getTotal).sum() - res.getTotalDiscount();

        for (Promotion p : promos) {
            if (p.getScope() == PromotionScope.GLOBAL) {
                double discount = calculateDiscount(currentSubtotal, p);
                if (discount > 0) {
                    String displayName = p.getName();
                    if (p.getCode() != null && p.getCode().startsWith("GIFT-")) {
                        displayName = String.format("REGALO FIDELIDAD (-%.2f\u20ac)", p.getValue());
                    }
                    res.addDiscountWithCode(-1, discount, displayName, p.getCode());
                    currentSubtotal -= discount; // Aplicación cascada
                }
            }
        }
    }

    private double calculateDiscount(double basePrice, Promotion p) {
        double result = 0;
        if (p.getType() == PromotionType.PERCENTAGE) {
            result = basePrice * (p.getValue() / 100.0);
        } else if (p.getType() == PromotionType.FIXED_DISCOUNT) {
            result = p.getValue();
        }
        return Math.max(0, result); // Nunca retornar descuentos negativos
    }
}
