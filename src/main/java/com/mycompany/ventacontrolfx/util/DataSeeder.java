package com.mycompany.ventacontrolfx.util;

import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import com.mycompany.ventacontrolfx.infrastructure.persistence.JdbcProductRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DataSeeder {

    public static void main(String[] args) {
        boolean isCleanup = args.length > 0 && args[0].equalsIgnoreCase("cleanup");
        
        if (isCleanup) {
            System.out.println("Starting cleanup of stress test products...");
            cleanup();
            return;
        }

        System.out.println("Starting 20,000 products seeding process...");
        
        try (Connection conn = DBConnection.getConnection()) {
            // 1. Ensure we have at least one category
            int categoryId = ensureCategoryExists(conn, "CATEGORIA_PRUEBA_MASIVA");
            System.out.println("Using Category ID: " + categoryId);

            JdbcProductRepository repo = new JdbcProductRepository();
            int totalToInsert = 20000;
            int batchSize = 1000;
            Random random = new Random();

            for (int i = 0; i < totalToInsert; i += batchSize) {
                List<Product> batch = new ArrayList<>();
                for (int j = 0; j < batchSize; j++) {
                    int productNum = i + j + 1;
                    String sku = "SKU-" + String.format("%06d", productNum);
                    double price = 1.0 + (99.0 * random.nextDouble());
                    
                    Product p = new Product();
                    p.setCategoryId(categoryId);
                    p.setName("Producto de Prueba #" + productNum);
                    p.setSku(sku);
                    p.setPrice(Math.round(price * 100.0) / 100.0);
                    p.setCostPrice(Math.round(price * 0.6 * 100.0) / 100.0);
                    p.setVisible(true);
                    p.setActive(true);
                    p.setFavorite(random.nextDouble() < 0.05); // 5% favorites
                    p.setManageStock(true);
                    p.setStockQuantity(random.nextInt(500));
                    p.setMinStock(10);
                    p.setIva(21.0);
                    p.setTaxGroupId(1); // Normal IVA Group
                    
                    batch.add(p);
                }
                
                System.out.println("Inserting batch " + ((i / batchSize) + 1) + "/" + (totalToInsert / batchSize) + "...");
                repo.saveAll(batch);
            }

            System.out.println("\nSUCCESS: 20,000 products inserted successfully!");
            
        } catch (Exception e) {
            System.err.println("CRITICAL ERROR during seeding:");
            e.printStackTrace();
        }
    }

    private static void cleanup() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            
            System.out.println("Cleaning up prices...");
            stmt.executeUpdate("DELETE pp FROM product_prices pp JOIN products p ON pp.product_id = p.product_id WHERE p.name LIKE 'Stress Test Product %' OR p.name LIKE 'Producto de Prueba %'");
            
            System.out.println("Cleaning up products...");
            int deleted = stmt.executeUpdate("DELETE FROM products WHERE name LIKE 'Stress Test Product %' OR name LIKE 'Producto de Prueba %'");
            
            System.out.println("Cleaning up category...");
            stmt.executeUpdate("DELETE FROM categories WHERE name = 'CATEGORIA_PRUEBA_MASIVA' OR name = 'TEST_LOAD_CATEGORY'");

            System.out.println("SUCCESS: " + deleted + " test products removed.");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static int ensureCategoryExists(Connection conn, String name) throws Exception {
        String checkSql = "SELECT category_id FROM categories WHERE name = ? LIMIT 1";
        try (PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
            pstmt.setString(1, name);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }

        String insertSql = "INSERT INTO categories (name, visible, default_iva) VALUES (?, 1, 21.0)";
        try (PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return 1; // Fallback
    }
}
