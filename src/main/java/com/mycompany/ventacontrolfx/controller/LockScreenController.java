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

public class LockScreenController implements com.mycompany.ventacontrolfx.util.Injectable {

    @FXML
    private Label lblUsername;

    @FXML
    private PasswordField passwordField;

    private User currentUser;
    private UserService userService;
    private Runnable onUnlockCallback;
    private com.mycompany.ventacontrolfx.service.ServiceContainer container;

    @Override
    public void inject(com.mycompany.ventacontrolfx.service.ServiceContainer container) {
        this.container = container;
        this.userService = container.getUserService();
        this.currentUser = container.getUserSession().getCurrentUser();

        if (currentUser != null) {
            lblUsername.setText(currentUser.getUsername());
        }
    }

    public void initialize() {
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
                // Close the lock screen safely by removing fullscreen first
                Stage stage = (Stage) passwordField.getScene().getWindow();
                stage.setFullScreen(false);
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
        if (container != null && container.getUserSession() != null) {
            container.getUserSession().logout();
        }

        Stage currentStage = (Stage) passwordField.getScene().getWindow();
        Stage parentStage = (Stage) currentStage.getOwner();

        // Open new Login screen FIRST so application doesn't implicitly exit
        Stage loginStage = new Stage();
        com.mycompany.ventacontrolfx.util.SceneNavigator.loadScene(
                loginStage,
                "/view/login.fxml",
                "Login",
                900, 600,
                false,
                container);

        // Safely close the lock screen modal
        currentStage.setFullScreen(false);
        currentStage.close();

        // Close the main app window
        if (parentStage != null) {
            parentStage.close();
        }
    }

    private void showAlert(String title, String content) {
        AlertUtil.showError(title, content);
    }
}
