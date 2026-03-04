package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.User;
import com.mycompany.ventacontrolfx.application.usecase.UserUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.SceneNavigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

public class LoginController implements Injectable {

    @FXML
    private TextField txtUsername;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private Button btnLogin;
    @FXML
    private Label lblMessage;

    private ServiceContainer container;
    private UserUseCase userUseCase;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.userUseCase = container.getUserUseCase();
        checkIfUsersExist();
    }

    private void checkIfUsersExist() {
        try {
            if (userUseCase.getUserCount() == 0) {
                lblMessage.setText("No hay usuarios registrados. Redirigiendo...");
                Platform.runLater(() -> SceneNavigator.loadScene(
                        (Stage) btnLogin.getScene().getWindow(),
                        "/view/register_user.fxml",
                        "Registro Primer Administrador",
                        900, 600,
                        false,
                        container));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        try {
            boolean valid = userUseCase.validateLogin(username, password);
            if (valid) {
                User user = userUseCase.getUserByUsername(username);
                java.util.List<com.mycompany.ventacontrolfx.domain.model.Permission> perms = new java.util.ArrayList<>();

                // Cargar Rol y Permisos del usuario
                try {
                    String roleName = user.getRole();
                    com.mycompany.ventacontrolfx.domain.model.Role role = container.getRoleUseCase()
                            .getRoleByName(roleName);

                    if (role != null) {
                        user.setRoleObject(role);
                        // Obtener permisos de rol
                        java.util.List<com.mycompany.ventacontrolfx.domain.model.Permission> rolePerms = role
                                .getPermissions();

                        // Obtener permisos personalizados de usuario
                        java.util.List<com.mycompany.ventacontrolfx.domain.model.Permission> userPerms = container
                                .getPermissionUseCase().getPermissionsForUser(user.getUserId());

                        // Fusionar sin duplicados
                        java.util.Set<String> uniqueCodes = new java.util.HashSet<>();
                        for (com.mycompany.ventacontrolfx.domain.model.Permission p : rolePerms) {
                            if (uniqueCodes.add(p.getCode()))
                                perms.add(p);
                        }
                        for (com.mycompany.ventacontrolfx.domain.model.Permission p : userPerms) {
                            if (uniqueCodes.add(p.getCode()))
                                perms.add(p);
                        }
                    } else {
                        // COMPATIBILIDAD: Cargar directamente de la tabla user_permissions
                        perms.addAll(container.getPermissionUseCase().getPermissionsForUser(user.getUserId()));
                    }

                    // MIGRACIÓN: Si es admin y no tiene permisos, asignamos todos
                    if (perms.isEmpty() && ("admin".equalsIgnoreCase(user.getRole())
                            || "Administrador".equalsIgnoreCase(user.getRole()))) {
                        java.util.List<com.mycompany.ventacontrolfx.domain.model.Permission> allPerms = container
                                .getPermissionUseCase().getAllPermissions();
                        java.util.List<String> allCodes = allPerms.stream()
                                .map(com.mycompany.ventacontrolfx.domain.model.Permission::getCode)
                                .toList();
                        container.getPermissionUseCase().savePermissionsForUser(user.getUserId(), allCodes);
                        perms = allPerms;
                    }
                    user.setPermissions(perms);
                } catch (Exception ex) {
                    System.err.println("Advertencia al cargar permisos: " + ex.getMessage());
                }

                container.getUserSession().setCurrentUser(user);

                // Logging de Acceso (Exitoso)
                try {
                    container.getAuditRepository().logAccess(user.getUserId(), "LOGIN", "AUTH",
                            "Inicio de sesión exitoso");
                } catch (Exception ignored) {
                }

                lblMessage.setText("Login correcto 👍");
                SceneNavigator.loadScene(
                        (Stage) btnLogin.getScene().getWindow(),
                        "/view/main.fxml",
                        "TPV",
                        1200, 800,
                        true,
                        container);
            } else {
                // Logging de Acceso (Fallido)
                try {
                    container.getAuditRepository().logAccess(-1, "LOGIN_FAILED", "AUTH",
                            "Intento fallido con usuario: " + username);
                } catch (Exception ignored) {
                }

                lblMessage.setText("Usuario o contraseña incorrectos ❌");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            lblMessage.setText("Error de base de datos 💥");
        }
    }

    @FXML
    private void handleForgotPassword() {
        SceneNavigator.loadScene(
                (Stage) btnLogin.getScene().getWindow(),
                "/view/password_recovery.fxml",
                "Recuperar Contraseña",
                900, 600,
                false,
                container);
    }
}
