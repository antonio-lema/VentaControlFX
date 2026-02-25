package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.User;
import com.mycompany.ventacontrolfx.service.UserService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import javafx.stage.Stage;

import java.sql.SQLException;

public class RegisterUserController {

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
    private Button btnRegister;

    @FXML
    private Label lblTitle;

    private UserService userService;

    @FXML
    private javafx.scene.layout.StackPane rootStackPane;

    private double xOffset = 0;
    private double yOffset = 0;

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

    @FXML
    public void initialize() {
        userService = new UserService();
        // Initialize roles if not done in FXML (though FXML covers it)
        if (cmbRole.getItems().isEmpty()) {
            cmbRole.setItems(FXCollections.observableArrayList("admin", "cajero"));
        }

        checkSetupMode();
    }

    private boolean isInitialSetup = false;

    private void checkSetupMode() {
        try {
            if (userService.getCount() == 0) {
                isInitialSetup = true;
                if (lblTitle != null)
                    lblTitle.setText("Configuración: Crear Administrador");
                if (btnCancel != null) {
                    btnCancel.setVisible(false);
                    btnCancel.setManaged(false);
                }
                cmbRole.setValue("admin");
                cmbRole.setDisable(true);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private User userToEdit;

    public void setUser(User user) {
        this.userToEdit = user;
        if (user != null) {
            // Edit Mode
            txtFullName.setText(user.getFullName());
            txtUsername.setText(user.getUsername());
            txtUsername.setDisable(true); // Don't allow changing username
            txtEmail.setText(user.getEmail());

            // Map DB role to UI role
            String uiRole = "cajero";
            if ("admin".equalsIgnoreCase(user.getRole())) {
                uiRole = "admin";
            }
            cmbRole.setValue(uiRole);

            // Update UI Interface
            // We need to access the button to change text, but it's not bound.
            // Let's rely on the title for context.
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

        // Basic Validation
        if (fullName.isEmpty() || username.isEmpty() || role == null) {
            showError("Todos los campos obligatorios deben llenarse.");
            return;
        }

        // Additional validation for Create Mode
        if (userToEdit == null) {
            if (pass1.isEmpty() || pass2.isEmpty()) {
                showError("La contraseña es obligatoria para nuevos usuarios.");
                return;
            }
        }

        // Password Validation (if provided)
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

        // Map UI role to Database role
        String dbRole = "user";
        if ("admin".equalsIgnoreCase(role)) {
            dbRole = "admin";
        }

        try {
            if (userToEdit == null) {
                // CREATE MODE
                User newUser = new User();
                newUser.setFullName(fullName);
                newUser.setUsername(username);
                newUser.setEmail(email);
                newUser.setRole(dbRole);
                newUser.setPassword(pass1);

                // Check existence
                User existing = userService.findByUsername(username);
                if (existing != null && existing.getUsername() != null) {
                    showError("El nombre de usuario ya está en uso.");
                    return;
                }

                if (userService.createUser(newUser)) {
                    showSuccess("Usuario creado correctamente.");

                    if (isInitialSetup) {
                        // Redirect back to login if it was initial setup
                        com.mycompany.ventacontrolfx.util.SceneNavigator.loadScene(
                                (javafx.stage.Stage) rootStackPane.getScene().getWindow(),
                                "/view/login.fxml",
                                "Login - TPV",
                                900, 600);
                    } else {
                        closeWindow();
                    }
                } else {
                    showError("No se pudo crear el usuario.");
                }

            } else {
                // UPDATE MODE
                userToEdit.setFullName(fullName);
                userToEdit.setEmail(email);
                userToEdit.setRole(dbRole);
                // Username is not updated

                if (userService.updateUser(userToEdit)) {
                    // Update password if provided
                    if (!pass1.isEmpty()) {
                        userService.updatePassword(userToEdit.getUserId(), pass1);
                    }
                    showSuccess("Usuario actualizado correctamente.");
                    closeWindow();
                } else {
                    showError("No se pudo actualizar el usuario.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error de base de datos: " + e.getMessage());
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
        Stage stage = (Stage) rootStackPane.getScene().getWindow();
        stage.close();
    }
}
