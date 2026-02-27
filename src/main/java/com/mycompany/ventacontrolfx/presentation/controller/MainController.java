package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.infrastructure.navigation.NavigationService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class MainController implements Injectable {

    @FXML
    private StackPane headerContainer, sidebarContainer, cartContainer;
    @FXML
    private ScrollPane mainContent;
    @FXML
    private VBox loadingOverlay;

    private ServiceContainer container;
    private NavigationService navigationService;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.navigationService = new NavigationService(mainContent, loadingOverlay, container);
        container.setNavigationService(navigationService);

        navigationService.setCartVisibilityListener(shouldShow -> {
            if (cartContainer != null) {
                cartContainer.setVisible(shouldShow);
                cartContainer.setManaged(shouldShow);
            }
        });

        loadStaticComponents();
        showInitialView();
    }

    private void loadStaticComponents() {
        loadComponent("/view/main_header.fxml", headerContainer);
        loadComponent("/view/sidebar.fxml", sidebarContainer);
        loadComponent("/view/cart_panel.fxml", cartContainer);
    }

    private void showInitialView() {
        navigationService.navigateTo("/view/sell_view.fxml");
    }

    private void loadComponent(String fxml, StackPane target) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            Node node = loader.load();
            Object controller = loader.getController();
            if (controller instanceof Injectable) {
                ((Injectable) controller).inject(container);
            }
            target.getChildren().setAll(node);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
