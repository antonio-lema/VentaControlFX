package com.mycompany.ventacontrolfx;

import com.mycompany.ventacontrolfx.domain.model.Promotion;
import com.mycompany.ventacontrolfx.infrastructure.persistence.JdbcPromotionRepository;
import java.util.List;

public class DiagnosticPromotion {
    public static void main(String[] args) {
        try {
            JdbcPromotionRepository repo = new JdbcPromotionRepository();
            List<Promotion> activePromos = repo.getActive();

            System.out.println("=== DIAGN\u00d3STICO DE PROMOCIONES ACTIVAS ===");
            if (activePromos.isEmpty()) {
                System.out.println("No hay promociones activas en la base de datos.");
            } else {
                for (Promotion p : activePromos) {
                    System.out.println("ID: " + p.getId());
                    System.out.println("Nombre: " + p.getName());
                    System.out.println("Tipo: " + p.getType());
                    System.out.println("Activa: " + p.isActive());
                    System.out.println("Scope: " + p.getScope());
                    System.out.println("Buy Qty: " + p.getBuyQty());
                    System.out.println("Free Qty: " + p.getFreeQty());
                    System.out.println("IDs Afectados: " + p.getAffectedIds());
                    System.out.println("-----------------------------------");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
