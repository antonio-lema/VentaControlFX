package com.mycompany.ventacontrolfx.infrastructure.persistence;

import org.mindrot.jbcrypt.BCrypt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Servicio encargado de migrar contrase\u00f1as en texto plano a BCrypt.
 * Detecta si una contrase\u00f1a no tiene el prefijo de BCrypt ($2a$) y la hashea.
 */
public class BCryptMigrationService {

    public static void migratePasswords(Connection conn) {
        String query = "SELECT user_id, password_hash FROM users";

        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                int userId = rs.getInt("user_id");
                String currentPass = rs.getString("password_hash");

                // Si la contrase\u00f1a no parece un hash de BCrypt (empiezan por $2a$, $2b$ o $2y$)
                if (currentPass != null && !currentPass.startsWith("$2a$") && !currentPass.startsWith("$2b$")
                        && !currentPass.startsWith("$2y$")) {
                    System.out.println("Migrando contrase\u00f1a de usuario ID: " + userId + " a BCrypt...");

                    String hashedPassword = BCrypt.hashpw(currentPass, BCrypt.gensalt());
                    updatePassword(conn, userId, hashedPassword);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error durante la migraci\u00f3n de contrase\u00f1as: " + e.getMessage());
        }
    }

    private static void updatePassword(Connection conn, int userId, String newHash) throws SQLException {
        String updateQuery = "UPDATE users SET password_hash = ? WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {
            pstmt.setString(1, newHash);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        }
    }
}
