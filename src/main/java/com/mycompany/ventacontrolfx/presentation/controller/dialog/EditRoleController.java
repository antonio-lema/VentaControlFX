package com.mycompany.ventacontrolfx.presentation.controller.dialog;

import com.mycompany.ventacontrolfx.domain.model.Permission;
import com.mycompany.ventacontrolfx.domain.model.Role;
import com.mycompany.ventacontrolfx.application.usecase.RoleUseCase;
import com.mycompany.ventacontrolfx.application.usecase.PermissionUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
    private ServiceContainer container;

    // UI state
    private List<CheckBox> permissionCheckboxes = new ArrayList<>();
    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
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

            // Agrupar por prefijo (e.g. "caja", "venta")
            Map<String, List<Permission>> grouped = new TreeMap<>();
            for (Permission perm : allPerms) {
                String category = "General";
                if (perm.getCode().contains(".")) {
                    category = perm.getCode().split("\\.")[0].toUpperCase();
                } else {
                    // Fallback para c\u00f3digos sin punto
                    category = perm.getCode().toUpperCase();
                }
                grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(perm);
            }

            for (Map.Entry<String, List<Permission>> entry : grouped.entrySet()) {
                String categoryName = entry.getKey();
                List<Permission> perms = entry.getValue();

                VBox groupContainer = new VBox(8);
                groupContainer.setPadding(new Insets(10, 0, 15, 0));

                // Header de categor\u00eda con CheckBox "Seleccionar Todo"
                CheckBox chkSelectAll = new CheckBox(categoryName);
                chkSelectAll.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-primary; -fx-font-size: 14px;");

                VBox permsBox = new VBox(5);
                permsBox.setPadding(new Insets(0, 0, 0, 20));

                List<CheckBox> groupCheckboxes = new ArrayList<>();

                for (Permission perm : perms) {
                    CheckBox cb = new CheckBox(perm.getDescription());
                    cb.setId("perm_" + perm.getCode());
                    cb.getStyleClass().add("permission-checkbox");
                    cb.setUserData(perm.getCode());
                    cb.setTooltip(new Tooltip(perm.getCode()));

                    permissionCheckboxes.add(cb);
                    groupCheckboxes.add(cb);
                    permsBox.getChildren().add(cb);

                    // L\u00f3gica para actualizar el "Seleccionar Todo" si se cambia uno individual
                    cb.selectedProperty().addListener((obs, old, val) -> {
                        if (!val) {
                            chkSelectAll.setSelected(false);
                        } else {
                            boolean allSelected = groupCheckboxes.stream().allMatch(CheckBox::isSelected);
                            if (allSelected)
                                chkSelectAll.setSelected(true);
                        }
                    });
                }

                chkSelectAll.setOnAction(e -> {
                    boolean allCurrentlySelected = groupCheckboxes.stream().allMatch(CheckBox::isSelected);
                    boolean target = !allCurrentlySelected;

                    // Asegurar que el checkbox maestro refleje el nuevo estado deseado antes de
                    // disparar hijos
                    chkSelectAll.setSelected(target);
                    groupCheckboxes.forEach(cb -> cb.setSelected(target));
                });

                groupContainer.getChildren().addAll(chkSelectAll, new Separator(), permsBox);
                permissionsContainer.getChildren().add(groupContainer);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("No se pudo cargar el cat\u00e1logo de permisos.");
        }
    }

    public void setRole(Role role) {
        this.roleToEdit = role;
        if (role != null) {
            lblTitle.setText("Editar Rol: " + role.getName());
            txtRoleName.setText(role.getName());
            txtDescription.setText(role.getDescription());

            boolean isSystemRole = "admin".equalsIgnoreCase(role.getName()) || 
                                 "cajero".equalsIgnoreCase(role.getName()) ||
                                 "SUPERADMIN".equalsIgnoreCase(role.getName());
            
            boolean currentIsSuperAdmin = container.getAuthService().isSuperAdmin();

            // Bloquear la edici\u00f3n del nombre si el rol es de sistema
            if (isSystemRole) {
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

                // Si es un rol protegido y el usuario NO es SuperAdmin, bloquear cambios
                if (isSystemRole && !currentIsSuperAdmin) {
                    cb.setDisable(true);
                }
            }

            // Si es un rol protegido y el usuario NO es SuperAdmin, deshabilitar bot\u00f3n guardar
            if (isSystemRole && !currentIsSuperAdmin) {
                btnSave.setDisable(true);
                lblError.setText("Este rol est\u00e1 protegido y s\u00f3lo un SuperAdmin puede modificarlo.");
                lblError.setVisible(true);
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
                    // En este sistema no tenemos un m\u00e9todo "asigna_permisos_a_rol" directamente en
                    // el repository todav\u00eda.
                    // Oh, no a\u00f1adimos un saveRolePermissions a IRoleRepository o
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

