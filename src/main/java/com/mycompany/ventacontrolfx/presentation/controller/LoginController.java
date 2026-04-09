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

import java.sql.SQLException;

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

        try {
            // Delegamos toda la lÃ³gica al Caso de Uso (Clean Architecture)
            User user = loginUseCase.execute(username, password);

            // Si llegamos aquÃ­, el login fue exitoso
            container.getUserSession().setCurrentUser(user);

            // Iniciar turno automÃ¡ticamente al entrar
            try {
                container.getWorkSessionUseCase().startShift(user.getUserId());
            } catch (Exception e) {
                // Si ya tenÃ­a un turno abierto (ej: cierre inesperado), no hacemos nada
                System.out.println("El usuario ya tiene un turno activo o hubo un error al iniciar: " + e.getMessage());
            }

            lblMessage.setText(container.getBundle().getString("login.success"));
            SceneNavigator.loadScene(
                    (Stage) btnLogin.getScene().getWindow(),
                    "/view/main.fxml",
                    "TPV",
                    1200, 800,
                    true,
                    container);

        } catch (com.mycompany.ventacontrolfx.domain.exception.UserNotFoundException ex) {
            showErrorMessage(container.getBundle().getString("login.error.user_not_found"));
        } catch (com.mycompany.ventacontrolfx.domain.exception.InvalidPasswordException ex) {
            showErrorMessage(container.getBundle().getString("login.error.wrong_password"));
        } catch (SQLException ex) {
            ex.printStackTrace();
            showErrorMessage(container.getBundle().getString("login.error.database"));
        } catch (Exception ex) {
            ex.printStackTrace();
            showErrorMessage(container.getBundle().getString("login.error.unexpected") + ": " + ex.getMessage());
        }
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
