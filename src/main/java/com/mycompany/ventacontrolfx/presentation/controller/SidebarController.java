package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.infrastructure.navigation.NavigationService;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AuthorizationService;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.util.RippleEffect;
import com.mycompany.ventacontrolfx.util.ModalService;
import com.mycompany.ventacontrolfx.presentation.theme.ThemeManager;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;

public class SidebarController implements Injectable {

    @FXML
    private Button btnSell, btnProducts, btnProductsList, btnCategories, btnHistory, btnReturns, btnClosures,
            btnClients, btnUsers,
            btnConfig, btnLock, btnThemeToggle, btnReports, btnSellerReport, btnClientReport, btnThemeSettings,
            btnUsersList, btnRoles;
    @FXML
    private VBox productsSubmenu, reportsSubmenu, usersSubmenu;
    @FXML
    private Label lblProductsArrow, lblReportsArrow, lblUsersArrow;
    @FXML
    private FontAwesomeIconView themeIcon;
    @FXML
    private Label lblTheme;

    private boolean isDarkTheme = false;

    private NavigationService navigationService;
    private AuthorizationService authService;
    private ServiceContainer container;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.authService = container.getAuthService();
        this.navigationService = container.getNavigationService();
        String theme = "LIGHT";
        try {
            String savedTheme = container.getAppSettingsRepository().getSetting("ui.theme_mode");
            if (savedTheme != null)
                theme = savedTheme;
        } catch (Exception e) {
            // Fallback to light
        }
        this.isDarkTheme = "DARK".equals(theme);

        applyVisualEffects();
        checkRoles();
        updateThemeUI();
    }

    private void applyVisualEffects() {
        Button[] btns = { btnSell, btnProducts, btnProductsList, btnCategories, btnHistory, btnReturns, btnClosures,
                btnClients,
                btnUsers, btnConfig, btnLock, btnThemeToggle, btnReports, btnSellerReport, btnClientReport,
                btnThemeSettings };
        for (Button b : btns) {
            if (b != null)
                RippleEffect.applyTo(b);
        }
    }

    private void checkRoles() {
        if (authService == null)
            return;

        setVisible(btnSell, authService.hasPermission("VENTAS"));
        setVisible(btnHistory, authService.hasPermission("HISTORIAL"));
        setVisible(btnProducts, authService.hasPermission("PRODUCTOS"));
        setVisible(btnProductsList, authService.hasPermission("PRODUCTOS"));
        setVisible(btnCategories, authService.hasPermission("PRODUCTOS"));
        setVisible(btnClients, authService.hasPermission("CLIENTES"));
        setVisible(btnClosures, authService.hasPermission("CIERRES"));
        setVisible(btnReturns, authService.hasPermission("venta.devolucion"));
        setVisible(btnUsers, authService.hasPermission("USUARIOS"));
        setVisible(btnConfig, authService.hasPermission("CONFIGURACION"));
        setVisible(btnThemeSettings, authService.hasPermission("CONFIGURACION"));

        boolean hasReports = authService.hasPermission("HISTORIAL");
        setVisible(btnReports, hasReports);

        if (productsSubmenu != null && !authService.hasPermission("PRODUCTOS")) {
            productsSubmenu.setVisible(false);
            productsSubmenu.setManaged(false);
        }
    }

    private void setVisible(Button btn, boolean visible) {
        if (btn != null) {
            btn.setVisible(visible);
            btn.setManaged(visible);
        }
    }

    private void setActiveButton(Button activeBtn) {
        Button[] btns = { btnSell, btnProducts, btnProductsList, btnCategories, btnHistory, btnReturns, btnClosures,
                btnClients,
                btnUsers, btnConfig, btnLock, btnReports, btnSellerReport, btnClientReport, btnThemeSettings,
                btnUsersList, btnRoles };
        for (Button b : btns) {
            if (b != null)
                b.getStyleClass().remove("active-sidebar-button");
        }
        if (activeBtn != null)
            activeBtn.getStyleClass().add("active-sidebar-button");
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
    private void handleShowReturns() {
        setActiveButton(btnReturns);
        navigationService.navigateTo("/view/return_list.fxml");
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
    private void toggleProductsMenu() {
        setActiveButton(btnProducts);
        boolean visible = !productsSubmenu.isVisible();
        productsSubmenu.setVisible(visible);
        productsSubmenu.setManaged(visible);
        lblProductsArrow.setText(visible ? "v" : "<");
    }

    @FXML
    private void toggleReportsMenu() {
        setActiveButton(btnReports);
        boolean visible = !reportsSubmenu.isVisible();
        reportsSubmenu.setVisible(visible);
        reportsSubmenu.setManaged(visible);
        if (lblReportsArrow != null)
            lblReportsArrow.setText(visible ? "v" : "<");
    }

    @FXML
    private void showSellerReport() {
        setActiveButton(btnSellerReport);
        navigationService.navigateTo("/view/seller_report.fxml");
    }

    @FXML
    private void showClientReport() {
        setActiveButton(btnClientReport);
        navigationService.navigateTo("/view/client_report.fxml");
    }

    @FXML
    private void toggleUsersMenu() {
        setActiveButton(btnUsers);
        boolean visible = !usersSubmenu.isVisible();
        usersSubmenu.setVisible(visible);
        usersSubmenu.setManaged(visible);
        if (lblUsersArrow != null)
            lblUsersArrow.setText(visible ? "v" : "<");
    }

    @FXML
    private void handleShowUsers() {
        setActiveButton(btnUsersList);
        authService.checkAdminAccess(() -> navigationService.navigateTo("/view/manage_users.fxml"));
    }

    @FXML
    private void handleShowRoles() {
        setActiveButton(btnRoles);
        authService.checkAdminAccess(() -> navigationService.navigateTo("/view/manage_roles.fxml"));
    }

    @FXML
    private void handleShowConfig() {
        setActiveButton(btnConfig);
        authService.checkAdminAccess(() -> navigationService.navigateTo("/view/sale_config.fxml"));
    }

    @FXML
    private void handleShowCustomization() {
        setActiveButton(btnThemeSettings);
        navigationService.navigateTo("/view/customization_panel.fxml");
    }

    @FXML
    private void handleLockApp() {
        ModalService.showFullScreenModal("/view/lock_screen.fxml", "Pantalla de Bloqueo", container, null);
    }

    @FXML
    private void handleThemeToggle() {
        isDarkTheme = !isDarkTheme;
        try {
            container.getAppSettingsRepository().saveSetting("ui.theme_mode", isDarkTheme ? "DARK" : "LIGHT");
            container.getThemeManager().applyTheme(btnThemeToggle.getScene());
            updateThemeUI();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateThemeUI() {
        if (isDarkTheme) {
            if (themeIcon != null)
                themeIcon.setGlyphName("SUN_ALT");
            if (lblTheme != null)
                lblTheme.setText("Modo Claro");
        } else {
            if (themeIcon != null)
                themeIcon.setGlyphName("MOON_ALT");
            if (lblTheme != null)
                lblTheme.setText("Modo Oscuro");
        }
    }
}
