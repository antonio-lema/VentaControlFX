package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.User;
import com.mycompany.ventacontrolfx.service.UserService;
import com.mycompany.ventacontrolfx.util.UserSession;
import java.sql.SQLException;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController implements com.mycompany.ventacontrolfx.util.Injectable {
    private com.mycompany.ventacontrolfx.service.ServiceContainer container;
    private UserService userService;

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Button btnLogin;

    @FXML
    private Label lblMessage;

    @Override
    public void inject(com.mycompany.ventacontrolfx.service.ServiceContainer container) {
        this.container = container;
        this.userService = container.getUserService();
        checkIfUsersExist();
    }

    @FXML
    public void initialize() {
        // Initialization handled in inject()
    }

    private void checkIfUsersExist() {
        try {
            if (userService.getCount() == 0) {
                lblMessage.setText("No hay usuarios registrados. Redirigiendo a registro de Administrador...");

                javafx.application.Platform.runLater(() -> {
                    com.mycompany.ventacontrolfx.util.SceneNavigator.loadScene(
                            (javafx.stage.Stage) btnLogin.getScene().getWindow(),
                            "/view/register_user.fxml",
                            "Registro Primer Administrador",
                            900, 600,
                            false,
                            container);
                });
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
            User user = userService.findByUsername(username);

            if (user != null && user.getPassword().equals(password)) {
                container.getUserSession().setCurrentUser(user);
                lblMessage.setText("Login correcto 👍");

                // Switch to Main View using SceneNavigator
                com.mycompany.ventacontrolfx.util.SceneNavigator.loadScene(
                        (javafx.stage.Stage) btnLogin.getScene().getWindow(),
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
        try {
            java.net.URL fxmlUrl = getClass().getResource("/view/password_recovery.fxml");

            if (fxmlUrl == null) {
                lblMessage.setText("Error: No se encuentra el archivo FXML.");
                return;
            }

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(fxmlUrl);
            javafx.scene.Parent root = loader.load();

            javafx.stage.Stage stage = (javafx.stage.Stage) btnLogin.getScene().getWindow();
            javafx.scene.Scene scene = new javafx.scene.Scene(root, 900, 600);

            java.net.URL cssUrl = getClass().getResource("/view/style.css");
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }

            stage.setScene(scene);
            stage.setTitle("Recuperar Contraseña");
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            lblMessage.setText("Error: " + e.getMessage());
        }
    }
}
