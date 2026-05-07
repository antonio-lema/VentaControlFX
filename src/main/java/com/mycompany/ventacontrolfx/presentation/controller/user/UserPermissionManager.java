package com.mycompany.ventacontrolfx.presentation.controller.user;

import com.mycompany.ventacontrolfx.application.usecase.PermissionUseCase;
import com.mycompany.ventacontrolfx.application.usecase.RoleUseCase;
import com.mycompany.ventacontrolfx.domain.model.Permission;
import com.mycompany.ventacontrolfx.domain.model.Role;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import java.sql.SQLException;
import java.util.*;

/**
 * Gestiona la renderización dinámica y lógica de permisos de usuario.
 */
public class UserPermissionManager {

    private final ServiceContainer container;
    private final PermissionUseCase permissionUseCase;
    private final RoleUseCase roleUseCase;
    private final VBox permissionsContainer;

    private List<CheckBox> permissionCheckboxes = new ArrayList<>();
    private CheckBox chkAllowExtraPerms;

    public UserPermissionManager(ServiceContainer container, VBox permissionsContainer) {
        this.container = container;
        this.permissionUseCase = container.getPermissionUseCase();
        this.roleUseCase = container.getRoleUseCase();
        this.permissionsContainer = permissionsContainer;
    }

    public void setup(String initialRole) {
        if (permissionsContainer == null) return;
        renderPermissions();
        updateByRole(initialRole);
    }

