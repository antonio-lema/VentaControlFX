package com.mycompany.ventacontrolfx.util;

import com.mycompany.ventacontrolfx.domain.model.User;

/**
 * Servicio de autorización granular.
 * Los permisos del usuario se cargan al hacer login y viven en la UserSession.
 * Esto evita consultas repetidas a la BD y es suficiente para una app de
 * escritorio.
 *
 * Códigos de permiso disponibles:
 * VENTAS, HISTORIAL, PRODUCTOS, CLIENTES, CIERRES, USUARIOS, CONFIGURACION
 */
public class AuthorizationService {
    private final UserSession userSession;

    public AuthorizationService(UserSession userSession) {
        this.userSession = userSession;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Métodos de consulta
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Comprueba si el usuario logado tiene un permiso concreto.
     * Ejemplo: authService.hasPermission("VENTAS")
     */
    public boolean hasPermission(String code) {
        User user = userSession.getCurrentUser();
        if (user == null)
            return false;

        // Super-user shortcut
        if (isAdmin())
            return true;

        return user.hasPermission(code);
    }

    /**
     * Comprueba si el rol del usuario es admin.
     * Nota: el rol "admin" ya NO implica permisos automáticos en la lógica normal,
     * pero aquí lo usamos como salvaguarda para la UI.
     */
    public boolean isAdmin() {
        User user = userSession.getCurrentUser();
        if (user == null)
            return false;
        String role = user.getRole();
        return role != null && ("admin".equalsIgnoreCase(role) || "Administrador".equalsIgnoreCase(role));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Métodos de acción protegida
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Ejecuta una acción solo si el usuario tiene el permiso indicado.
     * Si no lo tiene, muestra un error al usuario.
     *
     * @param permissionCode Código del permiso requerido (e.g. "PRODUCTOS")
     * @param action         Acción a ejecutar si el permiso existe
     */
    public void requirePermission(String permissionCode, Runnable action) {
        if (hasPermission(permissionCode)) {
            action.run();
        } else {
            AlertUtil.showError(
                    "Acceso Denegado",
                    "No tienes permiso para acceder a esta sección.\n" +
                            "Contacta con un administrador si crees que es un error.");
        }
    }

    /**
     * Conservado por compatibilidad con código existente.
     * Ahora comprueba el permiso "USUARIOS" en lugar de solo el rol.
     */
    public void checkAdminAccess(Runnable action) {
        requirePermission("USUARIOS", action);
    }
}
