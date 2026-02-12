package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.User;
import com.mycompany.ventacontrolfx.service.UserService;
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
                lblMessage.setText("Login correcto 👍");
                // Aquí podrías abrir la siguiente pantalla (main.fxml)
                // por ejemplo: SceneNavigator.changeScene("main.fxml", ...)
            } else {
                lblMessage.setText("Usuario o contraseña incorrectos ❌");
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            lblMessage.setText("Error de base de datos 💥");
        }
    }
}
