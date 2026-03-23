package com.mycompany.ventacontrolfx;

import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ApplyFixAllTablesTest {

    @Test
    public void applyFixes() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int totalUpdated = 0;

                // 1. Fix 'roles' table descriptions
                String sqlRoles = "UPDATE roles SET description = ? WHERE role_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sqlRoles)) {
                    totalUpdated += executeUpdate(pstmt, "Gestión de tienda, stock y cierres", 473);
                    totalUpdated += executeUpdate(pstmt, "Atención al cliente y ventas", 475);
                    totalUpdated += executeUpdate(pstmt, "Gestión de inventario y productos", 476);
                    totalUpdated += executeUpdate(pstmt, "Acceso básico a ventas", 477);
                }

                // 2. Fix 'system_config'
                String sqlConfig = "UPDATE system_config SET config_value = ? WHERE config_key = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sqlConfig)) {
                    pstmt.setString(1, "EUR € - Euro (€)");
                    pstmt.setString(2, "currency");
                    totalUpdated += pstmt.executeUpdate();

                    pstmt.setString(1, "¡Gracias por su compra!");
                    pstmt.setString(2, "footerMessage");
                    totalUpdated += pstmt.executeUpdate();

                    pstmt.setString(1, "58mm (Datáfono/Portátil)");
                    pstmt.setString(2, "ticketFormat");
                    totalUpdated += pstmt.executeUpdate();
                }

                // 3. Fix 'tax_rates'
                String sqlTax = "UPDATE tax_rates SET country = 'España' WHERE country LIKE '%Espa??a%'";
                try (PreparedStatement pstmt = conn.prepareStatement(sqlTax)) {
                    totalUpdated += pstmt.executeUpdate();
                }

                // 4. Fix 'sale_details' category_name_snapshot (Historical cleanup)
                String sqlDetailsGraficas = "UPDATE sale_details SET category_name_snapshot = 'Tarjetas gráficas' WHERE category_name_snapshot LIKE '%Tarjetas gr??ficas%'";
                String sqlDetailsPortatiles = "UPDATE sale_details SET category_name_snapshot = 'Portátiles' WHERE category_name_snapshot LIKE '%Port??tiles%'";
                try (PreparedStatement pstmt = conn.prepareStatement(sqlDetailsGraficas)) {
                    totalUpdated += pstmt.executeUpdate();
                }
                try (PreparedStatement pstmt = conn.prepareStatement(sqlDetailsPortatiles)) {
                    totalUpdated += pstmt.executeUpdate();
                }

                conn.commit();
                System.out.println(
                        "Applied fixes! Total rows updated (approx with specific statements): " + totalUpdated);
            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Transaction rolled back due to error: " + e.getMessage());
                throw e;
            }
        }
    }

    private int executeUpdate(PreparedStatement pstmt, String description, int roleId) throws SQLException {
        pstmt.setString(1, description);
        pstmt.setInt(2, roleId);
        return pstmt.executeUpdate();
    }
}
