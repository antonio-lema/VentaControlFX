package com.mycompany.ventacontrolfx;

import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import java.sql.Connection;
import java.sql.Statement;

public class DropPriceMigration {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement()) {

            System.out.println("Starting final price column drop migration...");

            try {
                stmt.executeUpdate("ALTER TABLE products DROP COLUMN price");
                System.out.println("Column 'price' dropped from 'products'.");
            } catch (Exception e) {
                System.out.println("Column 'price' might already be dropped: " + e.getMessage());
            }

            System.out.println("Migration complete!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
