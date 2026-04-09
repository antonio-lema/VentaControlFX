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
        lblTitle.setText("Recuperar ContraseÃ±a");
        lblStatus.setText("Paso 1: IdentificaciÃ³n");
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
                // Fix V-02: Usar findEmailByUsername que NO requiere permiso admin
                // Fix V-04 (anti-enumeraciÃ³n): No distinguimos en el catch si el usuario existe
                // o no
                String email = userUseCase.findEmailByUsername(username);

                if (email == null || email.isEmpty()) {
                    // No revelamos si el usuario no existe; dejamos pasar igual
                    // para que el siguiente paso muestre el mismo mensaje
                    currentRecoveryEmail = "__invalid__@noreply.com"; // Correo fake, el envÃ­o fallarÃ¡ silenciosamente
                } else {
                    currentRecoveryEmail = email;
                    userUseCase.recoverPassword(currentRecoveryEmail);
                }
                return null;
            } catch (Exception e) {
                // No propagamos el error para evitar revelar informaciÃ³n
                currentRecoveryEmail = "__invalid__@noreply.com";
                return null;
            }
        }, result -> {
            step1Box.setVisible(false);
            step1Box.setManaged(false);
            step2Box.setVisible(true);
            step2Box.setManaged(true);
            lblStatus.setText("Paso 2: VerificaciÃ³n");
            // Fix V-04: Mensaje genÃ©rico independientemente de si el usuario existe
            lblError.setText("Si el usuario existe y tiene correo asociado, recibirÃ¡ un cÃ³digo en breve.");
        }, error -> {
            // Mensaje genÃ©rico para no revelar detalles internos
            lblError.setText("Si el usuario existe y tiene correo asociado, recibirÃ¡ un cÃ³digo en breve.");
        });
    }

    @FXML
    private void handleVerifyCode() {
        String code = txtCode.getText().trim();
        if (code.isEmpty()) {
            lblError.setText("Introduce el cÃ³digo.");
            return;
        }

        asyncManager.runAsyncTask(() -> {
            return userUseCase.verifyCode(currentRecoveryEmail, code);
        }, isCorrect -> {
            if (isCorrect) {
                step2Box.setVisible(false);
                step2Box.setManaged(false);
                step3Box.setVisible(true);
                step3Box.setManaged(true);
                lblStatus.setText("Paso 3: Nueva ContraseÃ±a");
                lblError.setText("");
            } else {
                // Fix V-01: Comprobar si estÃ¡ bloqueado por exceso de intentos
                try {
                    boolean blocked = userUseCase.isRecoveryBlocked(currentRecoveryEmail);
                    if (blocked) {
                        lblError.setText("CÃ³digo bloqueado por demasiados intentos. Solicita un nuevo cÃ³digo.");
                        // Volver al paso 1 para que solicite otro
                        step2Box.setVisible(false);
                        step2Box.setManaged(false);
                        step1Box.setVisible(true);
                        step1Box.setManaged(true);
                        lblStatus.setText("Paso 1: IdentificaciÃ³n");
                    } else {
                        lblError.setText("CÃ³digo incorrecto o expirado. Te quedan pocos intentos.");
                    }
                } catch (Exception ex) {
                    lblError.setText("CÃ³digo incorrecto o expirado.");
                }
            }
        }, error -> lblError.setText("Error: " + error.getMessage()));
    }

    @FXML
    private void handleChangePassword() {
        String pass1 = txtNewPass.getText();
        String pass2 = txtConfirmPass.getText();

        if (pass1.length() < 4 || !pass1.equals(pass2)) {
            lblError.setText("ContraseÃ±as no vÃ¡lidas o no coinciden.");
            return;
        }

        asyncManager.runAsyncTask(() -> {
            userUseCase.resetPassword(currentRecoveryEmail, pass1);
            return null;
        }, result -> {
            AlertUtil.showInfo("Ã‰xito", "ContraseÃ±a cambiada.");
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
