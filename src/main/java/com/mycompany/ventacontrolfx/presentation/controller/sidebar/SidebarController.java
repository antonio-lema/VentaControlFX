package com.mycompany.ventacontrolfx.presentation.controller.sidebar;

import com.mycompany.ventacontrolfx.infrastructure.navigation.NavigationService;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;

import com.mycompany.ventacontrolfx.presentation.navigation.ModalService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SidebarController implements Injectable {

    @FXML private Button btnSell, btnProducts, btnCategories, btnHistory, btnReturns, btnClosures, btnBilling, btnClients, btnConfig, btnLock, btnThemeSettings, btnBackup, btnReports, btnClientReport, btnPriceLists, btnVat, btnUsers, btnRoles, btnPromotions, btnWorkSessions, btnOperativeControl, btnStaffCalendar, btnPunctualityAudit;
    @FXML private VBox contentVentas, contentCatalogo, contentPersonal, contentClientes, contentGestion, contentSistema;
    @FXML private Button sectionVentas, sectionCatalogo, sectionPersonal, sectionClientes, sectionGestion, sectionSistema;
    @FXML private Label lblAppName;
    @FXML private ImageView brandLogoImage;

    private NavigationService nav;
    private ServiceContainer container;
    private SidebarSecurityManager securityManager;
    private SidebarBrandingManager brandingManager;
    private SidebarUIManager uiManager;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.nav = container.getNavigationService();
        this.securityManager = new SidebarSecurityManager(container.getAuthService());
        this.brandingManager = new SidebarBrandingManager(container);
        this.uiManager = new SidebarUIManager(
            Arrays.asList(contentVentas, contentCatalogo, contentPersonal, contentClientes, contentGestion, contentSistema),
            Arrays.asList(sectionVentas, sectionCatalogo, sectionPersonal, sectionClientes, sectionGestion, sectionSistema)
        );

        init();
    }

    private void init() {
        brandingManager.applyBranding(lblAppName, brandLogoImage);
        
        List<Button> allButtons = Arrays.asList(btnSell, btnHistory, btnReturns, btnProducts, btnCategories, btnClients, btnReports, btnClientReport, btnClosures, btnBilling, btnConfig, btnThemeSettings, btnBackup, btnLock, btnPriceLists, btnVat, btnUsers, btnRoles, btnPromotions, btnWorkSessions, btnStaffCalendar, btnOperativeControl, btnPunctualityAudit);
        uiManager.applyEffects(allButtons);
        
        applyPermissions();
        uiManager.collapseAll(true);
        setActive(btnSell);
        Platform.runLater(() -> { if (btnSell != null) btnSell.requestFocus(); });
    }

    private void applyPermissions() {
        Map<Button, String> perms = new HashMap<>();
        perms.put(btnHistory, "HISTORIAL|venta.listar"); perms.put(btnReturns, "venta.devolucion");
        perms.put(btnProducts, "PRODUCTOS|producto.listar"); perms.put(btnCategories, "PRODUCTOS|categoria.listar");
        perms.put(btnClients, "CLIENTES|cliente.listar"); perms.put(btnClosures, "CIERRES|caja.historial");
        perms.put(btnConfig, "CONFIGURACION|config.general"); perms.put(btnThemeSettings, "CONFIGURACION|config.estetica");
        perms.put(btnBackup, "admin.backup"); perms.put(btnReports, "reporte.venta|reporte.vendedores");
        perms.put(btnClientReport, "reporte.cliente|HISTORIAL"); perms.put(btnBilling, "fiscal.reenviar|fiscal.config");
        perms.put(btnPriceLists, "admin.precios_masivo|PRODUCTOS"); perms.put(btnVat, "admin.iva");
        perms.put(btnUsers, "usuario.crear"); perms.put(btnRoles, "rol.editar");
        perms.put(btnPromotions, "admin.promociones"); perms.put(btnOperativeControl, "CIERRES|admin.facturacion");
        perms.put(btnPunctualityAudit, "CIERRES");

        Map<Button, VBox> sections = new HashMap<>();
        sections.put(sectionVentas, contentVentas); sections.put(sectionCatalogo, contentCatalogo);
        sections.put(sectionPersonal, contentPersonal); sections.put(sectionClientes, contentClientes);
        sections.put(sectionGestion, contentGestion); sections.put(sectionSistema, contentSistema);

        securityManager.applyPermissions(perms, sections);
    }

    private void setActive(Button b) {
        uiManager.setActiveButton(b, Arrays.asList(btnSell, btnHistory, btnReturns, btnProducts, btnCategories, btnClients, btnReports, btnClientReport, btnClosures, btnBilling, btnConfig, btnThemeSettings, btnBackup, btnLock, btnPriceLists, btnVat, btnUsers, btnRoles, btnPromotions, btnWorkSessions, btnStaffCalendar, btnOperativeControl, btnPunctualityAudit));
    }

    @FXML private void toggleVentas() { uiManager.toggleSection(contentVentas, sectionVentas); }
    @FXML private void toggleCatalogo() { uiManager.toggleSection(contentCatalogo, sectionCatalogo); }
    @FXML private void togglePersonal() { uiManager.toggleSection(contentPersonal, sectionPersonal); }
    @FXML private void toggleClientes() { uiManager.toggleSection(contentClientes, sectionClientes); }
    @FXML private void toggleGestion() { uiManager.toggleSection(contentGestion, sectionGestion); }
    @FXML private void toggleSistema() { uiManager.toggleSection(contentSistema, sectionSistema); }

    @FXML private void showSellView() { setActive(btnSell); nav.navigateTo("/view/cart/sell_view.fxml"); }
    @FXML private void showHistoryView() { setActive(btnHistory); nav.navigateTo("/view/receipt/sales.fxml"); }
    @FXML private void handleShowReturns() { setActive(btnReturns); nav.navigateTo("/view/receipt/return_list.fxml"); }
    @FXML private void handleShowClients() { setActive(btnClients); nav.navigateTo("/view/dialog/clients.fxml"); }
    @FXML private void handleShowReports() { setActive(btnReports); nav.navigateTo("/view/reports/seller_report.fxml"); }
    @FXML private void handleShowClientReport() { setActive(btnClientReport); nav.navigateTo("/view/reports/client_report.fxml"); }
    @FXML private void showProductsView() { setActive(btnProducts); nav.navigateTo("/view/product/products.fxml"); }
    @FXML private void handleShowCategories() { setActive(btnCategories); nav.navigateTo("/view/product/categories.fxml"); }
    @FXML private void handleShowClosures() { setActive(btnClosures); nav.navigateTo("/view/closure/closure_history.fxml"); }
    @FXML private void handleShowBilling() { setActive(btnBilling); nav.navigateTo("/view/receipt/fiscal_documents.fxml"); }
    @FXML private void handleShowPriceLists() { setActive(btnPriceLists); nav.navigateTo("/view/dialog/price_lists.fxml"); }
    @FXML private void handleShowPromotions() { setActive(btnPromotions); nav.navigateTo("/view/dialog/promotions.fxml"); }
    @FXML private void handleShowVat() { setActive(btnVat); nav.navigateTo("/view/vat/vat_management.fxml"); }
    @FXML private void handleShowUsers() { setActive(btnUsers); nav.navigateTo("/view/user/manage_users.fxml"); }
    @FXML private void handleShowRoles() { setActive(btnRoles); nav.navigateTo("/view/user/manage_roles.fxml"); }
    @FXML private void handleShowWorkSessions() { setActive(btnWorkSessions); nav.navigateTo("/view/user/shift_management.fxml"); }
    @FXML private void handleShowOperativeDashboard() { setActive(btnOperativeControl); nav.navigateTo("/view/dashboard/operative_dashboard.fxml"); }
    @FXML private void handleShowPunctualityAudit() { setActive(btnPunctualityAudit); nav.navigateTo("/view/reports/punctuality_audit.fxml"); }
    @FXML private void handleShowConfig() { setActive(btnConfig); container.getAuthService().checkAdminAccess(() -> nav.navigateTo("/view/config/sale_config.fxml")); }
    @FXML private void handleShowCustomization() { setActive(btnThemeSettings); nav.navigateTo("/view/customization/customization_panel.fxml"); }
    @FXML private void handleShowBackup() { setActive(btnBackup); nav.navigateTo("/view/main/backup_view.fxml"); }

    @FXML private void handleShowStaffCalendar() { setActive(btnStaffCalendar); ModalService.showFullScreenModal("/view/user/staff_calendar.fxml", container.getBundle().getString("sidebar.staff_calendar"), container, null); }
    @FXML private void handleLock() { ModalService.showFullScreenModal("/view/auth/lock_screen.fxml", container.getBundle().getString("sidebar.lock_screen"), container, null); }

    @FXML private void setLanguageEs() { container.setLanguage("es"); nav.reloadCurrent(); }
    @FXML private void setLanguageEn() { container.setLanguage("en"); nav.reloadCurrent(); }
}





