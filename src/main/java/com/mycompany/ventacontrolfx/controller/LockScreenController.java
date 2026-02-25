package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.User;
import com.mycompany.ventacontrolfx.util.UserSession;
import com.mycompany.ventacontrolfx.service.UserService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.mycompany.ventacontrolfx.util.AlertUtil;

import java.io.IOException;
import java.sql.SQLException;

public class LockScreenController {

    @FXML
    private Label lblUsername;

    @FXML
    private PasswordField passwordField;

    private User currentUser;
    private UserService userService;
    private Runnable onUnlockCallback;

    public void initialize() {
        userService = new UserService();
        currentUser = UserSession.getInstance().getCurrentUser();
        if (currentUser != null) {
            lblUsername.setText(currentUser.getUsername());
        }

        // Focus password field by default
        Platform.runLater(() -> passwordField.requestFocus());
    }

    public void setOnUnlock(Runnable onUnlock) {
        this.onUnlockCallback = onUnlock;
    }

    @FXML
    private void handleUnlock() {
        String password = passwordField.getText();

        if (password.isEmpty()) {
            showAlert("Error", "Por favor, introduce tu contraseña.");
            return;
        }

        try {
            if (userService.validateLogin(currentUser.getUsername(), password)) {
                // Correct password
                if (onUnlockCallback != null) {
                    onUnlockCallback.run();
                }
                // Close the lock screen
                Stage stage = (Stage) passwordField.getScene().getWindow();
                stage.close();
            } else {
                showAlert("Error", "Contraseña incorrecta.");
                passwordField.clear();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Error al verificar la contraseña: " + e.getMessage());
        }
    }

    @FXML
    private void handleChangeUser() {
        try {
            // Logout logic
            UserSession.getInstance().logout();

            // Load Login Screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) passwordField.getScene().getWindow(); // Get current stage (Lock Screen modal)

            // We need to get the PRIMARY stage to set the scene there, OR create a new one
            // and verify what to close.
            // Since Lock Screen is likely a modal on top of Main Window, we might want to
            // close Main Window too or redirect it.
            // Best approach: Close Lock Screen, Close Main Window (if possible to
            // reference), Open Login Window.

            // Getting the owner of the modal (Main Window)
            Stage mainStage = (Stage) stage.getOwner();
            if (mainStage != null) {
                mainStage.close();
            } else {
                // Fallback if no owner (shouldn't happen if opened as modal)
                // Try to find the main stage via other means or just open login.
            }

            stage.close(); // Close lock screen

            // Open Login Stage
            Stage loginStage = new Stage();
            Scene scene = new Scene(root, 900, 600);
            scene.getStylesheets().add(getClass().getResource("/view/style.css").toExternalForm());
            loginStage.setScene(scene);
            loginStage.setTitle("Login");
            loginStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        AlertUtil.showError(title, content);
    }
}
