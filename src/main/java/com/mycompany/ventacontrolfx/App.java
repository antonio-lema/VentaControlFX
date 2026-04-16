package com.mycompany.ventacontrolfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // 1. Create Global Service Container (Fast)
        com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer container = new com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer();

        // 2. Start heavy tasks in a background thread to unblock the UI
        new Thread(() -> {
            // Force load FontAwesome font
            try {
                javafx.scene.text.Font.loadFont(
                        de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView.class
                                .getResource(de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView.TTF_PATH)
                                .toExternalForm(),
                        10.0);
            } catch (Exception e) {
                System.err.println("Could not load FontAwesome font: " + e.getMessage());
            }

            // Initialize Database (Heavy task)
            try (java.sql.Connection conn = com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection
                    .getConnection()) {
                com.mycompany.ventacontrolfx.infrastructure.persistence.DatabaseInitializer.initialize(conn);
                System.out.println("Database initialization completed in background.");
            } catch (Exception e) {
                System.err.println("Error initializing database: " + e.getMessage());
            }
        }).start();

        // 3. IMMEDIATELY show the Login screen
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
