package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.User;
import com.mycompany.ventacontrolfx.application.usecase.UserUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.ModalService;
import com.mycompany.ventacontrolfx.util.SceneNavigator;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.sql.SQLException;

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
    private Button btnRegister;
    @FXML
    private Label lblTitle;
    @FXML
    private Label lblSelectedCompany;
    @FXML
    private Button btnSelectCompany;
    @FXML
    private StackPane rootStackPane;

    private UserUseCase userUseCase;
    private ServiceContainer container;
    private User userToEdit;
    private int selectedCompanyId = 0;
    private boolean isInitialSetup = false;
    private double xOffset = 0;
    private double yOffset = 0;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.userUseCase = container.getUserUseCase();

        if (cmbRole.getItems().isEmpty()) {
            cmbRole.setItems(FXCollections.observableArrayList("admin", "cajero"));
        }

        checkSetupMode();
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

            if (user.getCompanyName() != null && !user.getCompanyName().isEmpty()) {
                lblSelectedCompany.setText(user.getCompanyName());
                lblSelectedCompany.setStyle("-fx-text-fill: -text-main;");
                selectedCompanyId = user.getCompanyId();
            }

            if (lblTitle != null)
                lblTitle.setText("Editar Usuario");
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

        try {
            if (userToEdit == null) {
                User newUser = new User();
                newUser.setFullName(fullName);
                newUser.setUsername(username);
                newUser.setEmail(email);
                newUser.setRole(role);
                newUser.setPassword(pass1);
                newUser.setCompanyId(selectedCompanyId);

                User existing = userUseCase.getUserByUsername(username);
                if (existing != null) {
                    showError("El nombre de usuario ya está en uso.");
                    return;
                }

                userUseCase.registerUser(newUser);
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
                userToEdit.setCompanyId(selectedCompanyId);

                userUseCase.updateUser(userToEdit);
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

    @FXML
    private void handleSelectCompany() {
        ModalService.showStandardModal("/view/clients.fxml", "Seleccionar Empresa", container,
                (ClientsController ctrl) -> {
                    ctrl.setOnClientSelected(client -> {
                        this.selectedCompanyId = client.getId();
                        lblSelectedCompany.setText(client.getName());
                        lblSelectedCompany.setStyle("-fx-text-fill: -text-main;");
                    });
                });
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
