package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.User;
import com.mycompany.ventacontrolfx.application.usecase.UserUseCase;
import com.mycompany.ventacontrolfx.application.usecase.LoginUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.SceneNavigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;

import com.mycompany.ventacontrolfx.domain.exception.UserNotFoundException;
import com.mycompany.ventacontrolfx.domain.exception.InvalidPasswordException;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class LoginController implements Injectable {

    @FXML
    private TextField txtUsername;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private Button btnLogin;
    @FXML
    private Label lblMessage;
    @FXML
    private VBox loginCard;

    private ServiceContainer container;
    private UserUseCase userUseCase;
    private LoginUseCase loginUseCase;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.userUseCase = container.getUserUseCase();
        this.loginUseCase = container.getLoginUseCase();
        checkIfUsersExist();
    }

    private void checkIfUsersExist() {
        try {
            if (userUseCase.getUserCount() == 0) {
                showErrorMessage(container.getBundle().getString("login.error.no_users"));
                Platform.runLater(() -> SceneNavigator.loadScene(
                        (Stage) btnLogin.getScene().getWindow(),
                        "/view/register_user.fxml",
                        container.getBundle().getString("login.dialog.first_admin"),
                        900, 600,
                        false,
                        container));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showErrorMessage(container.getBundle().getString("login.error.incomplete"));
            return;
        }

        btnLogin.setDisable(true);
        
        ResourceBundle bundle = container.getBundle();
        String verifyingMsg = bundle.containsKey("login.status.verifying") ? 
                             bundle.getString("login.status.verifying") : "Verificando...";
        lblMessage.setText(verifyingMsg);

        container.getAsyncManager().runAsyncTask(() -> {
            try {
                // El proceso de BCrypt y carga de permisos puede tardar >500ms
                return loginUseCase.execute(username, password);
            } catch (Exception ex) {
                return ex; // Retornamos la excepción para manejarla en el callback
            }
        }, result -> {
            btnLogin.setDisable(false);
            
            if (result instanceof User) {
                User user = (User) result;
                container.getUserSession().setCurrentUser(user);
                lblMessage.setText(container.getBundle().getString("login.success"));
                
                SceneNavigator.loadScene(
                        (Stage) btnLogin.getScene().getWindow(),
                        "/view/main.fxml",
                        "TPV",
                        1200, 800,
                        true,
                        container);
            } else if (result instanceof UserNotFoundException) {
                showErrorMessage(container.getBundle().getString("login.error.user_not_found"));
            } else if (result instanceof InvalidPasswordException) {
                showErrorMessage(container.getBundle().getString("login.error.wrong_password"));
            } else if (result instanceof SQLException) {
                ((SQLException) result).printStackTrace();
                showErrorMessage(container.getBundle().getString("login.error.database"));
            } else if (result instanceof Exception) {
                ((Exception) result).printStackTrace();
                showErrorMessage(container.getBundle().getString("login.error.unexpected") + ": " + ((Exception) result).getMessage());
            }
        }, null);
    }

    private void showErrorMessage(String message) {
        lblMessage.setText(message);
        lblMessage.setVisible(true);
        lblMessage.setManaged(true);

        // Shake animation
        if (loginCard != null) {
            TranslateTransition tt = new TranslateTransition(Duration.millis(50), loginCard);
            tt.setFromX(0);
            tt.setByX(10);
            tt.setCycleCount(6);
            tt.setAutoReverse(true);
            tt.play();
        }
    }

    @FXML
    private void handleForgotPassword() {
        SceneNavigator.loadScene(
                (Stage) btnLogin.getScene().getWindow(),
                "/view/password_recovery.fxml",
                container.getBundle().getString("login.dialog.recovery"),
                900, 600,
                false,
                container);
    }
}