    private void renderPermissions() {
        permissionsContainer.getChildren().clear();
        permissionCheckboxes.clear();

        chkAllowExtraPerms = new CheckBox(container.getBundle().getString("user.register.perms.extra"));
        chkAllowExtraPerms.setStyle("-fx-text-fill: -color-primary; -fx-font-weight: bold; -fx-padding: 0 0 10 0;");
        chkAllowExtraPerms.selectedProperty().addListener((obs, old, val) -> updateByRole(getCurrentRole()));
        
        permissionsContainer.getChildren().add(chkAllowExtraPerms);
        permissionsContainer.getChildren().add(new Separator());

        try {
            List<Permission> all = permissionUseCase.getAllPermissions();
            Map<String, List<Permission>> grouped = groupPermissions(all);

            for (Map.Entry<String, List<Permission>> entry : grouped.entrySet()) {
                renderGroup(entry.getKey(), entry.getValue());
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void renderGroup(String categoryName, List<Permission> perms) {
        VBox groupContainer = new VBox(8);
        groupContainer.setPadding(new Insets(10, 0, 15, 0));

        CheckBox chkSelectAll = new CheckBox(categoryName);
        chkSelectAll.setStyle("-fx-font-weight: bold; -fx-text-fill: -color-primary; -fx-font-size: 14px;");

        VBox permsBox = new VBox(5);
        permsBox.setPadding(new Insets(0, 0, 0, 20));
        List<CheckBox> groupCheckboxes = new ArrayList<>();

        for (Permission perm : perms) {
            CheckBox cb = new CheckBox(perm.getDescription());
            cb.setUserData(perm.getCode());
            cb.setTooltip(new Tooltip(perm.getCode()));
            cb.getStyleClass().add("permission-checkbox");
            
            permissionCheckboxes.add(cb);
            groupCheckboxes.add(cb);
            permsBox.getChildren().add(cb);

            cb.selectedProperty().addListener((obs, old, val) -> {
                if (!val && !cb.isDisabled()) chkSelectAll.setSelected(false);
                else if (val && groupCheckboxes.stream().allMatch(CheckBox::isSelected)) chkSelectAll.setSelected(true);
            });
        }

        chkSelectAll.setOnAction(e -> {
            boolean target = !groupCheckboxes.stream().filter(cb -> !cb.isDisabled()).allMatch(CheckBox::isSelected);
            chkSelectAll.setSelected(target);
            groupCheckboxes.forEach(cb -> { if (!cb.isDisabled()) cb.setSelected(target); });
        });

        groupContainer.getChildren().addAll(chkSelectAll, new Separator(), permsBox);
        permissionsContainer.getChildren().add(groupContainer);
    }

    private Map<String, List<Permission>> groupPermissions(List<Permission> perms) {
        Map<String, List<Permission>> grouped = new TreeMap<>();
        for (Permission perm : perms) {
            String category = perm.getCode().contains(".") ? perm.getCode().split("\\.")[0].toUpperCase() : perm.getCode().toUpperCase();
            grouped.computeIfAbsent(category, k -> new ArrayList<>()).add(perm);
        }
        return grouped;
    }

    public void updateByRole(String roleName) {
        if (roleName == null) return;
        try {
            Role role = roleUseCase.getRoleByName(roleName);
            if (role == null) return;

            List<String> rolePermCodes = role.getPermissions().stream().map(Permission::getCode).toList();
            boolean customEnabled = chkAllowExtraPerms.isSelected();

            for (CheckBox cb : permissionCheckboxes) {
                String code = cb.getUserData().toString();
                if (rolePermCodes.contains(code)) {
                    cb.setSelected(true);
                    cb.setDisable(!customEnabled);
                    cb.setStyle(customEnabled ? "" : "-fx-opacity: 0.8; -fx-text-fill: #888888; -fx-font-weight: bold;");
                } else {
                    cb.setDisable(!customEnabled);
                    cb.setStyle(customEnabled ? "" : "-fx-opacity: 0.5;");
                }
            }
            updateSelectAllHeaders();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void loadUserPermissions(int userId, String roleName, boolean hasCustom) {
        try {
            List<Permission> userPerms = permissionUseCase.getPermissionsForUser(userId);
            List<String> userCodes = userPerms.stream().map(Permission::getCode).toList();

            Role role = roleUseCase.getRoleByName(roleName);
            if (role != null) {
                List<String> roleCodes = role.getPermissions().stream().map(Permission::getCode).toList();
                if (userCodes.stream().anyMatch(c -> !roleCodes.contains(c)) || hasCustom) chkAllowExtraPerms.setSelected(true);
            } else if (hasCustom) chkAllowExtraPerms.setSelected(true);

            for (CheckBox cb : permissionCheckboxes) cb.setSelected(userCodes.contains(cb.getUserData().toString()));
            updateSelectAllHeaders();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void updateSelectAllHeaders() {
        for (javafx.scene.Node node : permissionsContainer.getChildren()) {
            if (node instanceof VBox group && group.getChildren().size() >= 3) {
                if (group.getChildren().get(0) instanceof CheckBox chkAll && group.getChildren().get(2) instanceof VBox permsBox) {
                    boolean allSelected = permsBox.getChildren().stream().filter(n -> n instanceof CheckBox).map(n -> (CheckBox) n).allMatch(CheckBox::isSelected);
                    boolean anyDisabled = permsBox.getChildren().stream().filter(n -> n instanceof CheckBox).map(n -> (CheckBox) n).anyMatch(CheckBox::isDisabled);
                    chkAll.setSelected(allSelected && !permsBox.getChildren().isEmpty());
                    chkAll.setDisable(anyDisabled);
                }
            }
        }
    }

    public List<String> getSelectedPermissionCodes() {
        return permissionCheckboxes.stream().filter(CheckBox::isSelected).map(cb -> cb.getUserData().toString()).toList();
    }

    public void setAllSelected(boolean selected) { permissionCheckboxes.forEach(cb -> cb.setSelected(selected)); }

    public void setCustomizationEnabled(boolean enabled) { chkAllowExtraPerms.setSelected(enabled); }

    public boolean isCustomizationEnabled() { return chkAllowExtraPerms.isSelected(); }

    private String getCurrentRole() { return ""; /* Se inyectará o se obtendrá del controlador */ }
}

