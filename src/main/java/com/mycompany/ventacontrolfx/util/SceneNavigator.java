package com.mycompany.ventacontrolfx.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.image.Image;
import com.mycompany.ventacontrolfx.service.SaleConfigService;
import com.mycompany.ventacontrolfx.model.SaleConfig;
import java.io.File;
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
        loadScene(stage, fxmlPath, title, width, height, false);
    }

    /**
     * Loads a new scene into the provided stage.
     * 
     * @param stage     The stage where the scene will be set.
     * @param fxmlPath  The path to the FXML file.
     * @param title     The title for the window.
     * @param width     The width of the scene.
     * @param height    The height of the scene.
     * @param maximized Whether the window should be maximized.
     */
    public static void loadScene(Stage stage, String fxmlPath, String title, double width, double height,
            boolean maximized) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root, width, height);

            // Apply global CSS
            String css = SceneNavigator.class.getResource(STYLE_SHEET).toExternalForm();
            scene.getStylesheets().add(css);

            SaleConfigService configService = new SaleConfigService();
            SaleConfig cfg = configService.load();

            String finalTitle = title;
            if (cfg.getAppName() != null && !cfg.getAppName().isEmpty()) {
                finalTitle = title + " - " + cfg.getAppName();
            }

            stage.setTitle(finalTitle);
            stage.setScene(scene);
            stage.centerOnScreen();
            stage.setMaximized(maximized);
            applyAppIcon(stage, cfg);
            stage.show();
        } catch (IOException e) {
            System.err.println("Error loading scene: " + fxmlPath);
            e.printStackTrace();
        }
    }

    private static void applyAppIcon(Stage stage, SaleConfig cfg) {
        String iconPath = cfg.getAppIconPath();

        if (iconPath != null && !iconPath.isEmpty()) {
            File file = new File(iconPath);
            if (file.exists()) {
                try {
                    stage.getIcons().clear();
                    stage.getIcons().add(new Image(file.toURI().toString()));
                } catch (Exception e) {
                    System.err.println("Error loading app icon: " + e.getMessage());
                }
            }
        }
    }
}
