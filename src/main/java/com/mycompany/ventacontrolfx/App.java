package com.mycompany.ventacontrolfx;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Force application timezone
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Europe/Madrid"));
        
        // 1. Show Splash Screen Immediately
        FXMLLoader splashLoader = new FXMLLoader(getClass().getResource("/view/auth/splash.fxml"));
        Parent splashRoot = splashLoader.load();
        Scene splashScene = new Scene(splashRoot);
        splashScene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        
        primaryStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        primaryStage.setScene(splashScene);
        primaryStage.setTitle("VentaControlFX");
        primaryStage.show();
        primaryStage.centerOnScreen();

        // 2. Load Container in Background
        new Thread(() -> {
            long start = System.currentTimeMillis();
            try {
                // Initialize Database Connection (Pre-heat)
                try (java.sql.Connection conn = com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection.getConnection()) {
                    System.out.println("[App] Database connection established.");
                } catch (Exception e) {
                    System.err.println("[App] Database connection failed: " + e.getMessage());
                }

                // Create Container (Heavy task)
                com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer container = 
                        new com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer();
                
                System.out.println("[App] Container ready in " + (System.currentTimeMillis() - start) + "ms");

                // Switch to Login UI
                javafx.application.Platform.runLater(() -> {
                    try {
                        // Create a NEW stage for Login to change StageStyle back to DECORATED
                        Stage mainStage = new Stage();
                        
                        com.mycompany.ventacontrolfx.presentation.navigation.SceneNavigator.loadScene(
                                mainStage,
                                "/view/auth/login.fxml",
                                "Login",
                                900,
                                600,
                                false,
                                container);
                        
                        // Close splash
                        primaryStage.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}



