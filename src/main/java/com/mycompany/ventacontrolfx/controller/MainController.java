package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.service.*;
import com.mycompany.ventacontrolfx.util.Injectable;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import java.io.IOException;

/**
 * Main application coordinator.
 * Orchestrates the static layout and initializes the navigation.
 */
public class MainController implements Injectable {
    @FXML
    private StackPane headerContainer, sidebarContainer, cartContainer, statusBarContainer;
    @FXML
    private ScrollPane mainContent;
    @FXML
    private VBox loadingOverlay;

    private ServiceContainer container;
    private NavigationService navigationService;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        // Core navigation setup
        navigationService = new NavigationService(mainContent, loadingOverlay, container);
        container.setNavigationService(navigationService);

        // Fix #4 – Visibilidad del carrito: mostrar solo en la vista de ventas.
        // setManaged(false) es CRÍTICO: además de ocultarlo, libera el espacio en el
        // BorderPane.
        navigationService.setCartVisibilityListener(shouldShow -> {
            cartContainer.setVisible(shouldShow);
            cartContainer.setManaged(shouldShow);
        });

        loadStaticComponents();
        showInitialView();
    }

    public void initialize() {
        // We wait for inject() to be called by SceneNavigator
    }

    private void loadStaticComponents() {
        // Load Header and link it with Navigation for search delegation
        HeaderController hc = loadStaticComponent("/view/main_header.fxml", headerContainer);
        if (hc != null)
            hc.setNavigationService(navigationService);

        // Load Sidebar for menu navigation
        SidebarController sc = loadStaticComponent("/view/sidebar.fxml", sidebarContainer);
        if (sc != null)
            sc.init(navigationService);

        // Load Cart for item management (se carga siempre; la visibilidad la controla
        // el listener)
        CartController cc = loadStaticComponent("/view/cart_panel.fxml", cartContainer);
        if (cc != null)
            cc.init(navigationService);

        // Load Status Bar (it will self-refresh via GlobalEventBus)
        loadStaticComponent("/view/status_bar.fxml", statusBarContainer);
    }

    private void showInitialView() {
        navigationService.navigateTo("/view/sell_view.fxml");
    }

    /**
     * Helper to load a component into a container and perform automatic Injection.
     */
    private <T> T loadStaticComponent(String fxmlPath, StackPane containerNode) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node node = loader.load();
            T controller = loader.getController();

            // Automatic Dependency Injection
            if (controller instanceof Injectable) {
                ((Injectable) controller).inject(container);
            }

            containerNode.getChildren().setAll(node);
            return controller;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
