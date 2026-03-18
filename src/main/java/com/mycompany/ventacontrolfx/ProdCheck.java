package com.mycompany.ventacontrolfx;

import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class ProdCheck {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            System.out.println("=== ANALISIS PRODUCTOS CATEGORIA 5 (Tecnología) ===");
            String sql = "SELECT product_id, name, tax_group_id, iva, tax_rate FROM products WHERE category_id = 5";
            try (ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    System.out.printf("ID: %d | Name: %15s | GroupID: %s | IVA: %.2f | Rate: %.2f\n",
                        rs.getInt("product_id"), rs.getString("name"), rs.getString("tax_group_id"),
                        rs.getDouble("iva"), rs.getDouble("tax_rate"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
