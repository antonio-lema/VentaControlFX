package com.mycompany.ventacontrolfx;

import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ApplyFixPermissionsTest {

    @Test
    public void applyFixes() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sql = "UPDATE permissions SET description = ? WHERE permission_id = ?";
                int totalUpdated = 0;

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    totalUpdated += executeUpdate(pstmt, "Gestionar productos y categorías", 3);
                    totalUpdated += executeUpdate(pstmt, "Configuración del sistema", 7);
                    totalUpdated += executeUpdate(pstmt, "Eliminar productos del catálogo", 3562);
                    totalUpdated += executeUpdate(pstmt, "Ver analíticas por vendedor", 3571);
                }

                conn.commit();
                System.out.println("Applied fixes to permissions! Total rows updated: " + totalUpdated);
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Transaction rolled back due to error: " + e.getMessage());
                throw e;
            }
        }
    }

    private int executeUpdate(PreparedStatement pstmt, String description, int id) throws SQLException {
        pstmt.setString(1, description);
        pstmt.setInt(2, id);
        return pstmt.executeUpdate();
    }
}
