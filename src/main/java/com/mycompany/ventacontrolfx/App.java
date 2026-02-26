package com.mycompany.ventacontrolfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Force load FontAwesome font to ensure icons render correctly
        try {
            javafx.scene.text.Font.loadFont(
                    de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView.class
                            .getResource(de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView.TTF_PATH).toExternalForm(),
                    10.0);
        } catch (Exception e) {
            System.err.println("Could not load FontAwesome font: " + e.getMessage());
        }

        // Initialize Database
        try (java.sql.Connection conn = com.mycompany.ventacontrolfx.dao.DBConnection.getConnection()) {
            com.mycompany.ventacontrolfx.dao.DatabaseInitializer.initialize(conn);
        } catch (Exception e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }

        // Create Global Service Container
        com.mycompany.ventacontrolfx.service.ServiceContainer container = new com.mycompany.ventacontrolfx.service.ServiceContainer();

        // Use SceneNavigator to load the Login screen
        com.mycompany.ventacontrolfx.util.SceneNavigator.loadScene(
                primaryStage,
                "/view/login.fxml",
                "Login",
                900,
                600,
                false,
                container);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
