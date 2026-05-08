package com.mycompany.ventacontrolfx.presentation.controller.main;

import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.infrastructure.navigation.NavigationService;

import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

/**
 * Gestor de la estructura visual (Shell) de la aplicación.
 * Maneja la carga de componentes fijos y la navegación inicial.
 */
public class AppShellManager {

    private final ServiceContainer container;
    private final NavigationService navService;
    private HeaderController headerController;

    public AppShellManager(ServiceContainer container, NavigationService navService) {
        this.container = container;
        this.navService = navService;
    }

    public void build(StackPane headerTarget, StackPane sidebarTarget, StackPane cartTarget) {
        this.headerController = loadWithController("/view/main/main_header.fxml", headerTarget);
        load("/view/sidebar/sidebar.fxml", sidebarTarget);
        load("/view/cart/cart_panel.fxml", cartTarget);

        // Conectar visibilidad de búsqueda entre navegación y cabecera
        if (headerController != null) {
            navService.setSearchBarVisibilityListener(headerController::setSearchBarVisible);
        }

        navService.setCartVisibilityListener(shouldShow -> {
            if (cartTarget != null) {
                cartTarget.setVisible(shouldShow);
                cartTarget.setManaged(shouldShow);
            }
        });
    }

    private void showInitialView() {
        navService.navigateTo("/view/cart/sell_view.fxml");
    }

    private void load(String fxml, StackPane target) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml), container.getBundle());
            Node node = loader.load();
            Object controller = loader.getController();
            if (controller instanceof Injectable) ((Injectable) controller).inject(container);
            target.getChildren().setAll(node);
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError(container.getBundle().getString("shell.error"), container.getBundle().getString("shell.fxml_load_error") + ": " + fxml + "\n" + e.getMessage());
        }
    }

    private <T> T loadWithController(String fxml, StackPane target) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml), container.getBundle());
            Node node = loader.load();
            Object controller = loader.getController();
            if (controller instanceof Injectable) ((Injectable) controller).inject(container);
            target.getChildren().setAll(node);
            return (T) controller;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}



