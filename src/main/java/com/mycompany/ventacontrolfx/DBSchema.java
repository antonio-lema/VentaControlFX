package com.mycompany.ventacontrolfx;

import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DBSchema {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection();
                Statement stmt = conn.createStatement()) {

            String[] tables = { "categories", "products", "price_update_log", "promotions", "promotion_items" };
            for (String table : tables) {
                System.out.println("=== SCHEMA FOR " + table + " ===");
                try (ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE " + table)) {
                    if (rs.next()) {
                        System.out.println(rs.getString(2));
                    }
                }
                System.out.println();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
