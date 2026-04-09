package com.mycompany.ventacontrolfx.util;

import com.mycompany.ventacontrolfx.domain.model.User;

/**
 * Servicio de autorizaciÃ³n granular.
 * Los permisos del usuario se cargan al hacer login y viven en la UserSession.
 * Esto evita consultas repetidas a la BD y es suficiente para una app de
 * escritorio.
 *
 * CÃ³digos de permiso disponibles:
 * VENTAS, HISTORIAL, PRODUCTOS, CLIENTES, CIERRES, USUARIOS, CONFIGURACION
 */
public class AuthorizationService {
    private final UserSession userSession;

    public AuthorizationService(UserSession userSession) {
        this.userSession = userSession;
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // MÃ©todos de consulta
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

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
     * Nota: el rol "admin" ya NO implica permisos automÃ¡ticos en la lÃ³gica normal,
     * pero aquÃ­ lo usamos como salvaguarda para la UI.
     */
    public boolean isAdmin() {
        User user = userSession.getCurrentUser();
        if (user == null)
            return false;
        String role = user.getRole();
        return role != null && ("admin".equalsIgnoreCase(role) || "Administrador".equalsIgnoreCase(role));
    }

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // MÃ©todos de acciÃ³n protegida
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    /**
     * Ejecuta una acciÃ³n solo si el usuario tiene el permiso indicado.
     * Si no lo tiene, muestra un error al usuario.
     *
     * @param permissionCode CÃ³digo del permiso requerido (e.g. "PRODUCTOS")
     * @param action         AcciÃ³n a ejecutar si el permiso existe
     */
    public void requirePermission(String permissionCode, Runnable action) {
        if (hasPermission(permissionCode)) {
            action.run();
        } else {
            AlertUtil.showError(
                    "Acceso Denegado",
                    "No tienes permiso para acceder a esta secciÃ³n.\n" +
                            "Contacta con un administrador si crees que es un error.");
        }
    }

    /**
     * Lanza una excepciÃ³n si el usuario no tiene el permiso indicado.
     * Ãštil para UseCases.
     */
    public void checkPermission(String permissionCode) throws SecurityException {
        if (!hasPermission(permissionCode)) {
            throw new SecurityException("Acceso denegado: falta el permiso " + permissionCode);
        }
    }

    /**
     * Conservado por compatibilidad con cÃ³digo existente.
     * Ahora comprueba el permiso "USUARIOS" en lugar de solo el rol.
     */
    public void checkAdminAccess(Runnable action) {
        requirePermission("USUARIOS", action);
    }
}
