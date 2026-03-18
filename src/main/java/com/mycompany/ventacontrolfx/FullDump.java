package com.mycompany.ventacontrolfx;

import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class FullDump {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement()) {

            System.out.println("=== TODOS LOS GRUPOS DE IMPUESTOS ===");
            try (ResultSet rs = stmt.executeQuery("SELECT * FROM tax_groups")) {
                while (rs.next()) {
                    System.out.printf("ID: %d | Name: %s | Default: %b\n",
                            rs.getInt("tax_group_id"), rs.getString("name"), rs.getBoolean("is_default"));
                }
            }

            System.out.println("\n=== TODAS LAS CATEGORIAS (Con conteo de productos) ===");
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT c.category_id, c.name, c.tax_group_id, c.default_iva, (SELECT COUNT(*) FROM products p WHERE p.category_id = c.category_id) as pcount FROM categories c ORDER BY c.category_id ASC")) {
                while (rs.next()) {
                    System.out.printf("ID: %d | Name: %20s | GroupID: %s | IVA: %.2f | Prods: %d\n",
                            rs.getInt("category_id"), rs.getString("name"), rs.getString("tax_group_id"),
                            rs.getDouble("default_iva"), rs.getInt("pcount"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
