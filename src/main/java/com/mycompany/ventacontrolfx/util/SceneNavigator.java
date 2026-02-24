package com.mycompany.ventacontrolfx.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * Utility class for navigating between scenes in the application.
 * Centralizes FXML loading and scene setup.
 */
public class SceneNavigator {

    private static final String STYLE_SHEET = "/view/style.css";

    /**
     * Loads a new scene into the provided stage.
     * 
     * @param stage    The stage where the scene will be set.
     * @param fxmlPath The path to the FXML file (e.g., "/view/login.fxml").
     * @param title    The title for the window.
     * @param width    The width of the scene.
     * @param height   The height of the scene.
     */
    public static void loadScene(Stage stage, String fxmlPath, String title, double width, double height) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root, width, height);

            // Apply global CSS
            String css = SceneNavigator.class.getResource(STYLE_SHEET).toExternalForm();
            scene.getStylesheets().add(css);

            stage.setTitle(title);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            System.err.println("Error loading scene: " + fxmlPath);
            e.printStackTrace();
        }
    }
}
