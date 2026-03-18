package com.mycompany.ventacontrolfx;

import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class LogDump {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement()) {

            System.out.println("=== PRICE UPDATE LOGS ===");
            try (ResultSet rs = stmt.executeQuery("SELECT * FROM price_update_log ORDER BY applied_at DESC LIMIT 20")) {
                while (rs.next()) {
                    System.out.printf(
                            "ID: %d | Type: %s | Scope: %s | CatID: %d | Value: %.4f | Prods: %d | Reason: %s | Date: %s\n",
                            rs.getInt("log_id"),
                            rs.getString("update_type"),
                            rs.getString("scope"),
                            rs.getInt("category_id"),
                            rs.getDouble("value"),
                            rs.getInt("products_updated"),
                            rs.getString("reason"),
                            rs.getTimestamp("applied_at"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
