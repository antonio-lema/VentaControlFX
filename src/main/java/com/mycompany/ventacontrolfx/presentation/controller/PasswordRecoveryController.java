package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.User;
import com.mycompany.ventacontrolfx.application.usecase.UserUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.shared.async.AsyncManager;

import java.io.IOException;

public class PasswordRecoveryController implements Injectable {

    private ServiceContainer container;
    private UserUseCase userUseCase;
    private AsyncManager asyncManager;
    private String currentRecoveryEmail;

    @FXML
    private Label lblTitle;
    @FXML
    private Label lblStatus;
    @FXML
    private Label lblError;

    @FXML
    private VBox boxSelection;
    @FXML
    private VBox step1Box;
    @FXML
    private TextField txtUsername;
    @FXML
    private VBox step2Box;
    @FXML
    private TextField txtCode;
    @FXML
    private VBox step3Box;
    @FXML
    private PasswordField txtNewPass;
    @FXML
    private PasswordField txtConfirmPass;
    @FXML
    private VBox stepRecoverUserBox;
    @FXML
    private TextField txtRecoverEmail;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.userUseCase = container.getUserUseCase();
        this.asyncManager = container.getAsyncManager();
    }

    @FXML
    public void initialize() {
        lblError.setText("");
    }

    @FXML
    private void showPasswordRecovery() {
        boxSelection.setVisible(false);
        boxSelection.setManaged(false);
        step1Box.setVisible(true);
        step1Box.setManaged(true);
        lblTitle.setText("Recuperar Contraseña");
        lblStatus.setText("Paso 1: Identificación");
    }

    @FXML
    private void showUsernameRecovery() {
        boxSelection.setVisible(false);
        boxSelection.setManaged(false);
        stepRecoverUserBox.setVisible(true);
        stepRecoverUserBox.setManaged(true);
        lblTitle.setText("Recuperar Usuario");
        lblStatus.setText("Introduce tu correo asociado");
    }

    @FXML
    private void handleVerifyUserAndSendCode() {
        String username = txtUsername.getText().trim();
        if (username.isEmpty()) {
            lblError.setText("Introduce un usuario.");
            return;
        }

        asyncManager.runAsyncTask(() -> {
            try {
                // Find user by username to get email
                User user = userUseCase.listUsers().stream()
                        .filter(u -> u.getUsername().equalsIgnoreCase(username))
                        .findFirst().orElse(null);

                if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) {
                    throw new Exception("Usuario no encontrado o sin email.");
                }

                currentRecoveryEmail = user.getEmail();
                userUseCase.recoverPassword(currentRecoveryEmail);
                return null;
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }, result -> {
            step1Box.setVisible(false);
            step1Box.setManaged(false);
            step2Box.setVisible(true);
            step2Box.setManaged(true);
            lblStatus.setText("Paso 2: Verificación");
            lblError.setText("Código enviado.");
        }, error -> {
            lblError.setText(error.getMessage());
        });
    }

    @FXML
    private void handleVerifyCode() {
        String code = txtCode.getText().trim();
        if (userUseCase.verifyCode(currentRecoveryEmail, code)) {
            step2Box.setVisible(false);
            step2Box.setManaged(false);
            step3Box.setVisible(true);
            step3Box.setManaged(true);
            lblStatus.setText("Paso 3: Nueva Contraseña");
            lblError.setText("");
        } else {
            lblError.setText("Código incorrecto.");
        }
    }

    @FXML
    private void handleChangePassword() {
        String pass1 = txtNewPass.getText();
        String pass2 = txtConfirmPass.getText();

        if (pass1.length() < 4 || !pass1.equals(pass2)) {
            lblError.setText("Contraseñas no válidas o no coinciden.");
            return;
        }

        asyncManager.runAsyncTask(() -> {
            userUseCase.resetPassword(currentRecoveryEmail, pass1);
            return null;
        }, result -> {
            AlertUtil.showInfo("Éxito", "Contraseña cambiada.");
            handleBack();
        }, error -> lblError.setText(error.getMessage()));
    }

    @FXML
    private void handleRecoverUsername() {
        String email = txtRecoverEmail.getText().trim();
        asyncManager.runAsyncTask(() -> {
            userUseCase.sendUsernameReminder(email);
            return null;
        }, result -> {
            AlertUtil.showInfo("Enviado", "Usuario enviado al correo.");
            handleBack();
        }, error -> lblError.setText(error.getMessage()));
    }

    @FXML
    private void handleBack() {
        com.mycompany.ventacontrolfx.util.SceneNavigator.loadScene(
                (Stage) lblTitle.getScene().getWindow(),
                "/view/login.fxml",
                "Login",
                900, 600,
                false,
                container);
    }
}
