package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.Permission;
import com.mycompany.ventacontrolfx.domain.model.Role;
import com.mycompany.ventacontrolfx.application.usecase.RoleUseCase;
import com.mycompany.ventacontrolfx.application.usecase.PermissionUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EditRoleController implements Injectable {

    @FXML
    private Label lblTitle;
    @FXML
    private TextField txtRoleName;
    @FXML
    private TextArea txtDescription;
    @FXML
    private VBox permissionsContainer;
    @FXML
    private Label lblError;
    @FXML
    private Button btnSave;
    @FXML
    private Button btnCancel;
    @FXML
    private StackPane rootStackPane;

    private RoleUseCase roleUseCase;
    private PermissionUseCase permissionUseCase;
    private Role roleToEdit;

    // UI state
    private List<CheckBox> permissionCheckboxes = new ArrayList<>();
    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void inject(ServiceContainer container) {
        this.roleUseCase = container.getRoleUseCase();
        this.permissionUseCase = container.getPermissionUseCase();
        loadCatalogPermissions();
    }

    private void loadCatalogPermissions() {
        if (permissionsContainer == null)
            return;
        permissionsContainer.getChildren().clear();
        permissionCheckboxes.clear();

        try {
            List<Permission> allPerms = permissionUseCase.getAllPermissions();
            for (Permission perm : allPerms) {
                CheckBox cb = new CheckBox(perm.getDescription());
                cb.setId("perm_" + perm.getCode());
                cb.getStyleClass().add("permission-checkbox");
                cb.setUserData(perm.getCode());
                // Agregar un tooltip con el código
                cb.setTooltip(new Tooltip(perm.getCode()));
                permissionCheckboxes.add(cb);
                permissionsContainer.getChildren().add(cb);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("No se pudo cargar el catálogo de permisos.");
        }
    }

    public void setRole(Role role) {
        this.roleToEdit = role;
        if (role != null) {
            lblTitle.setText("Editar Rol: " + role.getName());
            txtRoleName.setText(role.getName());
            txtDescription.setText(role.getDescription());

            // Bloquear la edición del nombre si el rol es 'admin' o 'cajero' para evitar
            // romper lógica dura
            if ("admin".equalsIgnoreCase(role.getName()) || "cajero".equalsIgnoreCase(role.getName())) {
                txtRoleName.setDisable(true);
            }

            // Marcar los permisos que el rol ya tiene
            List<String> roleCodes = role.getPermissions().stream()
                    .map(Permission::getCode)
                    .toList();

            for (CheckBox cb : permissionCheckboxes) {
                if (roleCodes.contains((String) cb.getUserData())) {
                    cb.setSelected(true);
                }
            }
        }
    }

    @FXML
    private void handleSave() {
        String name = txtRoleName.getText().trim();
        String description = txtDescription.getText().trim();

        if (name.isEmpty()) {
            showError("Debe escribir un nombre para el rol.");
            return;
        }

        List<String> selectedPermissions = new ArrayList<>();
        for (CheckBox cb : permissionCheckboxes) {
            if (cb.isSelected()) {
                selectedPermissions.add((String) cb.getUserData());
            }
        }

        try {
            if (roleToEdit == null) {
                // Modo crear nuevo rol
                Role existing = roleUseCase.getRoleByName(name);
                if (existing != null) {
                    showError("Ya existe un rol con ese nombre.");
                    return;
                }

                Role newRole = new Role(0, name, description);
                if (roleUseCase.createRole(newRole)) {
                    // Update the automatically loaded ID into newRole
                    // En este sistema no tenemos un método "asigna_permisos_a_rol" directamente en
                    // el repository todavía.
                    // Oh, no añadimos un saveRolePermissions a IRoleRepository o
                    // PermissionRepository!
                    saveRolePermissions(newRole.getRoleId(), selectedPermissions);
                    closeWindow();
                } else {
                    showError("No se pudo guardar el rol.");
                }
            } else {
                // Modo actualizar rol existente
                roleToEdit.setName(name);
                roleToEdit.setDescription(description);

                if (roleUseCase.updateRole(roleToEdit)) {
                    saveRolePermissions(roleToEdit.getRoleId(), selectedPermissions);
                    closeWindow();
                } else {
                    showError("Error al actualizar el rol.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error en BD: " + e.getMessage());
        }
    }

    private void saveRolePermissions(int roleId, List<String> codes) throws SQLException {
        // Necesitamos implementar setRolePermissions en PermissionRepository
        // Por ahora, usamos una llamada temporal que tendremos que crear:
        permissionUseCase.savePermissionsForRole(roleId, codes);
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
        lblError.setManaged(true);
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
