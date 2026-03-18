package com.mycompany.ventacontrolfx;

import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class FirstCats {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement()) {

            System.out.println("=== CATEGORIAS 1-30 ===");
            String sql = "SELECT c.category_id, c.name, (SELECT COUNT(*) FROM products p WHERE p.category_id = c.category_id) as prod_count, c.default_iva, c.tax_group_id FROM categories c LIMIT 30";
            try (ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    System.out.printf("Cat ID: %d | Name: %20s | Prods: %d | IVA: %.2f | Group: %s\n",
                            rs.getInt("category_id"), rs.getString("name"), rs.getInt("prod_count"),
                            rs.getDouble("default_iva"), rs.getString("tax_group_id"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
