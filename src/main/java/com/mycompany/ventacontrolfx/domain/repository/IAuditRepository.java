package com.mycompany.ventacontrolfx.domain.repository;

import java.sql.SQLException;

public interface IAuditRepository {
    void log(int userId, String action, int targetId, String oldValue, String newValue) throws SQLException;

    void logAccess(int userId, String action, String resource, String description) throws SQLException;
}
