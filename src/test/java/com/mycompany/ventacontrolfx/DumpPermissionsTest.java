package com.mycompany.ventacontrolfx;

import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import org.junit.jupiter.api.Test;

import java.sql.*;

public class DumpPermissionsTest {

    @Test
    public void dumpAll() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            System.out.println("\n=== ROLES ===");
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT * FROM roles")) {
                while (rs.next()) {
                    System.out.println(String.format("ID: %d | Name: %s | Description: %s | IsSystem: %b",
                            rs.getInt("role_id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getBoolean("is_system")));
                }
            }

            System.out.println("\n=== PERMISSIONS ===");
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT * FROM permissions")) {
                while (rs.next()) {
                    System.out.println(String.format("ID: %d | Code: %s | Description: %s",
                            rs.getInt("permission_id"),
                            rs.getString("code"),
                            rs.getString("description")));
                }
            }

            System.out.println("\n=== ROLE_PERMISSIONS ===");
            String sql = "SELECT r.name as role_name, p.code as perm_code " +
                    "FROM role_permissions rp " +
                    "JOIN roles r ON rp.role_id = r.role_id " +
                    "JOIN permissions p ON rp.permission_id = p.permission_id " +
                    "ORDER BY r.name, p.code";
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery(sql)) {
                String currentRole = "";
                while (rs.next()) {
                    String role = rs.getString("role_name");
                    String perm = rs.getString("perm_code");
                    if (!role.equals(currentRole)) {
                        System.out.println("\n--- " + role + " ---");
                        currentRole = role;
                    }
                    System.out.println("  - " + perm);
                }
            }
        }
    }
}
