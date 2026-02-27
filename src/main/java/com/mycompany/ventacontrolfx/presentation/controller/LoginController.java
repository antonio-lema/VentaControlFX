package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.User;
import com.mycompany.ventacontrolfx.application.usecase.UserUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.SceneNavigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

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

    private ServiceContainer container;
    private UserUseCase userUseCase;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.userUseCase = container.getUserUseCase();
        checkIfUsersExist();
    }

    private void checkIfUsersExist() {
        try {
            if (userUseCase.getUserCount() == 0) {
                lblMessage.setText("No hay usuarios registrados. Redirigiendo...");
                Platform.runLater(() -> SceneNavigator.loadScene(
                        (Stage) btnLogin.getScene().getWindow(),
                        "/view/register_user.fxml",
                        "Registro Primer Administrador",
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

        try {
            boolean valid = userUseCase.validateLogin(username, password);
            if (valid) {
                User user = userUseCase.getUserByUsername(username);
                container.getUserSession().setCurrentUser(user);
                lblMessage.setText("Login correcto 👍");
                SceneNavigator.loadScene(
                        (Stage) btnLogin.getScene().getWindow(),
                        "/view/main.fxml",
                        "TPV",
                        1200, 800,
                        true,
                        container);
            } else {
                lblMessage.setText("Usuario o contraseña incorrectos ❌");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            lblMessage.setText("Error de base de datos 💥");
        }
    }

    @FXML
    private void handleForgotPassword() {
        SceneNavigator.loadScene(
                (Stage) btnLogin.getScene().getWindow(),
                "/view/password_recovery.fxml",
                "Recuperar Contraseña",
                900, 600,
                false,
                container);
    }
}
