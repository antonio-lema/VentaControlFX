
package com.mycompany.ventacontrolfx.infrastructure.persistence;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseMigration {

    private static final int CURRENT_VERSION = 2; // Incrementar al añadir nuevas columnas

    public static void ensureSchemaUpdate() {
        try (Connection conn = DBConnection.getConnection()) {
            // Asegurar que exista al menos un SUPERADMIN en la base de datos (se ejecuta siempre en el arranque)
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE UPPER(role) = 'SUPERADMIN'")) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        System.out.println("[Migration] No se detecta ningun SUPERADMIN. Promocionando primer administrador...");
                        int adminId = -1;
                        try (ResultSet rsAdmin = stmt.executeQuery("SELECT user_id FROM users WHERE UPPER(role) = 'ADMIN' OR UPPER(role) = 'ADMINISTRADOR' ORDER BY user_id ASC LIMIT 1")) {
                            if (rsAdmin.next()) {
                                adminId = rsAdmin.getInt(1);
                            }
                        }
                        if (adminId != -1) {
                            stmt.executeUpdate("UPDATE users SET role = 'SUPERADMIN' WHERE user_id = " + adminId);
                            System.out.println("[Migration] Usuario con ID " + adminId + " promovido con exito a SUPERADMIN.");
                        } else {
                            int firstUserId = -1;
                            try (ResultSet rsFirst = stmt.executeQuery("SELECT user_id FROM users ORDER BY user_id ASC LIMIT 1")) {
                                if (rsFirst.next()) {
                                    firstUserId = rsFirst.getInt(1);
                                }
                            }
                            if (firstUserId != -1) {
                                stmt.executeUpdate("UPDATE users SET role = 'SUPERADMIN' WHERE user_id = " + firstUserId);
                                System.out.println("[Migration] Primer usuario de la BD (ID " + firstUserId + ") promovido con exito a SUPERADMIN.");
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                System.err.println("[Migration] Error al verificar/promover SUPERADMIN: " + ex.getMessage());
            }

            int installedVersion = getDatabaseVersion(conn);
            if (installedVersion >= CURRENT_VERSION) {
                return; // Schema is up to date, skip checks
            }

            System.out.println(
                    "[Migration] Updating schema from v" + installedVersion + " to v" + CURRENT_VERSION + "...");

            // 1. Columnas de precisión decimal
            ensureColumn(conn, "products", "decimals", "INT DEFAULT NULL");
            ensureColumn(conn, "categories", "decimals", "INT DEFAULT NULL");

            // 2. Columnas Verifactu
            ensureColumn(conn, "sales", "fiscal_status", "VARCHAR(50) DEFAULT 'PENDING'");
            ensureColumn(conn, "sales", "fiscal_msg", "TEXT");
            ensureColumn(conn, "sales", "xml_sent", "LONGTEXT");
            ensureColumn(conn, "sales", "xml_received", "LONGTEXT");
            ensureColumn(conn, "sales", "incident_reason", "TEXT");

            ensureColumn(conn, "returns", "fiscal_status", "VARCHAR(50) DEFAULT 'PENDING'");
            ensureColumn(conn, "returns", "fiscal_msg", "TEXT");
            ensureColumn(conn, "returns", "xml_sent", "LONGTEXT");
            ensureColumn(conn, "returns", "xml_received", "LONGTEXT");
            ensureColumn(conn, "returns", "incident_reason", "TEXT");

            // 3. Tabla de logs de Verifactu
            String logTableSql = "CREATE TABLE IF NOT EXISTS verifactu_log (" +
                    "log_id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "entity_type VARCHAR(20) NOT NULL, " +
                    "entity_id INT NOT NULL, " +
                    "operation_type VARCHAR(20) NOT NULL, " +
                    "status VARCHAR(50) NOT NULL, " +
                    "message TEXT, " +
                    "xml_sent LONGTEXT, " +
                    "xml_received LONGTEXT, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(logTableSql);
            }

            setDatabaseVersion(conn, CURRENT_VERSION);
            System.out.println("[Migration] Schema updated successfully.");

        } catch (Exception e) {
            System.err.println("[Migration] Error actualizando esquema: " + e.getMessage());
        }
    }

    private static int getDatabaseVersion(Connection conn) {
        String sql = "SELECT config_value FROM system_config WHERE config_key = 'db.version'";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next())
                return Integer.parseInt(rs.getString("config_value"));
        } catch (Exception e) {
        }
        return 0;
    }

    private static void setDatabaseVersion(Connection conn, int version) {
        String sql = "INSERT INTO system_config (config_key, config_value) VALUES ('db.version', ?) " +
                "ON DUPLICATE KEY UPDATE config_value = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, String.valueOf(version));
            pstmt.setString(2, String.valueOf(version));
            pstmt.executeUpdate();
        } catch (Exception e) {
        }
    }

    private static void ensureColumn(Connection conn, String table, String column, String definition)
            throws SQLException {
        if (!columnExists(conn, table, column)) {
            String sql = "ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition;
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(sql);
                System.out.println("[Migration] A\u00f1adida columna '" + column + "' a la tabla '" + table + "'");
            } catch (SQLException e) {
                System.err.println("[Migration] Error a\u00f1adiendo columna " + column + ": " + e.getMessage());
            }
        }
    }

    private static boolean columnExists(Connection conn, String table, String column) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, table, column)) {
            return rs.next();
        }
    }
}
