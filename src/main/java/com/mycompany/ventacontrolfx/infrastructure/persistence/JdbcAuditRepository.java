package com.mycompany.ventacontrolfx.infrastructure.persistence;

import com.mycompany.ventacontrolfx.domain.repository.IAuditRepository;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class JdbcAuditRepository implements IAuditRepository {

    @Override
    public void log(int userId, String action, int targetId, String oldValue, String newValue) throws SQLException {
        String sql = "INSERT INTO audit_logs (user_id, event_type, resource_name, payload_before, payload_after, ip_address) VALUES (?, ?, ?, ?, ?, ?)";
        String ip = getIpAddress();

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            if (userId > 0)
                ps.setInt(1, userId);
            else
                ps.setNull(1, java.sql.Types.INTEGER);

            ps.setString(2, action);
            ps.setString(3, "ID:" + targetId);
            ps.setString(4, oldValue);
            ps.setString(5, newValue);
            ps.setString(6, ip);

            ps.executeUpdate();
        }
    }

    @Override
    public void logAccess(int userId, String action, String resource, String description) throws SQLException {
        String sql = "INSERT INTO audit_logs (user_id, event_type, resource_name, action_description, ip_address) VALUES (?, ?, ?, ?, ?)";
        String ip = getIpAddress();

        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            if (userId > 0)
                ps.setInt(1, userId);
            else
                ps.setNull(1, java.sql.Types.INTEGER);

            ps.setString(2, action);
            ps.setString(3, resource);
            ps.setString(4, description);
            ps.setString(5, ip);

            ps.executeUpdate();
        }
    }

    private String getIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}
