package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.Permission;
import com.mycompany.ventacontrolfx.domain.model.User;
import com.mycompany.ventacontrolfx.application.usecase.UserUseCase;
import com.mycompany.ventacontrolfx.application.usecase.PermissionUseCase;
import com.mycompany.ventacontrolfx.application.usecase.RoleUseCase;
import com.mycompany.ventacontrolfx.domain.model.Role;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.SceneNavigator;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class RegisterUserController implements Injectable {

    @FXML
    private TextField txtFullName;
    @FXML
    private TextField txtUsername;
    @FXML
    private TextField txtEmail;
    @FXML
    private ComboBox<String> cmbRole;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private PasswordField txtConfirmPassword;
    @FXML
    private Label lblError;
    @FXML
    private Button btnCancel;
    @FXML
    private Label lblTitle;
    @FXML
    private StackPane rootStackPane;
    @FXML
    private VBox permissionsContainer; // Contenedor donde se renderizan los checkboxes

    private UserUseCase userUseCase;
    private PermissionUseCase permissionUseCase;
    private RoleUseCase roleUseCase;
    private ServiceContainer container;
    private User userToEdit;
    private boolean isInitialSetup = false;
    private double xOffset = 0;
    private double yOffset = 0;

    // Lista de permisos disponibles y sus checkboxes
    private List<Permission> allPermissions = new ArrayList<>();
    private List<CheckBox> permissionCheckboxes = new ArrayList<>();
    private CheckBox chkAllowExtraPerms;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.userUseCase = container.getUserUseCase();
        this.permissionUseCase = container.getPermissionUseCase();
        this.roleUseCase = container.getRoleUseCase();

        loadRoles();
        loadAndRenderPermissions();

        cmbRole.valueProperty().addListener((obs, oldVal, newVal) -> updatePermissionsByRole(newVal));

        checkSetupMode();
    }

    private void loadRoles() {
        try {
            List<Role> roles = roleUseCase.getAllRoles();
            List<String> roleNames = roles.stream().map(Role::getName).toList();
            if (roleNames.isEmpty()) {
                // Fallback si no hay roles en BD
                roleNames = List.of("admin", "cajero");
            }
            cmbRole.setItems(FXCollections.observableArrayList(roleNames));
        } catch (SQLException e) {
            e.printStackTrace();
            cmbRole.setItems(FXCollections.observableArrayList("admin", "cajero"));
        }
    }

    private void updatePermissionsByRole(String roleName) {
        if (roleName == null)
            return;
        try {
            Role role = roleUseCase.getRoleByName(roleName);
            if (role == null)
                return;

            List<String> rolePermCodes = role.getPermissions().stream()
                    .map(Permission::getCode)
                    .toList();

            for (CheckBox cb : permissionCheckboxes) {
                String code = cb.getUserData().toString();
                if (rolePermCodes.contains(code)) {
                    cb.setSelected(true);
                    cb.setDisable(true);
                    cb.setStyle("-fx-opacity: 0.8; -fx-text-fill: #888888; -fx-font-weight: bold;");
                } else {
                    boolean customizationEnabled = chkAllowExtraPerms.isSelected();
                    cb.setDisable(!customizationEnabled);
                    cb.setStyle(customizationEnabled ? "" : "-fx-opacity: 0.5;");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Carga el catálogo de permisos desde la BD y genera checkboxes dinámicamente.
     */
    private void loadAndRenderPermissions() {
        if (permissionsContainer == null)
            return;
        permissionsContainer.getChildren().clear();
        permissionCheckboxes.clear();

        // Checkbox maestro para habilitar personalización
        chkAllowExtraPerms = new CheckBox("Habilitar permisos adicionales (fuera del rol)");
        chkAllowExtraPerms.setStyle("-fx-text-fill: -color-primary; -fx-font-weight: bold; -fx-padding: 0 0 10 0;");
        chkAllowExtraPerms.selectedProperty()
                .addListener((obs, old, val) -> updatePermissionsByRole(cmbRole.getValue()));
        permissionsContainer.getChildren().add(chkAllowExtraPerms);
        permissionsContainer.getChildren().add(new Separator());

        try {
            allPermissions = permissionUseCase.getAllPermissions();
            for (Permission perm : allPermissions) {
                CheckBox cb = new CheckBox(perm.getDescription());
                cb.setId("perm_" + perm.getCode());
                cb.getStyleClass().add("permission-checkbox");
                cb.setUserData(perm.getCode());
                permissionCheckboxes.add(cb);
                permissionsContainer.getChildren().add(cb);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void checkSetupMode() {
        try {
            if (userUseCase.getUserCount() == 0) {
                isInitialSetup = true;
                if (lblTitle != null)
                    lblTitle.setText("Configuración: Crear Administrador");
                if (btnCancel != null) {
                    btnCancel.setVisible(false);
                    btnCancel.setManaged(false);
                }
                cmbRole.setValue("admin");
                cmbRole.setDisable(true);
                // En el setup inicial marcar todos los permisos por defecto
                permissionCheckboxes.forEach(cb -> cb.setSelected(true));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setUser(User user) {
        this.userToEdit = user;
        if (user != null) {
            txtFullName.setText(user.getFullName());
            txtUsername.setText(user.getUsername());
            txtUsername.setDisable(true);
            txtEmail.setText(user.getEmail());
            String uiRole = "admin".equalsIgnoreCase(user.getRole()) ? "admin" : "cajero";
            cmbRole.setValue(uiRole);

            if (lblTitle != null)
                lblTitle.setText("Editar Usuario");

            updatePermissionsByRole(user.getRole());
            // Cargar los permisos actuales del usuario y marcar sus checkboxes
            loadUserPermissions(user.getUserId());
        }
    }

    private void loadUserPermissions(int userId) {
        try {
            List<Permission> userPerms = permissionUseCase.getPermissionsForUser(userId);
            List<String> userCodes = new ArrayList<>();
            for (Permission p : userPerms) {
                userCodes.add(p.getCode());
            }

            // Si el usuario tiene permisos que NO están en su rol, habilitamos el toggle de
            // personalización
            Role role = roleUseCase.getRoleByName(cmbRole.getValue());
            if (role != null) {
                List<String> roleCodes = role.getPermissions().stream().map(Permission::getCode).toList();
                boolean hasExtra = userCodes.stream().anyMatch(code -> !roleCodes.contains(code));
                if (hasExtra) {
                    chkAllowExtraPerms.setSelected(true);
                }
            }

            for (CheckBox cb : permissionCheckboxes) {
                cb.setSelected(userCodes.contains(cb.getUserData().toString()));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRegister() {
        String fullName = txtFullName.getText();
        String username = txtUsername.getText();
        String email = txtEmail.getText();
        String role = cmbRole.getValue();
        String pass1 = txtPassword.getText();
        String pass2 = txtConfirmPassword.getText();

        if (fullName.isEmpty() || username.isEmpty() || role == null) {
            showError("Todos los campos obligatorios deben llenarse.");
            return;
        }

        if (userToEdit == null && (pass1.isEmpty() || pass2.isEmpty())) {
            showError("La contraseña es obligatoria para nuevos usuarios.");
            return;
        }

        if (!pass1.isEmpty() || !pass2.isEmpty()) {
            if (!pass1.equals(pass2)) {
                showError("Las contraseñas no coinciden.");
                return;
            }
            if (pass1.length() < 4) {
                showError("La contraseña es muy corta (mínimo 4 caracteres).");
                return;
            }
        }

        // Recopilar los códigos de permisos marcados
        List<String> selectedPermissions = new ArrayList<>();
        for (CheckBox cb : permissionCheckboxes) {
            if (cb.isSelected()) {
                selectedPermissions.add(cb.getUserData().toString());
            }
        }

        try {
            if (userToEdit == null) {
                User newUser = new User();
                newUser.setFullName(fullName);
                newUser.setUsername(username);
                newUser.setEmail(email);
                newUser.setRole(role);
                newUser.setPassword(pass1);

                User existing = userUseCase.getUserByUsername(username);
                if (existing != null) {
                    showError("El nombre de usuario ya está en uso.");
                    return;
                }

                userUseCase.registerUser(newUser);

                // Obtener el ID del usuario recién creado para asignar permisos
                User created = userUseCase.getUserByUsername(username);
                if (created != null) {
                    permissionUseCase.savePermissionsForUser(created.getUserId(), selectedPermissions);
                }

                showSuccess("Usuario creado correctamente.");

                if (isInitialSetup) {
                    SceneNavigator.loadScene(
                            (Stage) rootStackPane.getScene().getWindow(),
                            "/view/login.fxml",
                            "Login - TPV",
                            900, 600,
                            false,
                            container);
                } else {
                    closeWindow();
                }
            } else {
                userToEdit.setFullName(fullName);
                userToEdit.setEmail(email);
                userToEdit.setRole(role);

                userUseCase.updateUser(userToEdit);
                permissionUseCase.savePermissionsForUser(userToEdit.getUserId(), selectedPermissions);

                if (!pass1.isEmpty()) {
                    userUseCase.resetPassword(userToEdit.getEmail(), pass1);
                }
                showSuccess("Usuario actualizado correctamente.");
                closeWindow();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Error: " + e.getMessage());
        }
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void showSuccess(String message) {
        AlertUtil.showInfo("Éxito", message);
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        if (rootStackPane != null && rootStackPane.getScene() != null) {
            ((Stage) rootStackPane.getScene().getWindow()).close();
        }
    }

    @FXML
    private void handleMousePressed(javafx.scene.input.MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML
    private void handleMouseDragged(javafx.scene.input.MouseEvent event) {
        Stage stage = (Stage) rootStackPane.getScene().getWindow();
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }
}
