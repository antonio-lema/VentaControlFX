package com.mycompany.ventacontrolfx.presentation.controller.sidebar;

import com.mycompany.ventacontrolfx.infrastructure.security.AuthorizationService;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import java.util.Map;

/**
 * Gestor de seguridad y visibilidad basada en roles para el Sidebar.
 */
public class SidebarSecurityManager {

    private final AuthorizationService auth;

    public SidebarSecurityManager(AuthorizationService auth) {
        this.auth = auth;
    }

    public void applyPermissions(Map<Button, String> buttonPermissions, Map<Button, VBox> sections) {
        if (auth == null) return;

        // 1. Aplicar permisos a botones individuales
        buttonPermissions.forEach((btn, perm) -> {
            boolean has = perm == null || perm.isEmpty() || auth.hasPermission(perm) 
                          || (perm.contains("|") && checkMulti(perm));
            setVisible(btn, has);
        });

        // 2. Ocultar secciones si no tienen hijos visibles
        sections.forEach(this::updateSectionVisibility);
    }

    private boolean checkMulti(String perms) {
        for (String p : perms.split("\\|")) {
            if (auth.hasPermission(p.trim())) return true;
        }
        return false;
    }

    public void updateSectionVisibility(Button header, VBox content) {
        if (content == null || header == null) return;
        boolean anyVisible = false;
        for (Node node : content.getChildren()) {
            if (node.isVisible()) { anyVisible = true; break; }
        }
        setVisible(header, anyVisible);
        if (!anyVisible) setVisible(content, false);
    }

    private void setVisible(Node node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }
}

