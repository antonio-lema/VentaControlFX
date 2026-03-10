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

        if (username.isEmpty() || password.isEmpty()) {
            lblMessage.setText("Por favor, rellene todos los campos ⚠️");
            return;
        }

        try {
            // Delegamos toda la lógica al Caso de Uso (Clean Architecture)
            User user = loginUseCase.execute(username, password);

            // Si llegamos aquí, el login fue exitoso
            container.getUserSession().setCurrentUser(user);

            lblMessage.setText("Login correcto 👍");
            SceneNavigator.loadScene(
                    (Stage) btnLogin.getScene().getWindow(),
                    "/view/main.fxml",
                    "TPV",
                    1200, 800,
                    true,
                    container);

        } catch (com.mycompany.ventacontrolfx.domain.exception.UserNotFoundException ex) {
            lblMessage.setText("El usuario no existe ❌");
        } catch (com.mycompany.ventacontrolfx.domain.exception.InvalidPasswordException ex) {
            lblMessage.setText("Contraseña incorrecta 🔑");
        } catch (SQLException ex) {
            ex.printStackTrace();
            lblMessage.setText("Error crítico de base de datos 💥");
        } catch (Exception ex) {
            ex.printStackTrace();
            lblMessage.setText("Error inesperado: " + ex.getMessage());
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
