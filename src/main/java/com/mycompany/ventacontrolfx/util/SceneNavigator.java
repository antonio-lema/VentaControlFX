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

            stage.setTitle(title);
            stage.setScene(scene);
            stage.setMaximized(fullScreen);
            stage.centerOnScreen();
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
