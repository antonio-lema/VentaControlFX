package com.mycompany.ventacontrolfx;

import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DeepSyncCheck {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            System.out.println("=== ANALISIS DE CATEGORIA 'Tecnologia' (ID 5) ===");
            try (ResultSet rs = stmt.executeQuery("SELECT category_id, name, tax_group_id, default_iva FROM categories WHERE category_id = 5")) {
                if (rs.next()) {
                    System.out.printf("Cat: %s | GroupID: %s | Default IVA: %.2f\n",
                        rs.getString("name"), rs.getString("tax_group_id"), rs.getDouble("default_iva"));
                }
            }
            
            System.out.println("\n=== PRODUCTOS DE ESTA CATEGORIA ===");
            try (ResultSet rs = stmt.executeQuery("SELECT product_id, name, tax_group_id, iva, tax_rate FROM products WHERE category_id = 5")) {
                while (rs.next()) {
                    System.out.printf("ID: %d | Name: %15s | GroupID: %s | IVA: %.2f | TaxRate: %.2f\n",
                        rs.getInt("product_id"), rs.getString("name"), rs.getString("tax_group_id"), 
                        rs.getDouble("iva"), rs.getDouble("tax_rate"));
                }
            }

            System.out.println("\n=== PRODUCTOS SIN GRUPO (Pero vinculados a Cat 5) ===");
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM products WHERE category_id = 5 AND tax_group_id IS NULL")) {
                if (rs.next()) {
                    System.out.println("Total productos sin tax_group_id en esta categoría: " + rs.getInt(1));
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
