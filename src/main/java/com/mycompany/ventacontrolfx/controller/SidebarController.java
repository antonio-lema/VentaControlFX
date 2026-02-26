package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.service.NavigationService;
import com.mycompany.ventacontrolfx.service.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AuthorizationService;
import com.mycompany.ventacontrolfx.util.Injectable;
import com.mycompany.ventacontrolfx.util.RippleEffect;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class SidebarController implements Injectable {
    @FXML
    private Button btnSell, btnProducts, btnProductsList, btnCategories, btnHistory, btnClosures, btnClients, btnUsers,
            btnConfig, btnLock;
    @FXML
    private VBox productsSubmenu;
    @FXML
    private Label lblProductsArrow;

    private NavigationService navigationService;
    private AuthorizationService authService;
    private ServiceContainer container;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.authService = container.getAuthService();
        applyVisualEffects();
        checkRoles();
    }

    public void init(NavigationService navigationService) {
        this.navigationService = navigationService;
    }

    private void applyVisualEffects() {
        Button[] btns = { btnSell, btnProducts, btnProductsList, btnCategories, btnHistory, btnClosures, btnClients,
                btnUsers, btnConfig, btnLock };
        for (Button b : btns)
            if (b != null)
                RippleEffect.applyTo(b);
    }

    private void checkRoles() {
        if (authService == null)
            return;

        boolean admin = authService.isAdmin();
        if (btnUsers != null) {
            btnUsers.setVisible(admin);
            btnUsers.setManaged(admin);
        }
        if (btnConfig != null) {
            btnConfig.setVisible(admin);
            btnConfig.setManaged(admin);
        }
    }

    private void setActiveButton(Button activeBtn) {
        Button[] btns = { btnSell, btnProducts, btnProductsList, btnCategories, btnHistory, btnClosures, btnClients,
                btnUsers, btnConfig, btnLock };
        for (Button b : btns) {
            if (b != null) {
                b.getStyleClass().remove("active-sidebar-button");
            }
        }
        if (activeBtn != null && !activeBtn.getStyleClass().contains("active-sidebar-button")) {
            activeBtn.getStyleClass().add("active-sidebar-button");
        }
    }

    @FXML
    private void showSellView() {
        setActiveButton(btnSell);
        navigationService.navigateTo("/view/sell_view.fxml");
    }

    @FXML
    private void showHistoryView() {
        setActiveButton(btnHistory);
        navigationService.navigateTo("/view/sales.fxml");
    }

    @FXML
    private void toggleProductsMenu() {
        setActiveButton(btnProducts);
        boolean visible = !productsSubmenu.isVisible();
        productsSubmenu.setVisible(visible);
        productsSubmenu.setManaged(visible);
        lblProductsArrow.setText(visible ? "v" : ">");
    }

    @FXML
    private void handleShowClosures() {
        setActiveButton(btnClosures);
        navigationService.navigateTo("/view/closure_history.fxml");
    }

    @FXML
    private void showProductsView() {
        setActiveButton(btnProductsList);
        navigationService.navigateTo("/view/products.fxml");
    }

    @FXML
    private void showCategoriesView() {
        setActiveButton(btnCategories);
        navigationService.navigateTo("/view/categories.fxml");
    }

    @FXML
    private void showAddClientDialog() {
        setActiveButton(btnClients);
        navigationService.navigateTo("/view/clients.fxml");
    }

    @FXML
    private void handleShowUsers() {
        setActiveButton(btnUsers);
        authService.checkAdminAccess(() -> navigationService.navigateTo("/view/manage_users.fxml"));
    }

    @FXML
    private void handleShowConfig() {
        setActiveButton(btnConfig);
        authService.checkAdminAccess(() -> navigationService.navigateTo("/view/sale_config.fxml"));
    }

    @FXML
    private void handleLockApp() {
        com.mycompany.ventacontrolfx.util.ModalService.showFullScreenModal("/view/lock_screen.fxml",
                "Pantalla de Bloqueo", container, null);
    }
}
