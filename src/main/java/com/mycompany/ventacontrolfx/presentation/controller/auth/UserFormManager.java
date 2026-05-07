package com.mycompany.ventacontrolfx.presentation.controller.auth;

import com.mycompany.ventacontrolfx.domain.model.User;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;

/**
 * Gestor de validación y reglas de negocio para el formulario de usuarios.
 */
public class UserFormManager {

    private final ServiceContainer container;
    private final Label lblError;

    public UserFormManager(ServiceContainer container, Label lblError) {
        this.container = container;
        this.lblError = lblError;
    }

    public boolean validate(String fullName, String username, String role, String pass1, String pass2, boolean isEdit) {
        if (fullName.isEmpty() || username.isEmpty() || role == null) {
            showError(container.getBundle().getString("user.register.error.fields"));
            return false;
        }

        if (!isEdit && (pass1.isEmpty() || pass2.isEmpty())) {
            showError(container.getBundle().getString("user.register.error.password_required"));
            return false;
        }

        if (!pass1.isEmpty() || !pass2.isEmpty()) {
            if (!pass1.equals(pass2)) {
                showError(container.getBundle().getString("user.register.error.password_mismatch"));
                return false;
            }
            if (pass1.length() < 4) {
                showError(container.getBundle().getString("user.register.error.password_short"));
                return false;
            }
        }
        
        clearError();
        return true;
    }

    /**
     * Aplica restricciones de seguridad según quién edita a quién.
     */
    public void applySecurityConstraints(User target, TextField txtFullName, TextField txtEmail, ComboBox<String> cmbRole, javafx.scene.layout.VBox permsContainer) {
        if (target == null) return;

        boolean isPrincipalAdmin = target.getUserId() == 1 || "admin".equalsIgnoreCase(target.getUsername());
        boolean isTargetSuperAdmin = "SUPERADMIN".equalsIgnoreCase(target.getRole());
        boolean currentIsSuperAdmin = container.getAuthService().isSuperAdmin();

        // 1. El Admin principal (#1) no puede cambiar su propio rol (siempre admin)
        if (isPrincipalAdmin) {
            cmbRole.setDisable(true);
        }

        // 2. Si el objetivo es SUPERADMIN y el usuario actual NO lo es, bloqueamos todo
        if (isTargetSuperAdmin && !currentIsSuperAdmin) {
            cmbRole.setDisable(true);
            txtFullName.setDisable(true);
            txtEmail.setDisable(true);
            permsContainer.setDisable(true);
        }
    }

    private void showError(String msg) {
        if (lblError != null) {
            lblError.setText(msg);
            lblError.setVisible(true);
            lblError.setManaged(true);
        }
    }

    private void clearError() {
        if (lblError != null) {
            lblError.setVisible(false);
            lblError.setManaged(false);
        }
    }
}

