package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.User;
import com.mycompany.ventacontrolfx.service.UserService;
import com.mycompany.ventacontrolfx.service.EmailService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Random;

public class PasswordRecoveryController {

    private final UserService userService = new UserService();
    private final EmailService emailService = new EmailService();
    private User targetUser; // The user attempting to recover password
    private String generatedCode; // The code sent to email

    @FXML
    private Label lblTitle;
    @FXML
    private Label lblStatus;
    @FXML
    private Label lblError;

    // Selection Box
    @FXML
    private VBox boxSelection;

    // Flow 1: Password Recovery
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

    // Flow 2: Username Recovery
    @FXML
    private VBox stepRecoverUserBox;
    @FXML
    private TextField txtRecoverEmail;

    @FXML
    public void initialize() {
        lblError.setText("");
        // Listener para ENTER
        if (boxSelection != null) {
            boxSelection.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.setOnKeyPressed(event -> {
                        if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                            if (step1Box.isVisible()) {
                                handleVerifyUserAndSendCode();
                            } else if (step2Box.isVisible()) {
                                handleVerifyCode();
                            } else if (step3Box.isVisible()) {
                                handleChangePassword();
                            } else if (stepRecoverUserBox.isVisible()) {
                                handleRecoverUsername();
                            }
                        }
                    });
                }
            });
        }
    }

    // --- NAVIGATION METHODS ---

    @FXML
    private void showPasswordRecovery() {
        boxSelection.setVisible(false);
        boxSelection.setManaged(false);

        step1Box.setVisible(true);
        step1Box.setManaged(true);

        lblTitle.setText("Recuperar Contraseña");
        lblStatus.setText("Paso 1: Identificación");
        lblError.setText("");
    }

    @FXML
    private void showUsernameRecovery() {
        boxSelection.setVisible(false);
        boxSelection.setManaged(false);

        stepRecoverUserBox.setVisible(true);
        stepRecoverUserBox.setManaged(true);

        lblTitle.setText("Recuperar Usuario");
        lblStatus.setText("Introduce tu correo asociado");
        lblError.setText("");
    }

    // --- FLOW 1: PASSWORD RECOVERY LOGIC ---

    @FXML
    private void handleVerifyUserAndSendCode() {
        String username = txtUsername.getText();
        if (username.isEmpty()) {
            lblError.setText("Por favor, introduce un nombre de usuario.");
            return;
        }

        try {
            targetUser = userService.findByUsername(username);

            if (targetUser != null && targetUser.getEmail() != null && !targetUser.getEmail().isEmpty()) {
                // Generate Code
                generatedCode = String.format("%06d", new Random().nextInt(999999));

                // Send Email
                emailService.sendRecoveryCode(targetUser.getEmail(), generatedCode);

                // Move to Step 2
                step1Box.setVisible(false);
                step1Box.setManaged(false);
                step2Box.setVisible(true);
                step2Box.setManaged(true);

                lblStatus.setText("Paso 2: Verificación de Código");
                lblError.setText("Hemos enviado un código a su correo.");
            } else {
                // Security: Generic message or specific? Let's use specific for now as internal
                // app
                if (targetUser == null) {
                    lblError.setText("Usuario no encontrado.");
                } else {
                    lblError.setText("El usuario no tiene email configurado.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            lblError.setText("Error de base de datos.");
        }
    }

    @FXML
    private void handleVerifyCode() {
        String inputCode = txtCode.getText();
        if (inputCode.equals(generatedCode)) {
            // Code Matches
            step2Box.setVisible(false);
            step2Box.setManaged(false);
            step3Box.setVisible(true);
            step3Box.setManaged(true);

            lblStatus.setText("Paso 3: Nueva Contraseña");
            lblError.setText(""); // Clear success message from prev step
        } else {
            lblError.setText("Código incorrecto, verifique su correo.");
        }
    }

    @FXML
    private void handleChangePassword() {
        String pass1 = txtNewPass.getText();
        String pass2 = txtConfirmPass.getText();

        if (pass1.isEmpty() || pass2.isEmpty()) {
            lblError.setText("Los campos no pueden estar vacíos.");
            return;
        }

        if (!pass1.equals(pass2)) {
            lblError.setText("Las contraseñas no coinciden.");
            return;
        }

        if (pass1.length() < 4) {
            lblError.setText("La contraseña es muy corta.");
            return;
        }

        try {
            boolean updated = userService.updatePassword(targetUser.getUserId(), pass1);
            if (updated) {
                // Success! Redirect to login.
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.INFORMATION);
                alert.setTitle("Éxito");
                alert.setHeaderText(null);
                alert.setContentText("Contraseña actualizada con éxito. Por favor, inicia sesión.");
                alert.showAndWait();

                handleBack();
            } else {
                lblError.setText("No se pudo actualizar la contraseña.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            lblError.setText("Error al actualizar la contraseña.");
        }
    }

    // --- FLOW 2: USERNAME RECOVERY LOGIC ---

    @FXML
    private void handleRecoverUsername() {
        String email = txtRecoverEmail.getText();

        if (email.isEmpty()) {
            lblError.setText("Introduce un correo electrónico.");
            return;
        }

        try {
            User user = userService.findByEmail(email);

            if (user != null) {
                emailService.sendUsernameReminder(email, user.getUsername());

                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                        javafx.scene.control.Alert.AlertType.INFORMATION);
                alert.setTitle("Usuario Enviado");
                alert.setHeaderText(null);
                alert.setContentText("Se ha enviado el nombre de usuario a su correo.");
                alert.showAndWait();

                handleBack();
            } else {
                lblError.setText("No existe ninguna cuenta asociada a este correo.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            lblError.setText("Error de conexión.");
        }
    }

    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) lblStatus.getScene().getWindow();
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/view/style.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle("Login - TPV Bazar Electrónico");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
