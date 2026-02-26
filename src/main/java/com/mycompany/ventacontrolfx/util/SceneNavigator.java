package com.mycompany.ventacontrolfx.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import java.io.File;
import java.io.IOException;

/**
 * Utility class for navigating between top-level scenes (Stages).
 */
public class SceneNavigator {

    private static final String STYLE_SHEET = "/view/style.css";

    /**
     * Loads a new scene into the provided stage.
     */
    public static void loadScene(Stage stage, String fxmlPath, String title, double width, double height) {
        loadScene(stage, fxmlPath, title, width, height, false, null);
    }

    public static void loadScene(Stage stage, String fxmlPath, String title, double width, double height,
            boolean fullScreen) {
        loadScene(stage, fxmlPath, title, width, height, fullScreen, null);
    }

    public static void loadScene(Stage stage, String fxmlPath, String title, double width, double height,
            boolean fullScreen, com.mycompany.ventacontrolfx.service.ServiceContainer container) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root, width, height);

            Object controller = loader.getController();
            if (controller instanceof Injectable && container != null) {
                ((Injectable) controller).inject(container);
            }

            // Apply global CSS
            String css = SceneNavigator.class.getResource(STYLE_SHEET).toExternalForm();
            scene.getStylesheets().add(css);

            stage.setTitle(title);
            stage.setScene(scene);
            stage.setMaximized(fullScreen);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            System.err.println("Error loading scene: " + fxmlPath);
            e.printStackTrace();
        }
    }

    /**
     * Simplified icon apply logic.
     */
    public static void applyAppIcon(Stage stage, String iconPath) {
        if (iconPath != null && !iconPath.isEmpty()) {
            File file = new File(iconPath);
            if (file.exists()) {
                stage.getIcons().clear();
                stage.getIcons().add(new Image(file.toURI().toString()));
            }
        }
    }
}
