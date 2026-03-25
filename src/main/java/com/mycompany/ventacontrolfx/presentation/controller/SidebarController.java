package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.infrastructure.navigation.NavigationService;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AuthorizationService;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.util.RippleEffect;
import com.mycompany.ventacontrolfx.util.ModalService;
import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import java.io.File;

public class SidebarController implements Injectable {

    @FXML
    private Button btnSell, btnProducts, btnCategories, btnHistory, btnReturns, btnClosures, btnBilling,
            btnClients, btnConfig, btnLock, btnThemeSettings, btnReports, btnClientReport, btnPriceLists, btnVat,
            btnUsers, btnRoles, btnPromotions, btnWorkSessions;

    @FXML
    private VBox contentVentas, contentCatalogo, contentClientes, contentGestion, contentSistema;

    @FXML
    private Button sectionVentas, sectionCatalogo, sectionClientes, sectionGestion, sectionSistema;

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

        try {
            loadBranding(container);
        } catch (Exception e) {
            System.err.println("Error loading branding in Sidebar: " + e.getMessage());
        }

        try {
            checkRoles();
        } catch (Exception e) {
            System.err.println("Error checking roles in Sidebar: " + e.getMessage());
        }

        applyVisualEffects();
        initSections();

        try {
            setActiveButton(btnSell);
            // Asegurar que el foco inicial no caiga en el botón de bloquear al final del
            // VBox
            Platform.runLater(() -> {
                if (btnSell != null)
                    btnSell.requestFocus();
            });
        } catch (Exception e) {
            System.err.println("Error setting Active Button in Sidebar: " + e.getMessage());
        }
    }

    private void initSections() {
        collapseAll();
    }

    private void collapseAll() {
        setVisible(contentVentas, false);
        setVisible(contentCatalogo, false);
        setVisible(contentClientes, false);
        setVisible(contentGestion, false);
        setVisible(contentSistema, false);
    }

    private void toggleSection(VBox container) {
        if (container == null)
            return;
        boolean wasVisible = container.isVisible();

        collapseAll(); // Colapsar todo primero

        if (!wasVisible) {
            container.setVisible(true);
            container.setManaged(true);
        }
    }

    @FXML
    private void toggleVentas() {
        toggleSection(contentVentas);
    }

    @FXML
    private void toggleCatalogo() {
        toggleSection(contentCatalogo);
    }

    @FXML
    private void toggleClientes() {
        toggleSection(contentClientes);
    }

    @FXML
    private void toggleGestion() {
        toggleSection(contentGestion);
    }

    @FXML
    private void toggleSistema() {
        toggleSection(contentSistema);
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
                btnConfig, btnThemeSettings, btnLock, btnPriceLists, btnVat,
                btnUsers, btnRoles, btnPromotions, btnWorkSessions
        };
        for (Button b : btns) {
            if (b != null)
                RippleEffect.applyTo(b);
        }
    }

    private void checkRoles() {
        if (authService == null)
            return;

        // Forzamos visibilidad de Vender para evitar sidebar vacío
        setVisible(btnSell, true);

        setVisible(btnHistory,
                authService.hasPermission("HISTORIAL") || authService.hasPermission("venta.listar"));
        setVisible(btnReturns, authService.hasPermission("venta.devolucion"));
        setVisible(btnProducts, authService.hasPermission("PRODUCTOS") || authService.hasPermission("producto.listar"));
        setVisible(btnCategories,
                authService.hasPermission("PRODUCTOS") || authService.hasPermission("categoria.listar"));
        setVisible(btnClients, authService.hasPermission("CLIENTES") || authService.hasPermission("cliente.listar"));
        setVisible(btnClosures, authService.hasPermission("CIERRES") || authService.hasPermission("caja.historial"));
        setVisible(btnConfig,
                authService.hasPermission("CONFIGURACION") || authService.hasPermission("config.general"));
        setVisible(btnThemeSettings,
                authService.hasPermission("CONFIGURACION") || authService.hasPermission("config.estetica"));
        setVisible(btnReports,
                authService.hasPermission("reporte.venta") || authService.hasPermission("reporte.vendedores"));
        setVisible(btnClientReport,
                authService.hasPermission("reporte.cliente") || authService.hasPermission("HISTORIAL"));
        setVisible(btnBilling,
                authService.hasPermission("admin.facturacion") || authService.hasPermission("HISTORIAL"));
        setVisible(btnPriceLists, authService.hasPermission("admin.precios") || authService.hasPermission("PRODUCTOS"));
        setVisible(btnVat, authService.hasPermission("admin.iva"));
        setVisible(btnUsers, authService.hasPermission("usuario.crear"));
        setVisible(btnRoles, authService.hasPermission("rol.editar"));
        setVisible(btnPromotions, authService.hasPermission("admin.precios") || authService.hasPermission("PRODUCTOS"));
        setVisible(btnWorkSessions, true); // Accessible to all logged in users for now
        setVisible(btnLock, true);

        // Ocultar secciones enteras si no hay hijos permitidos
        updateSectionVisibility(sectionVentas, contentVentas);
        updateSectionVisibility(sectionCatalogo, contentCatalogo);
        updateSectionVisibility(sectionClientes, contentClientes);
        updateSectionVisibility(sectionGestion, contentGestion);
        updateSectionVisibility(sectionSistema, contentSistema);
    }

    private void updateSectionVisibility(Button sectionHeader, VBox content) {
        if (content == null || sectionHeader == null)
            return;
        boolean anyVisible = false;
        for (Node node : content.getChildren()) {
            if (node.isVisible()) {
                anyVisible = true;
                break;
            }
        }
        setVisible(sectionHeader, anyVisible);
        if (!anyVisible) {
            setVisible(content, false);
        }
    }

    private void setVisible(Node node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    private void setActiveButton(Button activeBtn) {
        Button[] btns = {
                btnSell, btnHistory, btnReturns, btnProducts, btnCategories,
                btnClients, btnReports, btnClientReport, btnClosures, btnBilling,
                btnConfig, btnThemeSettings, btnLock, btnPriceLists, btnVat,
                btnUsers, btnRoles, btnPromotions, btnWorkSessions
        };
        for (Button b : btns) {
            if (b != null)
                b.getStyleClass().remove("active-sidebar-button");
        }
        if (activeBtn != null) {
            activeBtn.getStyleClass().add("active-sidebar-button");
            expandParentSection(activeBtn);
        }
    }

    private void expandParentSection(Button btn) {
        if (btn == null || btn.getParent() == null)
            return;
        if (btn.getParent() instanceof VBox) {
            VBox parent = (VBox) btn.getParent();
            if (parent.getStyleClass().contains("sidebar-section-content")) {
                setVisible(parent, true);
            }
        }
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
    private void handleShowClientReport() {
        setActiveButton(btnClientReport);
        navigationService.navigateTo("/view/client_report.fxml");
    }

    @FXML
    private void showProductsView() {
        setActiveButton(btnProducts);
        navigationService.navigateTo("/view/products.fxml");
    }

    @FXML
    private void handleShowCategories() {
        setActiveButton(btnCategories);
        navigationService.navigateTo("/view/categories.fxml");
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
    private void handleShowPromotions() {
        setActiveButton(btnPromotions);
        navigationService.navigateTo("/view/promotions.fxml");
    }

    @FXML
    private void handleShowVat() {
        setActiveButton(btnVat);
        navigationService.navigateTo("/view/vat_management.fxml");
    }

    @FXML
    private void handleShowUsers() {
        setActiveButton(btnUsers);
        navigationService.navigateTo("/view/manage_users.fxml");
    }

    @FXML
    private void handleShowRoles() {
        setActiveButton(btnRoles);
        navigationService.navigateTo("/view/manage_roles.fxml");
    }

    @FXML
    private void handleShowWorkSessions() {
        setActiveButton(btnWorkSessions);
        navigationService.navigateTo("/view/shift_management.fxml");
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
    private void handleLock() {
        ModalService.showFullScreenModal("/view/lock_screen.fxml", "Pantalla de Bloqueo", container, null);
    }
}
