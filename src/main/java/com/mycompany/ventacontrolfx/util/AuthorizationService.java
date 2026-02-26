package com.mycompany.ventacontrolfx.util;

import com.mycompany.ventacontrolfx.model.User;

/**
 * Enterprise Authorization Service.
 * Injected with UserSession to check permissions.
 */
public class AuthorizationService {
    private final UserSession userSession;

    public AuthorizationService(UserSession userSession) {
        this.userSession = userSession;
    }

    public boolean isAdmin() {
        User user = userSession.getCurrentUser();
        return user != null && "admin".equalsIgnoreCase(user.getRole());
    }

    public void checkAdminAccess(Runnable action) {
        if (isAdmin()) {
            action.run();
        } else {
            AlertUtil.showError("Acceso Denegado", "No tienes permisos de administrador para realizar esta acción.");
        }
    }
}
