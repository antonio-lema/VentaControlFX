package com.mycompany.ventacontrolfx.util;

import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class SceneNavigator {

    public static void loadScene(Stage stage, String fxmlPath, String title, double width, double height,
            boolean fullScreen, ServiceContainer container) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root, width, height);

            Object controller = loader.getController();
            if (controller instanceof Injectable && container != null) {
                ((Injectable) controller).inject(container);
            }

            // Aplicar sistema modular de estilos
            ThemeManager.applyStyles(scene);

            // Configurar icono de la aplicación y título
            if (container != null) {
                var cfg = container.getConfigUseCase().getConfig();
                String iconPath = cfg.getAppIconPath();
                if (iconPath == null || iconPath.isEmpty()) {
                    iconPath = cfg.getLogoPath();
                }

                if (iconPath != null && !iconPath.isEmpty()) {
                    java.io.File file = new java.io.File(iconPath);
                    if (file.exists()) {
                        stage.getIcons().clear();
                        stage.getIcons().add(new javafx.scene.image.Image(file.toURI().toString()));
                    }
                }

                String appName = cfg.getAppName();
                if (appName != null && !appName.isEmpty()) {
                    stage.setTitle(appName + " - " + title);
                } else {
                    stage.setTitle(title);
                }
            } else {
                stage.setTitle(title);
            }

            stage.setScene(scene);
            stage.setMaximized(fullScreen);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
