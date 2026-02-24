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

public class LoginController {

    private final UserService userService = new UserService();

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Button btnLogin;

    @FXML
    private Label lblMessage;

    @FXML
    public void initialize() {
        // Cualquier inicialización que necesites al mostrar la pantalla
    }

    @FXML
    private void handleLogin() {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        try {
            User user = userService.findByUsername(username);

            if (user != null && user.getPassword().equals(password)) {
                UserSession.getInstance().setCurrentUser(user);
                lblMessage.setText("Login correcto 👍");

                // Switch to Main View
                try {
                    javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                            getClass().getResource("/view/main.fxml"));
                    javafx.scene.Parent root = loader.load();

                    javafx.stage.Stage stage = (javafx.stage.Stage) btnLogin.getScene().getWindow();
                    javafx.scene.Scene scene = new javafx.scene.Scene(root);
                    scene.getStylesheets().add(getClass().getResource("/view/style.css").toExternalForm());

                    stage.setScene(scene);
                    stage.setTitle("TPV Bazar Electrónico");
                    stage.centerOnScreen();
                    stage.setMaximized(true); // Open main window maximized
                    stage.show();

                } catch (java.io.IOException e) {
                    e.printStackTrace();
                    lblMessage.setText("Error al cargar la aplicación principal: " + e.getMessage());
                }

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
            stage.setTitle("Recuperar Contraseña - TPV Bazar Electrónico");
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            lblMessage.setText("Error: " + e.getMessage());
        }
    }
}
