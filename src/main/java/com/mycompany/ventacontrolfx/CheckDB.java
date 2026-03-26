package com.mycompany.ventacontrolfx;

import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CheckDB {
    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(CheckDB.class.getName());

    public static void main(String[] args) {
        int categoryId = 27; // Default
        if (args.length > 0) {
            try {
                categoryId = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                LOGGER.warning("Invalid category ID format. Using default: 27");
            }
        }

        try (Connection conn = DBConnection.getConnection()) {
            LOGGER.info("Starting Database Check...");

            System.out.println("=== CATEGORIES ===");
            String catSql = "SELECT category_id, name, (SELECT COUNT(*) FROM products WHERE category_id = c.category_id) as pcount FROM categories c";
            try (PreparedStatement pstmt = conn.prepareStatement(catSql);
                    ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    System.out.printf("ID: %d | Name: [%s] | Prods: %d\n",
                            rs.getInt("category_id"), rs.getString("name"), rs.getInt("pcount"));
                }
            }

            System.out.println("\n=== PRODUCTS IN CAT " + categoryId + " ===");
            String prodSql = "SELECT product_id, name FROM products WHERE category_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(prodSql)) {
                pstmt.setInt(1, categoryId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        System.out.printf("PID: %d | Name: %s\n", rs.getInt("product_id"), rs.getString("name"));
                    }
                }
            }

        } catch (SQLException e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Database error during check", e);
        }
    }
}
