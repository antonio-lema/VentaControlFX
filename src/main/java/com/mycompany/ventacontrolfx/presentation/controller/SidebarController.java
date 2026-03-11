package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.infrastructure.navigation.NavigationService;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AuthorizationService;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.util.RippleEffect;
import com.mycompany.ventacontrolfx.util.ModalService;
import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;

public class SidebarController implements Injectable {

    @FXML
    private Button btnSell, btnProducts, btnHistory, btnReturns, btnClosures, btnBilling,
            btnClients, btnConfig, btnLock, btnThemeSettings, btnReports, btnPriceLists, btnVat;

    @FXML
    private Label lblAppName;

    @FXML
    private ImageView brandLogoImage;

    private NavigationService navigationService;
    private AuthorizationService authService;
    private ServiceContainer container;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.authService = container.getAuthService();
        this.navigationService = container.getNavigationService();
        loadBranding(container);
        applyVisualEffects();
        checkRoles();
        setActiveButton(btnSell);
    }

    private void loadBranding(ServiceContainer container) {
        try {
            SaleConfig config = container.getICompanyConfigRepository().load();
            // Nombre de la aplicación guardado
            String appName = config.getAppName();
            if (appName != null && !appName.isBlank()) {
                lblAppName.setText(appName);
            } else {
                lblAppName.setText(config.getCompanyName() != null && !config.getCompanyName().isBlank()
                        ? config.getCompanyName()
                        : "GestionTPV");
            }
            // Logo: preferimos el logoPath de la empresa
            String logoPath = config.getLogoPath();
            if (logoPath == null || logoPath.isBlank()) {
                logoPath = config.getAppIconPath();
            }
            if (logoPath != null && !logoPath.isBlank()) {
                File f = new File(logoPath);
                if (f.exists()) {
                    brandLogoImage.setImage(new Image(f.toURI().toString()));
                    brandLogoImage.setVisible(true);
                    brandLogoImage.setManaged(true);
                }
            }
        } catch (Exception e) {
            // Si falla, se muestran los valores por defecto del FXML
            lblAppName.setText("GestionTPV");
        }
    }

    private void applyVisualEffects() {
        Button[] btns = {
                btnSell, btnHistory, btnReturns, btnProducts,
                btnClients, btnReports, btnClosures, btnBilling,
                btnConfig, btnThemeSettings, btnLock, btnPriceLists, btnVat
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
        setVisible(btnClients, authService.hasPermission("CLIENTES"));
        setVisible(btnClosures, authService.hasPermission("CIERRES"));
        setVisible(btnConfig, authService.hasPermission("CONFIGURACION"));
        setVisible(btnThemeSettings, authService.hasPermission("CONFIGURACION"));
        setVisible(btnReports,
                authService.hasPermission("reporte.vendedores") || authService.hasPermission("reporte.clientes"));
        setVisible(btnBilling, authService.hasPermission("HISTORIAL"));
        setVisible(btnPriceLists, authService.hasPermission("PRODUCTOS"));
        setVisible(btnVat, authService.hasPermission("admin.iva"));
    }

    private void setVisible(Button btn, boolean visible) {
        if (btn != null) {
            btn.setVisible(visible);
            btn.setManaged(visible);
        }
    }

    private void setActiveButton(Button activeBtn) {
        Button[] btns = {
                btnSell, btnHistory, btnReturns, btnProducts,
                btnClients, btnReports, btnClosures, btnBilling,
                btnConfig, btnThemeSettings, btnLock, btnPriceLists, btnVat
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

    @FXML
    private void handleShowClients() {
        setActiveButton(btnClients);
        navigationService.navigateTo("/view/clients.fxml");
    }

    @FXML
    private void handleShowReports() {
        setActiveButton(btnReports);
        navigationService.navigateTo("/view/seller_report.fxml");
    }

    @FXML
    private void showProductsView() {
        setActiveButton(btnProducts);
        navigationService.navigateTo("/view/products.fxml");
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

    @FXML
    private void handleShowPriceLists() {
        setActiveButton(btnPriceLists);
        navigationService.navigateTo("/view/price_lists.fxml");
    }

    @FXML
    private void handleShowVat() {
        setActiveButton(btnVat);
        navigationService.navigateTo("/view/vat_management.fxml");
    }

    // ─── ADMINISTRACIÓN ──────────────────────────────────────

    // ─── ADMINISTRACIÓN ──────────────────────────────────────

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
