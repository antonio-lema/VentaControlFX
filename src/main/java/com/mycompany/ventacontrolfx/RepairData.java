package com.mycompany.ventacontrolfx;

import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RepairData {
    public static void main(String[] args) {
        try (Connection conn = DBConnection.getConnection()) {
            System.out.println("=== LISTANDO PROMOCIONES ACTIVAS ===");
            String sql = "SELECT * FROM promotions WHERE active = 1";
            try (PreparedStatement ps = conn.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("promotion_id");
                    System.out.println("ID: " + id + " | " + rs.getString("name"));
                    System.out.println("   Tipo: " + rs.getString("type") + " | Valor: " + rs.getDouble("value"));
                    System.out.println("   Buy: " + rs.getInt("buy_qty") + " | Free: " + rs.getInt("free_qty"));
                    System.out.println("   Scope: " + rs.getString("scope"));

                    // Affected IDs
                    String sqlItems = "SELECT item_id FROM promotion_items WHERE promotion_id = ?";
                    try (PreparedStatement psItems = conn.prepareStatement(sqlItems)) {
                        psItems.setInt(1, id);
                        try (ResultSet rsItems = psItems.executeQuery()) {
                            System.out.print("   Productos/Cat afectados (IDs): ");
                            while (rsItems.next()) {
                                System.out.print(rsItems.getInt("item_id") + " ");
                            }
                            System.out.println();
                        }
                    }
                    System.out.println("-----------------------------------");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
