package com.mycompany.ventacontrolfx;

import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckDB {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement()) {

            System.out.println("=== CATEGORIES ===");
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT category_id, name, (SELECT COUNT(*) FROM products WHERE category_id = c.category_id) as pcount FROM categories c")) {
                while (rs.next()) {
                    System.out.printf("ID: %d | Name: [%s] | Prods: %d\n",
                            rs.getInt("category_id"), rs.getString("name"), rs.getInt("pcount"));
                }
            }

            System.out.println("\n=== PRODUCTS IN CAT 27 ===");
            try (ResultSet rs = stmt.executeQuery("SELECT product_id, name FROM products WHERE category_id = 27")) {
                while (rs.next()) {
                    System.out.printf("PID: %d | Name: %s\n", rs.getInt("product_id"), rs.getString("name"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
