package com.mycompany;

import com.mycompany.ventacontrolfx.infrastructure.persistence.JdbcPromotionRepository;
import com.mycompany.ventacontrolfx.domain.model.Promotion;
import java.util.List;

public class DumpPromos {
    public static void main(String[] args) {
        try {
            JdbcPromotionRepository repo = new JdbcPromotionRepository();
            List<Promotion> list = repo.getActive();
            System.out.println("=== ACTIVE PROMOS ===");
            for (Promotion p : list) {
                System.out.println("ID: " + p.getId() + " | Name: " + p.getName() + " | Type: " + p.getType() + " | Buy: " + p.getBuyQty() + " | Free: " + p.getFreeQty() + " | Scope: " + p.getScope() + " | Affected: " + p.getAffectedIds());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
