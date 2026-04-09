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
            btnUsers, btnRoles, btnPromotions, btnWorkSessions, btnOperativeControl, btnStaffCalendar,
            btnPunctualityAudit;

    @FXML
    private VBox contentVentas, contentCatalogo, contentPersonal, contentClientes, contentGestion, contentSistema;

    @FXML
    private Button sectionVentas, sectionCatalogo, sectionPersonal, sectionClientes, sectionGestion, sectionSistema;

    @FXML
    private Label lblAppName;

    @FXML
    private ImageView brandLogoImage;

    private NavigationService navigationService;
    private AuthorizationService authService;
    private ServiceContainer container;
    private VBox activeSection;

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
            // Asegurar que el foco inicial no caiga en el bot\u00c3\u00b3n de bloquear al final del
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
        collapseAll(false);
    }

    private void collapseAll(boolean force) {
        if (force || activeSection != contentVentas) {
            setVisible(contentVentas, false);
            sectionVentas.getStyleClass().remove("sidebar-section-header-active");
        }
        if (force || activeSection != contentCatalogo) {
            setVisible(contentCatalogo, false);
            sectionCatalogo.getStyleClass().remove("sidebar-section-header-active");
        }
        if (force || activeSection != contentPersonal) {
            setVisible(contentPersonal, false);
            sectionPersonal.getStyleClass().remove("sidebar-section-header-active");
        }
        if (force || activeSection != contentClientes) {
            setVisible(contentClientes, false);
            sectionClientes.getStyleClass().remove("sidebar-section-header-active");
        }
        if (force || activeSection != contentGestion) {
            setVisible(contentGestion, false);
            sectionGestion.getStyleClass().remove("sidebar-section-header-active");
        }
        if (force || activeSection != contentSistema) {
            setVisible(contentSistema, false);
            sectionSistema.getStyleClass().remove("sidebar-section-header-active");
        }
    }

    private void toggleSection(VBox container) {
        if (container == null)
            return;
        boolean wasVisible = container.isVisible();

        // Acorde\u00c3\u00b3n estricto: forzar cierre de todo lo dem\u00c3\u00a1s
        collapseAll(true);

        if (!wasVisible) {
            container.setVisible(true);
            container.setManaged(true);

            // Aplicar estilo activo a la cabecera correspondiente
            Button header = getHeaderForContent(container);
            if (header != null)
                header.getStyleClass().add("sidebar-section-header-active");
        }
    }

    private Button getHeaderForContent(VBox container) {
        if (container == contentVentas)
            return sectionVentas;
        if (container == contentCatalogo)
            return sectionCatalogo;
        if (container == contentPersonal)
            return sectionPersonal;
        if (container == contentClientes)
            return sectionClientes;
        if (container == contentGestion)
            return sectionGestion;
        if (container == contentSistema)
            return sectionSistema;
        return null;
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
    private void togglePersonal() {
        toggleSection(contentPersonal);
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
            // Nombre de la aplicaci\u00c3\u00b3n guardado
            String appName = config.getAppName();
            if (appName != null && !appName.isBlank()) {
                lblAppName.setText(appName);
            } else {
                lblAppName.setText(config.getCompanyName() != null && !config.getCompanyName().isBlank()
                        ? config.getCompanyName()
                        : container.getBundle().getString("app.name.default"));
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
            lblAppName.setText(container.getBundle().getString("app.name.default"));
        }
    }

    private void applyVisualEffects() {
        Button[] btns = {
                btnSell, btnHistory, btnReturns, btnProducts,
                btnClients, btnReports, btnClosures, btnBilling,
                btnConfig, btnThemeSettings, btnLock, btnPriceLists, btnVat,
                btnUsers, btnRoles, btnPromotions, btnWorkSessions, btnStaffCalendar, btnPunctualityAudit,
                btnOperativeControl
        };
        for (Button b : btns) {
            if (b != null)
                RippleEffect.applyTo(b);
        }
    }

    private void checkRoles() {
        if (authService == null)
            return;

        // Forzamos visibilidad de Vender para evitar sidebar vac\u00c3\u00ado
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
        setVisible(btnStaffCalendar, true);
        setVisible(btnOperativeControl,
                authService.hasPermission("CIERRES") || authService.hasPermission("admin.facturacion"));
        setVisible(btnPunctualityAudit,
                authService.hasPermission("CIERRES") || authService.hasPermission("rol.editar"));
        setVisible(btnLock, true);

        // Ocultar secciones enteras si no hay hijos permitidos
        updateSectionVisibility(sectionVentas, contentVentas);
        updateSectionVisibility(sectionCatalogo, contentCatalogo);
        updateSectionVisibility(sectionPersonal, contentPersonal);
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
                btnUsers, btnRoles, btnPromotions, btnWorkSessions, btnStaffCalendar,
                btnOperativeControl, btnPunctualityAudit
        };
        for (Button b : btns) {
            if (b != null)
                b.getStyleClass().remove("active-sidebar-button");
        }
        if (activeBtn != null) {
            activeBtn.getStyleClass().add("active-sidebar-button");
            this.activeSection = expandParentSection(activeBtn);
        }
    }

    private VBox expandParentSection(Button btn) {
        if (btn == null || btn.getParent() == null)
            return null;
        if (btn.getParent() instanceof VBox) {
            VBox parent = (VBox) btn.getParent();
            if (parent.getStyleClass().contains("sidebar-section-content")) {
                setVisible(parent, true);

                // Asegurar que la cabecera se vea activa
                Button header = getHeaderForContent(parent);
                if (header != null && !header.getStyleClass().contains("sidebar-section-header-active")) {
                    header.getStyleClass().add("sidebar-section-header-active");
                }

                return parent;
            }
        }
        return null;
    }

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac PRINCIPAL \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac

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

    @FXML
    private void handleShowOperativeDashboard() {
        setActiveButton(btnOperativeControl);
        navigationService.navigateTo("/view/operative_dashboard.fxml");
    }

    @FXML
    private void handleShowPunctualityAudit() {
        setActiveButton(btnPunctualityAudit);
        navigationService.navigateTo("/view/punctuality_audit.fxml");
    }

    @FXML
    private void handleShowStaffCalendar() {
        setActiveButton(btnStaffCalendar);
        ModalService.showFullScreenModal("/view/staff_calendar.fxml",
                container.getBundle().getString("sidebar.staff_calendar"), container, null);
    }

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac ADMINISTRACI\u00c3\u201cN \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac ADMINISTRACI\u00c3\u201cN \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac

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

    // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac ACCIONES INFERIORES \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac

    @FXML
    private void handleLock() {
        ModalService.showFullScreenModal("/view/lock_screen.fxml",
                container.getBundle().getString("sidebar.lock_screen"), container, null);
    }

    @FXML
    private void setLanguageEs() {
        container.setLanguage("es");
    }

    @FXML
    private void setLanguageEn() {
        container.setLanguage("en");
    }

}
