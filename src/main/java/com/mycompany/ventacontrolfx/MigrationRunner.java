package com.mycompany.ventacontrolfx;

import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import java.sql.Connection;
import java.sql.Statement;

public class MigrationRunner {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement()) {

            System.out.println("Starting migration...");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS price_lists (" +
                    "price_list_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(255) NOT NULL, " +
                    "is_default BOOLEAN NOT NULL DEFAULT 0, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                    ")");
            System.out.println("Created price_lists table.");

            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS product_prices (" +
                    "price_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "product_id INT NOT NULL, " +
                    "price_list_id INT NOT NULL, " +
                    "price DECIMAL(10, 2) NOT NULL, " +
                    "start_date DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "end_date DATETIME NULL DEFAULT NULL, " +
                    "reason VARCHAR(255), " +
                    "FOREIGN KEY (product_id) REFERENCES products(product_id) ON DELETE CASCADE, " +
                    "FOREIGN KEY (price_list_id) REFERENCES price_lists(price_list_id) ON DELETE CASCADE" +
                    ")");
            System.out.println("Created product_prices table.");

            try {
                stmt.executeUpdate(
                        "CREATE INDEX idx_active_price ON product_prices(product_id, price_list_id, end_date)");
            } catch (Exception e) {
                System.out.println("Index idx_active_price might already exist: " + e.getMessage());
            }

            try {
                stmt.executeUpdate("CREATE INDEX idx_product_category ON products(category_id)");
            } catch (Exception e) {
                System.out.println("Index idx_product_category might already exist: " + e.getMessage());
            }

            try {
                stmt.executeUpdate(
                        "INSERT INTO price_lists (price_list_id, name, is_default) VALUES (1, 'Tarifa Base Estándar', 1)");
            } catch (Exception e) {
                System.out.println("Default price list might already exist: " + e.getMessage());
            }

            try {
                int migrated = stmt.executeUpdate(
                        "INSERT INTO product_prices (product_id, price_list_id, price, start_date, reason) " +
                                "SELECT product_id, 1, price, NOW(), 'Migración inicial' FROM products p " +
                                "WHERE NOT EXISTS (SELECT 1 FROM product_prices pp WHERE pp.product_id = p.product_id AND pp.price_list_id = 1)");
                System.out.println("Migrated " + migrated + " prices from products to product_prices.");
            } catch (Exception e) {
                System.out.println("Price column might not exist for migration: " + e.getMessage());
            }

            System.out.println("Migration complete!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
