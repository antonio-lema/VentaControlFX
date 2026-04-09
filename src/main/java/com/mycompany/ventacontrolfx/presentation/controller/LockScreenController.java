package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.User;
import com.mycompany.ventacontrolfx.application.usecase.UserUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.UserSession;

import java.sql.SQLException;

public class LockScreenController implements Injectable {

    @FXML
    private Label lblUsername;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label lblError;

    private User currentUser;
    private UserUseCase userUseCase;
    private UserSession userSession;
    private Runnable onUnlockCallback;
    private ServiceContainer container;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.userUseCase = container.getUserUseCase();
        this.userSession = container.getUserSession();
        this.currentUser = userSession.getCurrentUser();

        if (currentUser != null) {
            lblUsername.setText(currentUser.getUsername());
        }
    }

    public void initialize() {
        Platform.runLater(() -> passwordField.requestFocus());
        if (lblError != null) {
            lblError.setVisible(false);
            lblError.setManaged(false);
        }
    }

    public void setOnUnlock(Runnable onUnlock) {
        this.onUnlockCallback = onUnlock;
    }

    @FXML
    private void handleUnlock() {
        String password = passwordField.getText();
        if (password.isEmpty()) {
            showInPlaceError("Por favor, introduce tu contrase\u00c3\u00b1a.");
            return;
        }

        try {
            User user = userUseCase.login(currentUser.getUsername(), password);
            if (user != null) {
                if (onUnlockCallback != null) {
                    onUnlockCallback.run();
                }
                Stage stage = (Stage) passwordField.getScene().getWindow();
                stage.setFullScreen(false);
                stage.close();
            } else {
                showInPlaceError("Contrase\u00c3\u00b1a incorrecta.");
                passwordField.clear();
            }
        } catch (SQLException e) {
            showInPlaceError("Error de base de datos.");
        }
    }

    private void showInPlaceError(String message) {
        if (lblError != null) {
            lblError.setText(message);
            lblError.setVisible(true);
            lblError.setManaged(true);
        } else {
            AlertUtil.showError("Error", message);
        }
    }

    @FXML
    private void handleChangeUser() {
        if (userSession != null)
            userSession.logout();
        Stage currentStage = (Stage) passwordField.getScene().getWindow();
        currentStage.setFullScreen(false);

        Platform.runLater(() -> {
            Stage mainAppStage = null;
            for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
                if (window instanceof Stage && window != currentStage) {
                    mainAppStage = (Stage) window;
                    break;
                }
            }
            if (mainAppStage != null) {
                com.mycompany.ventacontrolfx.util.SceneNavigator.loadScene(
                        mainAppStage, "/view/login.fxml", "Login", 900, 600, false, container);
            }
            currentStage.close();
        });
    }
}
