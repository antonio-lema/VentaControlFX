package com.mycompany.ventacontrolfx.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConfigDAO {

    public String getValue(String key) {
        String sql = "SELECT config_value FROM system_config WHERE config_key = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, key);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("config_value");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Return null if not found or error
    }

    // Method to update or insert a config value (for future settings screen)
    public void setValue(String key, String value) {
        String sql = "INSERT INTO system_config (config_key, config_value) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE config_value = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.setString(3, value);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
