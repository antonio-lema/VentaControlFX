package com.mycompany.ventacontrolfx.util;

import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.Node;
import java.io.IOException;
import java.util.function.Consumer;

public class ModalService {

    public static <T> T showModal(String fxmlPath, String title, Modality modality, StageStyle style,
            com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer container,
            Consumer<T> controllerConsumer) {
        try {
            FXMLLoader loader = new FXMLLoader(ModalService.class.getResource(fxmlPath), container != null ? container.getBundle() : null);
            Parent root = loader.load();
            T controller = loader.getController();

            if (controller instanceof Injectable && container != null) {
                ((Injectable) controller).inject(container);
            }

            if (controllerConsumer != null) {
                controllerConsumer.accept(controller);
            }

            Stage stage = new Stage();
            stage.initModality(modality);
            stage.initStyle(style);
            stage.setTitle(title);

            Scene scene = new Scene(root);
            if (style == StageStyle.TRANSPARENT) {
                scene.setFill(null);
            }
            if (container != null) {
                container.getThemeManager().applyFullTheme(scene);
            }
            stage.setScene(scene);
            stage.sizeToScene();
            stage.centerOnScreen();

            stage.showAndWait();
            return controller;
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Error", "No se pudo abrir la ventana: " + title + "\n\nError: " + e.toString());
            return null;
        }
    }

    public static <T> T showTransparentModal(String fxmlPath, String title,
            com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer container,
            Consumer<T> controllerConsumer) {
        return showModal(fxmlPath, title, Modality.APPLICATION_MODAL, StageStyle.TRANSPARENT, container,
                controllerConsumer);
    }

    public static <T> T showStandardModal(String fxmlPath, String title,
            com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer container,
            Consumer<T> controllerConsumer) {
        return showModal(fxmlPath, title, Modality.APPLICATION_MODAL, StageStyle.DECORATED, container,
                controllerConsumer);
    }

    public static <T> T showFullScreenModal(String fxmlPath, String title,
            com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer container,
            Consumer<T> controllerConsumer) {
        try {
            FXMLLoader loader = new FXMLLoader(ModalService.class.getResource(fxmlPath), container != null ? container.getBundle() : null);
            if (container != null) {
                loader.setResources(container.getBundle());
            }
            Parent root = loader.load();
            T controller = loader.getController();

            if (controller instanceof Injectable && container != null) {
                ((Injectable) controller).inject(container);
            }

            if (controllerConsumer != null) {
                controllerConsumer.accept(controller);
            }

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.setTitle(title);

            Scene scene = new Scene(root);
            if (container != null) {
                container.getThemeManager().applyFullTheme(scene);
            }
            stage.setScene(scene);

            stage.setMaximized(true);
            stage.setFullScreen(true);
            stage.setFullScreenExitKeyCombination(javafx.scene.input.KeyCombination.NO_MATCH);

            // Prevent ESC button from closing the modal
            scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
                if (e.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    e.consume();
                }
            });

            stage.showAndWait();
            return controller;
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Error", "No se pudo abrir la ventana: " + title + "\n\nError: " + e.toString());
            return null;
        }
    }
}
