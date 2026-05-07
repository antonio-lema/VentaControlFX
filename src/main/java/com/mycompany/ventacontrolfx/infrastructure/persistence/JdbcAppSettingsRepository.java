package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.repository.IAppSettingsRepository;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class JdbcAppSettingsRepository implements IAppSettingsRepository {

    private static Map<String, String> settingsCache = null;

    @Override
    public String getSetting(String key) throws SQLException {
        if (settingsCache != null && settingsCache.containsKey(key)) {
            return settingsCache.get(key);
        }
        String sql = "SELECT setting_value FROM app_settings WHERE setting_key = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String val = rs.getString("setting_value");
                    if (settingsCache != null)
                        settingsCache.put(key, val);
                    return val;
                }
            }
        }
        return null;
    }

    @Override
    public void saveSetting(String key, String value) throws SQLException {
        settingsCache = null; // Invalidate
        String sql = "INSERT INTO app_settings (setting_key, setting_value) VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE setting_value = ?, updated_at = CURRENT_TIMESTAMP";

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
        if (settingsCache != null) {
            return new HashMap<>(settingsCache);
        }
        Map<String, String> settings = new HashMap<>();
        String sql = "SELECT setting_key, setting_value FROM app_settings";
        try (Connection conn = DBConnection.getConnection();
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                settings.put(rs.getString("setting_key"), rs.getString("setting_value"));
            }
        }
        settingsCache = new HashMap<>(settings);
        return settings;
    }
}

