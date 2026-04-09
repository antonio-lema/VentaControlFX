package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.repository.IAppSettingsRepository;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class JdbcAppSettingsRepository implements IAppSettingsRepository {

    @Override
    public String getSetting(String key) throws SQLException {
        String sql = "SELECT setting_value FROM app_settings WHERE setting_key = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("setting_value");
                }
            }
        }
        return null;
    }

    @Override
    public void saveSetting(String key, String value) throws SQLException {
        String sql = "INSERT INTO app_settings (setting_key, setting_value) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE setting_value = ?, updated_at = CURRENT_TIMESTAMP";
        // Nota: ON DUPLICATE KEY es MySQL. Para SQLite usamos INSERT OR REPLACE.
        // Como el proyecto parece usar SQLite/H2 estÃ¡ndar en algunos puntos, usarÃ© una
        // lÃ³gica mÃ¡s portable.

        try (Connection conn = DBConnection.getConnection()) {
            boolean exists = false;
            try (PreparedStatement checkPs = conn
                    .prepareStatement("SELECT 1 FROM app_settings WHERE setting_key = ?")) {
                checkPs.setString(1, key);
                try (ResultSet rs = checkPs.executeQuery()) {
                    exists = rs.next();
                }
            }

            if (exists) {
                try (PreparedStatement updatePs = conn.prepareStatement(
                        "UPDATE app_settings SET setting_value = ?, updated_at = CURRENT_TIMESTAMP WHERE setting_key = ?")) {
                    updatePs.setString(1, value);
                    updatePs.setString(2, key);
                    updatePs.executeUpdate();
                }
            } else {
                try (PreparedStatement insertPs = conn
                        .prepareStatement("INSERT INTO app_settings (setting_key, setting_value) VALUES (?, ?)")) {
                    insertPs.setString(1, key);
                    insertPs.setString(2, value);
                    insertPs.executeUpdate();
                }
            }
        }
    }

    @Override
    public Map<String, String> getAllSettings() throws SQLException {
        Map<String, String> settings = new HashMap<>();
        String sql = "SELECT setting_key, setting_value FROM app_settings";
        try (Connection conn = DBConnection.getConnection();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                settings.put(rs.getString("setting_key"), rs.getString("setting_value"));
            }
        }
        return settings;
    }
}
