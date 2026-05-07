package com.mycompany.ventacontrolfx.presentation.controller.auth;

import com.mycompany.ventacontrolfx.domain.model.User;
import com.mycompany.ventacontrolfx.presentation.controller.user.UserPermissionManager;
import com.mycompany.ventacontrolfx.application.usecase.UserUseCase;
import com.mycompany.ventacontrolfx.application.usecase.PermissionUseCase;
import com.mycompany.ventacontrolfx.application.usecase.RoleUseCase;
import com.mycompany.ventacontrolfx.domain.model.Role;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import com.mycompany.ventacontrolfx.presentation.navigation.SceneNavigator;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.List;

public class RegisterUserController implements Injectable {

    @FXML private TextField txtFullName, txtUsername, txtEmail;
    @FXML private ComboBox<String> cmbRole;
    @FXML private PasswordField txtPassword, txtConfirmPassword;
    @FXML private Label lblError, lblTitle;
    @FXML private StackPane rootStackPane;
    @FXML private VBox permissionsContainer;

    private ServiceContainer container;
    private UserUseCase userUseCase;
    private PermissionUseCase permissionUseCase;
    private RoleUseCase roleUseCase;
    private UserPermissionManager permissionManager;
    private UserFormManager formManager;
    private User userToEdit;
    private boolean isInitialSetup = false;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.userUseCase = container.getUserUseCase();
        this.permissionUseCase = container.getPermissionUseCase();
        this.roleUseCase = container.getRoleUseCase();
        
        this.permissionManager = new UserPermissionManager(container, permissionsContainer);
        this.formManager = new UserFormManager(container, lblError);

        loadRoles();
        permissionManager.setup(cmbRole.getValue());

        cmbRole.valueProperty().addListener((obs, old, nv) -> {
            if (nv != null && "SUPERADMIN".equalsIgnoreCase(nv) && !container.getAuthService().isSuperAdmin()) {
                cmbRole.setValue("admin"); return;
            }
            permissionManager.updateByRole(nv);
        });

        checkSetupMode();
    }

    private void loadRoles() {
        try {
            boolean isSuperAdmin = container.getAuthService().isSuperAdmin();
            List<String> roleNames = roleUseCase.getAllRoles().stream()
                .filter(r -> isSuperAdmin || !"SUPERADMIN".equalsIgnoreCase(r.getName()))
                .map(Role::getName).toList();
            cmbRole.setItems(FXCollections.observableArrayList(roleNames));
        } catch (Exception e) { cmbRole.setItems(FXCollections.observableArrayList("admin", "cajero")); }
    }

    private void checkSetupMode() {
        try {
            if (userUseCase.getUserCount() == 0) {
                isInitialSetup = true;
                lblTitle.setText(container.getBundle().getString("user.register.setup.title"));
                cmbRole.setValue("admin"); cmbRole.setDisable(true);
                permissionManager.setAllSelected(true);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    public void setUser(User user) {
        this.userToEdit = user;
        if (user == null) return;

        lblTitle.setText(container.getBundle().getString("user.register.edit.title"));
        txtFullName.setText(user.getFullName());
        txtUsername.setText(user.getUsername()); txtUsername.setDisable(true);
        txtEmail.setText(user.getEmail());
        cmbRole.setValue(user.getRole());

        formManager.applySecurityConstraints(user, txtFullName, txtEmail, cmbRole, permissionsContainer);
        permissionManager.loadUserPermissions(user.getUserId(), user.getRole(), user.isHasCustomPermissions());
    }

    @FXML
    private void handleRegister() {
        if (!formManager.validate(txtFullName.getText(), txtUsername.getText(), cmbRole.getValue(), 
                                 txtPassword.getText(), txtConfirmPassword.getText(), userToEdit != null)) return;

        try {
            if (userToEdit == null) {
                if (userUseCase.getUserByUsername(txtUsername.getText()) != null) {
                    AlertUtil.showError(container.getBundle().getString("alert.error"), container.getBundle().getString("user.register.error.username_exists"));
                    return;
                }
                registerNewUser();
            } else {
                updateExistingUser();
            }
        } catch (Exception e) { AlertUtil.showError("Error", e.getMessage()); }
    }

    private void registerNewUser() throws Exception {
        User newUser = new User();
        newUser.setFullName(txtFullName.getText());
        newUser.setUsername(txtUsername.getText());
        newUser.setEmail(txtEmail.getText());
        newUser.setRole(cmbRole.getValue());
        newUser.setPasswordHash(txtPassword.getText());
        newUser.setHasCustomPermissions(permissionManager.isCustomizationEnabled());

        userUseCase.registerUser(newUser);
        User created = userUseCase.getUserByUsername(newUser.getUsername());
        if (created != null) permissionUseCase.savePermissionsForUser(created.getUserId(), permissionManager.getSelectedPermissionCodes());

        AlertUtil.showInfo(container.getBundle().getString("user.register.success.title"), container.getBundle().getString("user.register.success.created"));
        
        if (isInitialSetup) SceneNavigator.loadScene((Stage) rootStackPane.getScene().getWindow(), "/view/auth/login.fxml", "Login", 900, 600, false, container);
        else closeWindow();
    }

    private void updateExistingUser() throws Exception {
        userToEdit.setFullName(txtFullName.getText());
        userToEdit.setEmail(txtEmail.getText());
        userToEdit.setRole(cmbRole.getValue());
        userToEdit.setHasCustomPermissions(permissionManager.isCustomizationEnabled());

        userUseCase.updateUser(userToEdit);
        permissionUseCase.savePermissionsForUser(userToEdit.getUserId(), permissionManager.getSelectedPermissionCodes());
        
        if (!txtPassword.getText().isEmpty()) userUseCase.resetPassword(userToEdit.getEmail(), txtPassword.getText());

        AlertUtil.showInfo(container.getBundle().getString("user.register.success.title"), container.getBundle().getString("user.register.success.updated"));
        closeWindow();
    }

    @FXML private void handleCancel() { closeWindow(); }
    private void closeWindow() { if (rootStackPane != null && rootStackPane.getScene() != null) ((Stage) rootStackPane.getScene().getWindow()).close(); }

    private double xOffset = 0, yOffset = 0;
    @FXML private void handleMousePressed(javafx.scene.input.MouseEvent event) { xOffset = event.getSceneX(); yOffset = event.getSceneY(); }
    @FXML private void handleMouseDragged(javafx.scene.input.MouseEvent event) {
        Stage stage = (Stage) rootStackPane.getScene().getWindow();
        stage.setX(event.getScreenX() - xOffset); stage.setY(event.getScreenY() - yOffset);
    }
}



