package com.mycompany.ventacontrolfx.presentation.controller.vat;

import com.mycompany.ventacontrolfx.domain.model.*;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.util.List;

/**
 * Controlador principal de Gestión de IVA y Actualización Masiva de Precios.
 * Orquestador que delega en Managers especializados por pestaña.
 */
public class VatManagementController implements Injectable {

    @FXML private TabPane mainTabPane;
    @FXML private Tab tabCurrentConfig, tabMassUpdate, tabCatalog, tabHistory;

    // --- Pestaña 1: Configuración Actual ---
    @FXML private ComboBox<TaxGroup> cmbGlobalTaxGroup, cmbCategoryTaxGroup;
    @FXML private Label lblCurrentGlobalTaxGroup;
    @FXML private MenuButton cmbCategory;

    // --- Pestaña 2: Subida Masiva ---
    @FXML private ComboBox<PriceList> cmbPriceListUpdate, cmbSourcePriceList;
    @FXML private ComboBox<String> cmbGroupingType, cmbPriceUpdateType;
    @FXML private TextField txtPriceValue, txtPriceReason, txtTopN, txtTopDays, txtBottomN, txtBottomDays, txtSlowDays, txtMinPrice, txtMaxPrice, txtProductSearch;
    @FXML private DatePicker dpPriceStartDate;
    @FXML private Label lblPriceValue, lblSelectedCount;
    @FXML private HBox panelCategory, panelTopSellers, panelBottomSellers, panelSlowMovers, panelPriceRange, panelFavorites, panelClone;
    @FXML private VBox panelProducts;
    @FXML private ComboBox<Category> cmbPriceCategory;
    @FXML private ListView<Product> listSelectedProducts;

    // --- Pestaña 3: Catálogo ---
    @FXML private TableView<TaxRate> taxRatesTable;
    @FXML private TableView<TaxGroup> taxGroupsTable;

    // --- Pestaña 4: Historial ---
    @FXML private TableView<TaxRevision> historyTable;
    @FXML private TableView<PriceUpdateLog> priceLogTable;
    @FXML private ToggleButton tglHistHoy, tglHist7d, tglHist1m, tglHistTodo;

    private ServiceContainer container;
    private VatConfigManager configManager;
    private VatPriceUpdateManager priceUpdateManager;
    private VatCatalogManager catalogManager;
    private VatHistoryManager historyManager;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;

        // 1. Inicializar Managers
        this.configManager = new VatConfigManager(container, 
                container.getTaxManagementUseCase(), 
                container.getCategoryUseCase(), 
                container.getAsyncManager(),
                cmbGlobalTaxGroup, lblCurrentGlobalTaxGroup, null, cmbCategory, cmbCategoryTaxGroup, null);

        this.priceUpdateManager = new VatPriceUpdateManager(container,
                container.getPriceUpdateService(),
                container.getAsyncManager(),
                container.getProductRepository(),
                cmbGroupingType, cmbPriceUpdateType, txtPriceValue, txtPriceReason, dpPriceStartDate,
                cmbPriceListUpdate, lblPriceValue, panelCategory, cmbPriceCategory, panelTopSellers,
                txtTopN, txtTopDays, panelBottomSellers, txtBottomN, txtBottomDays, panelSlowMovers,
                txtSlowDays, panelPriceRange, txtMinPrice, txtMaxPrice, panelProducts, txtProductSearch,
                listSelectedProducts, lblSelectedCount, panelFavorites, panelClone, cmbSourcePriceList);

        this.catalogManager = new VatCatalogManager(container, 
                container.getTaxManagementUseCase(), 
                taxRatesTable, taxGroupsTable);

        this.historyManager = new VatHistoryManager(container,
                container.getTaxManagementUseCase(),
                container.getPriceUpdateLogRepository(),
                historyTable, priceLogTable, null, null);

        // 2. Setup
        setupInitialData();
        priceUpdateManager.init();
        catalogManager.init();
        historyManager.init();
    }

    private void setupInitialData() {
        try {
            List<TaxGroup> groups = container.getTaxEngineService().getAllTaxGroups();
            cmbGlobalTaxGroup.getItems().setAll(groups);
            cmbCategoryTaxGroup.getItems().setAll(groups);
            
            TaxGroup currentDefault = container.getTaxManagementUseCase().getDefaultTaxGroup().orElse(null);
            if (currentDefault != null) {
                lblCurrentGlobalTaxGroup.setText(currentDefault.getName());
                cmbGlobalTaxGroup.getSelectionModel().select(currentDefault);
            }

            List<Category> categories = container.getCategoryUseCase().getAll();
            configManager.setupCategorySelectors(categories);
            if (cmbPriceCategory != null) cmbPriceCategory.getItems().setAll(categories);

            List<PriceList> priceLists = container.getPriceListUseCase().getAll();
            if (cmbPriceListUpdate != null) cmbPriceListUpdate.getItems().setAll(priceLists);
            if (cmbSourcePriceList != null) cmbSourcePriceList.getItems().setAll(priceLists);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Event Handlers (Delegación) ---

    @FXML private void handleUpdateGlobalTaxGroup() { configManager.handleUpdateGlobalTaxGroup(this::setupInitialData); }
    @FXML private void handleUpdateCategoryTaxGroup() { configManager.handleUpdateCategoryTaxGroup(this::setupInitialData); }
    @FXML private void handleUpdateProductTaxGroup() { AlertUtil.showInfo("Info", "Funcionalidad en desarrollo"); }
    @FXML private void handleClearVatSelection() { AlertUtil.showInfo("Info", "Selección limpiada"); }
    
    @FXML private void handleMassivePriceUpdate() { 
        priceUpdateManager.handleMassivePriceUpdate(() -> historyManager.refreshPriceLog()); 
    }

    @FXML private void handleAddTaxRate() { catalogManager.handleAddTaxRate(); }
    @FXML private void handleAddTaxGroup() { catalogManager.handleAddTaxGroup(null); }
    
    @FXML private void handleRefreshHistory() { historyManager.refreshHistory(); }

    @FXML private void handleQuickDateFilter(javafx.event.ActionEvent event) {
        ToggleButton btn = (ToggleButton) event.getSource();
        if (btn == tglHistHoy) historyManager.setFilterDays(1);
        else if (btn == tglHist7d) historyManager.setFilterDays(7);
        else if (btn == tglHist1m) historyManager.setFilterDays(30);
        else historyManager.setFilterDays(null);
    }
}
