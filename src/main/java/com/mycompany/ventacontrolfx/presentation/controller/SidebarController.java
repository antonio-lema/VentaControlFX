package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.infrastructure.navigation.NavigationService;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AuthorizationService;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.util.RippleEffect;
import com.mycompany.ventacontrolfx.util.ModalService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class SidebarController implements Injectable {

    @FXML
    private Button btnSell, btnProducts, btnProductsList, btnCategories,
            btnHistory, btnReturns, btnClosures, btnBilling,
            btnClients,
            btnUsers, btnUsersList, btnRoles,
            btnConfig, btnLock, btnThemeSettings, btnMassivePrices, btnPriceLists,
            btnReports, btnSellerReport, btnClientReport;

    @FXML
    private VBox productsSubmenu, reportsSubmenu, usersSubmenu;

    @FXML
    private Label lblProductsArrow, lblReportsArrow, lblUsersArrow;

    private NavigationService navigationService;
    private AuthorizationService authService;
    private ServiceContainer container;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.authService = container.getAuthService();
        this.navigationService = container.getNavigationService();
        applyVisualEffects();
        checkRoles();

        // El botón activo por defecto al abrir la aplicación
        setActiveButton(btnSell);
    }

    private void applyVisualEffects() {
        Button[] btns = {
                btnSell, btnProducts, btnProductsList, btnCategories,
                btnHistory, btnReturns, btnClosures, btnBilling,
                btnClients,
                btnUsers, btnUsersList, btnRoles,
                btnConfig, btnLock, btnThemeSettings, btnMassivePrices, btnPriceLists,
                btnReports, btnSellerReport, btnClientReport
        };
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
        setVisible(btnReturns, authService.hasPermission("venta.devolucion"));
        setVisible(btnProducts, authService.hasPermission("PRODUCTOS"));
        setVisible(btnProductsList, authService.hasPermission("PRODUCTOS"));
        setVisible(btnCategories, authService.hasPermission("PRODUCTOS"));
        setVisible(btnPriceLists, authService.hasPermission("PRODUCTOS"));
        setVisible(btnClients, authService.hasPermission("CLIENTES"));
        setVisible(btnClosures, authService.hasPermission("CIERRES"));
        setVisible(btnUsers, authService.hasPermission("usuario.crear"));
        setVisible(btnConfig, authService.hasPermission("CONFIGURACION"));
        setVisible(btnThemeSettings, authService.hasPermission("CONFIGURACION"));

        setVisible(btnMassivePrices, authService.hasPermission("admin.precios_masivo"));
        setVisible(btnReports,
                authService.hasPermission("reporte.vendedores") || authService.hasPermission("reporte.clientes"));
        setVisible(btnSellerReport, authService.hasPermission("reporte.vendedores"));
        setVisible(btnClientReport, authService.hasPermission("reporte.clientes"));
        setVisible(btnBilling, authService.hasPermission("HISTORIAL"));
    }

    private void setVisible(Button btn, boolean visible) {
        if (btn != null) {
            btn.setVisible(visible);
            btn.setManaged(visible);
        }
    }

    private void setActiveButton(Button activeBtn) {
        Button[] btns = {
                btnSell, btnProducts, btnProductsList, btnCategories,
                btnHistory, btnReturns, btnClosures, btnBilling,
                btnClients,
                btnUsers, btnUsersList, btnRoles,
                btnConfig, btnLock, btnThemeSettings, btnPriceLists, btnMassivePrices,
                btnReports, btnSellerReport, btnClientReport
        };
        for (Button b : btns) {
            if (b != null)
                b.getStyleClass().remove("active-sidebar-button");
        }
        if (activeBtn != null)
            activeBtn.getStyleClass().add("active-sidebar-button");
    }

    // ─── PRINCIPAL ──────────────────────────────────────────

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

    // ─── CATÁLOGO ────────────────────────────────────────────

    @FXML
    private void toggleProductsMenu() {
        setActiveButton(btnProducts);
        boolean visible = !productsSubmenu.isVisible();
        productsSubmenu.setVisible(visible);
        productsSubmenu.setManaged(visible);
        lblProductsArrow.setText(visible ? "˅" : "›");
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
    private void showPriceListsView() {
        setActiveButton(btnPriceLists);
        navigationService.navigateTo("/view/price_lists.fxml");
    }

    @FXML
    private void handleShowVatPrices() {
        setActiveButton(btnMassivePrices);
        navigationService.navigateTo("/view/vat_management.fxml");
    }

    // ─── CLIENTES ────────────────────────────────────────────

    @FXML
    private void handleShowClients() {
        setActiveButton(btnClients);
        navigationService.navigateTo("/view/clients.fxml");
    }

    // ─── ANALÍTICA ───────────────────────────────────────────

    @FXML
    private void toggleReportsMenu() {
        setActiveButton(btnReports);
        boolean visible = !reportsSubmenu.isVisible();
        reportsSubmenu.setVisible(visible);
        reportsSubmenu.setManaged(visible);
        if (lblReportsArrow != null)
            lblReportsArrow.setText(visible ? "˅" : "‹");
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
    private void handleShowClosures() {
        setActiveButton(btnClosures);
        navigationService.navigateTo("/view/closure_history.fxml");
    }

    @FXML
    private void handleShowBilling() {
        setActiveButton(btnBilling);
        navigationService.navigateTo("/view/fiscal_documents.fxml");
    }

    // ─── ADMINISTRACIÓN ──────────────────────────────────────

    @FXML
    private void toggleUsersMenu() {
        setActiveButton(btnUsers);
        boolean visible = !usersSubmenu.isVisible();
        usersSubmenu.setVisible(visible);
        usersSubmenu.setManaged(visible);
        if (lblUsersArrow != null)
            lblUsersArrow.setText(visible ? "˅" : "›");
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

    // ─── ACCIONES INFERIORES ─────────────────────────────────

    @FXML
    private void handleLockApp() {
        ModalService.showFullScreenModal("/view/lock_screen.fxml", "Pantalla de Bloqueo", container, null);
    }
}
