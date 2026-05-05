package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.Role;
import com.mycompany.ventacontrolfx.application.usecase.RoleUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.component.SkeletonRoleBox;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.ModalService;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import com.mycompany.ventacontrolfx.presentation.util.RealTimeSearchBinder;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class ManageRolesController implements Injectable {

    @FXML
    private FlowPane roleCardsPane;
    @FXML
    private TextField txtSearch;
    @FXML
    private Label lblCount;

    private RoleUseCase roleUseCase;
    private ServiceContainer container;
    private ObservableList<Role> roleList = FXCollections.observableArrayList();

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.roleUseCase = container.getRoleUseCase();
        loadRoles();

        if (txtSearch != null) {
            RealTimeSearchBinder.bind(txtSearch, query -> filterRoles(query));
        }
    }

    private void loadRoles() {
        if (roleCardsPane == null) return;
        
        // Mostrar esqueletos
        roleCardsPane.getChildren().clear();
        for (int i = 0; i < 3; i++) {
            roleCardsPane.getChildren().add(new SkeletonRoleBox());
        }
        
        container.getAsyncManager().runAsyncTask(() -> {
            try {
                return roleUseCase.getAllRoles();
            } catch (SQLException e) {
                return null;
            }
        }, (roles) -> {
            if (roles != null) {
                roleList.setAll((java.util.List<Role>) roles);
                renderRoleCards(roleList);
            } else {
                AlertUtil.showError("Error", "No se pudieron cargar los roles");
                roleCardsPane.getChildren().clear();
            }
        }, null);
    }

    private void renderRoleCards(List<Role> roles) {
        roleCardsPane.getChildren().clear();
        if (lblCount != null)
            lblCount.setText(roles.size() + " roles encontrados");
        for (Role role : roles) {
            roleCardsPane.getChildren().add(createRoleCard(role));
        }
    }

    private Node createRoleCard(Role role) {
        VBox card = new VBox(15);
        card.getStyleClass().add("user-card"); // Reusing user card class for styling
        card.setPrefWidth(280);
        card.setMinWidth(280);
        card.setAlignment(Pos.TOP_CENTER);

        String initial = (role.getName() != null && !role.getName().isEmpty())
                ? role.getName().substring(0, 1).toUpperCase()
                : "?";
        Label avatar = new Label(initial);
        avatar.getStyleClass().add("user-card-avatar"); // Reusing class
        avatar.setStyle(avatar.getStyle() + "; -fx-background-color: #f39c12;"); // Diferenciar con color naranja
        avatar.setPrefSize(60, 60);
        avatar.setAlignment(Pos.CENTER);

        VBox info = new VBox(5);
        info.setAlignment(Pos.CENTER);
        Label name = new Label(role.getName().toUpperCase());
        name.getStyleClass().add("user-name-label"); // Reusing class
        Label description = new Label(role.getDescription() != null ? role.getDescription() : "Sin descripci\u00f3n");
        description.getStyleClass().add("user-email-label"); // Reusing class
        info.getChildren().addAll(name, description);

        // Mostrar cantidad de permisos
        int permCount = role.getPermissions() != null ? role.getPermissions().size() : 0;
        Label lblPerms = new Label(permCount + " Permisos asignados");
        lblPerms.setStyle("-fx-text-fill: -color-primary; -fx-font-size: 13px; -fx-font-weight: bold;");
        info.getChildren().add(lblPerms);

        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER);
        actions.setPadding(new Insets(10, 0, 0, 0));

        Button btnEdit = new Button();
        boolean isSuperAdminRole = "SUPERADMIN".equalsIgnoreCase(role.getName());
        boolean currentIsSuperAdmin = container.getAuthService().isSuperAdmin();
        boolean canManageRoles = container.getUserSession().hasPermission("rol.editar");
        
        if (canManageRoles && (!isSuperAdminRole || currentIsSuperAdmin)) {
            btnEdit.getStyleClass().add("btn-action-edit");
            FontAwesomeIconView editIcon = new FontAwesomeIconView(FontAwesomeIcon.PENCIL);
            editIcon.setSize("18");
            btnEdit.setGraphic(editIcon);
            btnEdit.setOnAction(e -> handleEditSingleRole(role));
        } else {
            btnEdit.getStyleClass().add("btn-action-lock");
            btnEdit.setStyle("-fx-background-color: #64748b; -fx-text-fill: white; -fx-background-radius: 50;");
            FontAwesomeIconView lockIcon = new FontAwesomeIconView(FontAwesomeIcon.LOCK);
            lockIcon.setSize("18");
            lockIcon.setFill(Color.WHITE);
            btnEdit.setGraphic(lockIcon);
            btnEdit.setTooltip(new Tooltip(container.getBundle().getString("user.manage.btn.unlock")));
            btnEdit.setOnAction(e -> AlertUtil.showWarning(container.getBundle().getString("alert.locked"),
                    container.getBundle().getString("role.msg.locked_edit")));
        }

        // Disable editing for "admin" and "SUPERADMIN" role to ensure immutability from non-superadmins
        if ("admin".equalsIgnoreCase(role.getName()) || isSuperAdminRole) {
            if (!currentIsSuperAdmin) {
                btnEdit.setDisable(true);
                btnEdit.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.SHIELD, "18"));
                btnEdit.setTooltip(new Tooltip("Este rol es del sistema y no puede ser modificado por administradores."));
            }
        }

        Button btnDelete = new Button();
        if (canManageRoles && (!isSuperAdminRole || currentIsSuperAdmin)) {
            btnDelete.getStyleClass().add("btn-action-delete");
            FontAwesomeIconView deleteIcon = new FontAwesomeIconView(FontAwesomeIcon.TRASH);
            deleteIcon.setSize("18");
            btnDelete.setGraphic(deleteIcon);
            btnDelete.setOnAction(e -> handleDeleteSingleRole(role));
        } else {
            btnDelete.getStyleClass().add("btn-action-lock");
            btnDelete.setStyle("-fx-background-color: #64748b; -fx-text-fill: white; -fx-background-radius: 50;");
            FontAwesomeIconView lockIcon = new FontAwesomeIconView(FontAwesomeIcon.LOCK);
            lockIcon.setSize("18");
            lockIcon.setFill(Color.WHITE);
            btnDelete.setGraphic(lockIcon);
            btnDelete.setTooltip(new Tooltip(container.getBundle().getString("user.manage.btn.unlock")));
            btnDelete.setOnAction(e -> AlertUtil.showWarning(container.getBundle().getString("alert.locked"),
                    container.getBundle().getString("role.msg.locked_delete")));
        }

        // Disable deletion for system roles or "admin" or "SUPERADMIN"
        if ("admin".equalsIgnoreCase(role.getName()) || "cajero".equalsIgnoreCase(role.getName()) || isSuperAdminRole) {
            btnDelete.setDisable(true);
            btnDelete.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.SHIELD, "18"));
            btnDelete.setTooltip(new Tooltip("Rol protegido del sistema. No se puede eliminar."));
        }


        actions.getChildren().addAll(btnEdit, btnDelete);
        card.getChildren().addAll(avatar, info, actions);
        return card;
    }

    private void filterRoles(String query) {
        if (query == null || query.isEmpty()) {
            renderRoleCards(roleList);
        } else {
            String q = query.toLowerCase();
            List<Role> filtered = roleList.stream().filter(r -> r.getName().toLowerCase().contains(q) ||
                    (r.getDescription() != null && r.getDescription().toLowerCase().contains(q)))
                    .collect(Collectors.toList());
            renderRoleCards(filtered);
        }
    }

    @FXML
    private void handleNewRole() {
        if (!container.getUserSession().hasPermission("rol.editar")) {
            AlertUtil.showError("Acceso Denegado", "No tiene permiso para gestionar roles.");
            return;
        }
        ModalService.showTransparentModal("/view/edit_role.fxml", "Nuevo Rol", container, null);
        loadRoles(); // Refresh after modal closes
    }

    private void handleEditSingleRole(Role role) {
        if (!container.getUserSession().hasPermission("rol.editar")) {
            AlertUtil.showError("Acceso Denegado", "No tiene permiso para gestionar roles.");
            return;
        }
        ModalService.showTransparentModal("/view/edit_role.fxml", "Editar Rol", container,
                (EditRoleController controller) -> {
                    controller.setRole(role);
                });
        loadRoles();
    }

    private void handleDeleteSingleRole(Role role) {
        if (!container.getUserSession().hasPermission("rol.editar")) {
            AlertUtil.showError("Acceso Denegado", "No tiene permiso para gestionar roles.");
            return;
        }
        if (AlertUtil.showConfirmation("Eliminar Rol", "\u00bfeliminar rol: " + role.getName() + "?",
                "Podr\u00eda afectar a usuarios que dependan de este rol.")) {
            try {
                if (roleUseCase.deleteRole(role.getRoleId())) {
                    loadRoles();
                } else {
                    AlertUtil.showError("Error", "No se pudo eliminar el rol");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                AlertUtil.showError("Error", "Error al eliminar rol: " + e.getMessage());
            }
        }
    }
}
