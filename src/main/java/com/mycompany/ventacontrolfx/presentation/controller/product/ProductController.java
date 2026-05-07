package com.mycompany.ventacontrolfx.presentation.controller.product;

import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.domain.model.VisibilityFilter;
import com.mycompany.ventacontrolfx.application.usecase.ProductUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.shared.async.AsyncManager;
import com.mycompany.ventacontrolfx.shared.util.ServerPaginationHelper;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.List;

public class ProductController implements Injectable, com.mycompany.ventacontrolfx.presentation.util.Searchable,
        com.mycompany.ventacontrolfx.shared.bus.GlobalEventBus.DataChangeListener {

    @FXML
    private TableView<Product> productsTable;
    @FXML
    private TableColumn<Product, String> colCategoryName, colName, colImage, colSku;
    @FXML
    private TableColumn<Product, Integer> colStock;
    @FXML
    private TableColumn<Product, Double> colPrice;
    @FXML
    private TableColumn<Product, Boolean> colFavorite, colVisible;
    @FXML
    private TableColumn<Product, Double> colIva;
    @FXML
    private TableColumn<Product, Void> colActions;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<Integer> cmbRowLimit;
    @FXML
    private Label lblCount;
    @FXML
    private ToggleButton btnFilterAll, btnFilterVisible, btnFilterDisabled;
    @FXML
    private Pagination pagination;

    private ProductUseCase productUseCase;
    private AsyncManager asyncManager;
    private ServerPaginationHelper<Product> paginationHelper;
    private String currentSearchQuery = "";
    private ToggleGroup filterGroup;

    private ProductTableManager tableManager;
    private ProductActionManager actionManager;

    @Override
    public void inject(ServiceContainer container) {
        this.productUseCase = container.getProductUseCase();
        this.asyncManager = container.getAsyncManager();

        this.tableManager = new ProductTableManager(container, productUseCase, asyncManager);
        this.actionManager = new ProductActionManager(container, productUseCase, asyncManager);

        tableManager.setup(colCategoryName, colName, colSku, colStock, colPrice, colImage, 
                         colFavorite, colVisible, colIva, colActions, 
                         p -> actionManager.openProductDialog(p, () -> paginationHelper.refresh()), 
                         p -> actionManager.handleDeleteProduct(p, () -> paginationHelper.refresh()));

        setupFilterGroup();
        paginationHelper = new ServerPaginationHelper<>(productsTable, cmbRowLimit, lblCount, pagination,
                container.getBundle().getString("products.entity_plural"),
                this::fetchProductsPage, container.getBundle());
        
        searchField.textProperty().addListener((obs, old, nv) -> handleSearch(nv));
        container.getEventBus().subscribe(this);
    }

    private void setupFilterGroup() {
        filterGroup = new ToggleGroup();
        btnFilterAll.setToggleGroup(filterGroup);
        btnFilterVisible.setToggleGroup(filterGroup);
        btnFilterDisabled.setToggleGroup(filterGroup);
        filterGroup.selectedToggleProperty().addListener((obs, old, nv) -> {
            if (nv == null) { old.setSelected(true); return; }
            paginationHelper.refresh();
        });
    }

    @Override
    public void onDataChanged() {
        javafx.application.Platform.runLater(() -> paginationHelper.refresh());
    }

    private void fetchProductsPage(int offset, int limit) {
        VisibilityFilter filter = getSelectedFilter();
        asyncManager.runAsyncTask(() -> {
            int total = productUseCase.getTotalProductCount(currentSearchQuery, filter);
            List<Product> items = productUseCase.getPaginatedProducts(currentSearchQuery, limit, offset, filter);
            return new Object[] { total, items };
        }, (Object res) -> {
            Object[] data = (Object[]) res;
            paginationHelper.applyDataTarget((List<Product>) data[1], (int) data[0]);
        }, null);
    }

    private VisibilityFilter getSelectedFilter() {
        if (filterGroup == null || filterGroup.getSelectedToggle() == null) return VisibilityFilter.VISIBLE;
        if (filterGroup.getSelectedToggle() == btnFilterAll) return VisibilityFilter.ALL;
        if (filterGroup.getSelectedToggle() == btnFilterDisabled) return VisibilityFilter.DISABLED;
        return VisibilityFilter.VISIBLE;
    }

    @Override
    public void handleSearch(String text) {
        this.currentSearchQuery = (text == null) ? "" : text.trim();
        paginationHelper.refresh();
    }

    @FXML
    private void handleImportExcel() {
        actionManager.handleImportCsv(() -> paginationHelper.refresh());
    }

    @FXML
    private void handleAddProduct() {
        actionManager.openProductDialog(null, () -> paginationHelper.refresh());
    }

    @FXML private void handleJumpBack10() { if (paginationHelper != null) paginationHelper.jumpPages(-10); }
    @FXML private void handleJumpForward10() { if (paginationHelper != null) paginationHelper.jumpPages(10); }
}



