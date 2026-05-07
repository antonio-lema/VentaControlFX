package com.mycompany.ventacontrolfx.domain.repository;

import java.sql.SQLException;
import java.util.Map;

public interface IAppSettingsRepository {
    String getSetting(String key) throws SQLException;

    default String getSetting(String key, String defaultValue) throws SQLException {
        String val = getSetting(key);
        return val != null ? val : defaultValue;
    }

    void saveSetting(String key, String value) throws SQLException;

    Map<String, String> getAllSettings() throws SQLException;
}

