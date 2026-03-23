package com.mycompany.ventacontrolfx;

import com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection;
import org.junit.jupiter.api.Test;

import java.sql.*;

public class FixPermissionsTest {

    @Test
    public void applyFixes() throws SQLException {
        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false); // Transaccional

            try (Statement stmt = conn.createStatement()) {

                // --- 1. VENDEDOR ---
                // Remover: caja.abrir, caja.cerrar, venta.devolucion
                System.out.println("Actualizando Vendedor...");
                String removeVendedor = "DELETE rp FROM role_permissions rp " +
                        "JOIN roles r ON rp.role_id = r.role_id " +
                        "JOIN permissions p ON rp.permission_id = p.permission_id " +
                        "WHERE r.name = 'Vendedor' AND p.code IN ('caja.abrir', 'caja.cerrar', 'venta.devolucion')";
                int deletedVendedor = stmt.executeUpdate(removeVendedor);
                System.out.println("  - Removidos " + deletedVendedor + " permisos.");

                // Añadir: venta.crear, venta.ticket, venta.limpiar, venta.aplazar
                String addVendedor = "INSERT IGNORE INTO role_permissions (role_id, permission_id) " +
                        "SELECT r.role_id, p.permission_id FROM roles r, permissions p " +
                        "WHERE r.name = 'Vendedor' AND p.code IN ('venta.crear', 'venta.ticket', 'venta.limpiar', 'venta.aplazar')";
                int addedVendedor = stmt.executeUpdate(addVendedor);
                System.out.println("  + Añadidos " + addedVendedor + " permisos.");

                // --- 2. ALMACENERO ---
                // Remover: caja.ingresar, venta.aplazar, venta.limpiar
                System.out.println("\nActualizando Almacenero...");
                String removeAlmacenero = "DELETE rp FROM role_permissions rp " +
                        "JOIN roles r ON rp.role_id = r.role_id " +
                        "JOIN permissions p ON rp.permission_id = p.permission_id " +
                        "WHERE r.name = 'Almacenero' AND p.code IN ('caja.ingresar', 'venta.aplazar', 'venta.limpiar')";
                int deletedAlmacenero = stmt.executeUpdate(removeAlmacenero);
                System.out.println("  - Removidos " + deletedAlmacenero + " permisos.");

                // --- 3. SUPERVISOR ---
                // Añadir: venta.crear, venta.ticket, venta.limpiar, venta.descuento
                System.out.println("\nActualizando Supervisor...");
                String addSupervisor = "INSERT IGNORE INTO role_permissions (role_id, permission_id) " +
                        "SELECT r.role_id, p.permission_id FROM roles r, permissions p " +
                        "WHERE r.name = 'Supervisor' AND p.code IN ('venta.crear', 'venta.ticket', 'venta.limpiar', 'venta.descuento')";
                int addedSupervisor = stmt.executeUpdate(addSupervisor);
                System.out.println("  + Añadidos " + addedSupervisor + " permisos.");

                // --- 4. CAJERO ---
                // Remover: venta.devolucion
                System.out.println("\nActualizando Cajero...");
                String removeCajero = "DELETE rp FROM role_permissions rp " +
                        "JOIN roles r ON rp.role_id = r.role_id " +
                        "JOIN permissions p ON rp.permission_id = p.permission_id " +
                        "WHERE r.name = 'Cajero' AND p.code = 'venta.devolucion'";
                int deletedCajero = stmt.executeUpdate(removeCajero);
                System.out.println("  - Removidos " + deletedCajero + " permisos.");

                conn.commit();
                System.out.println("\n¡Actualización completada con éxito!");

            } catch (SQLException e) {
                conn.rollback();
                System.err.println("Error ejecutando la actualización: " + e.getMessage());
                throw e;
            }
        }
    }
}
