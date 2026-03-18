package com.mycompany.ventacontrolfx;

import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class StatsCheck {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            System.out.println("=== TODOS LAS CATEGORIAS ===");
            String sql = "SELECT c.category_id, c.name, (SELECT COUNT(*) FROM products p WHERE p.category_id = c.category_id) as prod_count, c.default_iva, c.tax_group_id FROM categories c";
            try (ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    System.out.printf("Cat ID: %d | Name: %s | Prods: %d | IVA: %.2f | Group: %s\n",
                        rs.getInt("category_id"), rs.getString("name"), rs.getInt("prod_count"), 
                        rs.getDouble("default_iva"), rs.getString("tax_group_id"));
                }
            }
            
            System.out.println("\n=== PRODUCTOS EN CATEGORIA 27 (Aplicando descuento) ===");
            try (ResultSet rs = stmt.executeQuery("SELECT product_id, name, tax_group_id, iva, tax_rate FROM products WHERE category_id = 27")) {
                 while (rs.next()) {
                     System.out.printf("Prod ID: %d | Name: %15s | GroupID: %s | IVA: %.2f | Rate: %.2f\n",
                        rs.getInt("product_id"), rs.getString("name"), rs.getString("tax_group_id"),
                        rs.getDouble("iva"), rs.getDouble("tax_rate"));
                 }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
