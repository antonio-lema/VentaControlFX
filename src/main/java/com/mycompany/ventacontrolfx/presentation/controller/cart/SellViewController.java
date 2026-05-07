package com.mycompany.ventacontrolfx.presentation.controller.cart;

import com.mycompany.ventacontrolfx.application.usecase.ProductFilterUseCase;
import com.mycompany.ventacontrolfx.domain.model.PriceList;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.renderer.CategoryMenuRenderer;
import com.mycompany.ventacontrolfx.presentation.renderer.ProductGridRenderer;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

public class SellViewController implements Injectable, CategoryMenuRenderer.CategorySelectionHandler,
        com.mycompany.ventacontrolfx.presentation.util.Searchable,
        com.mycompany.ventacontrolfx.shared.bus.GlobalEventBus.DataChangeListener {

    @FXML
    private TilePane productsPane;
    @FXML
    private ScrollPane productsScrollPane;
    @FXML
    private ComboBox<PriceList> comboPriceList;
    @FXML
    private VBox categoryMenuContainer;
    @FXML
    private FlowPane categoriesMegaFlowPane;
    @FXML
    private Label lblSelectedCategory, filterLabel;
    @FXML
    private VBox loadingOverlay;

    private ServiceContainer container;
    private ProductFilterUseCase filterUseCase;
    private SellProductManager productManager;
    private SellCategoryManager categoryManager;
    private SellPriceListManager priceListManager;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.filterUseCase = container.createProductFilterUseCase();
        
        com.mycompany.ventacontrolfx.domain.model.SaleConfig config = container.getICompanyConfigRepository().load();
        ProductGridRenderer productRenderer = new ProductGridRenderer(
                productsPane, null, config.getTaxRate(), config.isPricesIncludeTax(),
                p -> productManager.getDiscountDescription(p), container.getBundle(),
                p -> {
                    try { container.getCartUseCase().addItem(p); }
                    catch (IllegalArgumentException ex) { com.mycompany.ventacontrolfx.presentation.util.AlertUtil.showWarning(container.getBundle().getString("alert.validation"), ex.getMessage()); }
                });

        // 1. Inicializar Managers
        this.productManager = new SellProductManager(container, filterUseCase, productRenderer, loadingOverlay, filterLabel, productsScrollPane);
        this.categoryManager = new SellCategoryManager(container, container.getCategoryUseCase(), filterUseCase, categoriesMegaFlowPane, lblSelectedCategory, categoryMenuContainer);
        this.priceListManager = new SellPriceListManager(container, container.getPriceListUseCase(), comboPriceList);

        productManager.init(container.getCartUseCase().getPriceListId());

        // 2. Setup UI
        priceListManager.setup(id -> productManager.refresh(id));
        categoryManager.setup(id -> container.getCartUseCase().setSelectedCategoryId(id), () -> productManager.refresh(container.getCartUseCase().getPriceListId()));

        // 3. Listeners Globales
        container.getCartUseCase().priceListIdProperty().addListener((obs, old, nv) -> {
            if (nv != null) {
                priceListManager.syncSelection(nv.intValue());
                productManager.refresh(nv.intValue());
            }
        });

        container.getCartUseCase().selectedCategoryIdProperty().addListener((obs, old, nv) -> {
            if (nv != null) {
                categoryManager.updateLabel(nv.intValue());
                categoryManager.applyFilterById(nv.intValue(), () -> productManager.refresh(container.getCartUseCase().getPriceListId()));
            }
        });

        productsScrollPane.vvalueProperty().addListener((obs, old, nv) -> {
            if (nv.doubleValue() >= 0.95) productManager.loadNextPage();
        });

        container.getEventBus().subscribe(this);
        
        // Carga inicial
        int initialCatId = container.getCartUseCase().getSelectedCategoryId();
        categoryManager.updateLabel(initialCatId);
        categoryManager.applyFilterById(initialCatId, () -> productManager.refresh(container.getCartUseCase().getPriceListId()));
    }

    @Override
    public void onDataChanged() {
        javafx.application.Platform.runLater(() -> productManager.refresh(container.getCartUseCase().getPriceListId()));
    }

    @Override
    public void onCategorySelected(com.mycompany.ventacontrolfx.domain.model.Category category) {
        categoryManager.applyFilter(category, () -> productManager.refresh(container.getCartUseCase().getPriceListId()));
    }

    @Override
    public void onSpecialFilterSelected(com.mycompany.ventacontrolfx.application.usecase.ProductFilterUseCase.FilterType type) {
        if (type == com.mycompany.ventacontrolfx.application.usecase.ProductFilterUseCase.FilterType.FAVORITES) filterUseCase.applyFavorites();
        else if (type == com.mycompany.ventacontrolfx.application.usecase.ProductFilterUseCase.FilterType.PROMOTIONS) filterUseCase.applyPromotions(productManager.getActivePromotions());
        else filterUseCase.applyAll();
        productManager.refresh(container.getCartUseCase().getPriceListId());
    }

    @Override
    public void handleSearch(String text) {
        if (text == null || text.trim().isEmpty()) filterUseCase.applyAll();
        else filterUseCase.applySearch(text.trim());
        productManager.refresh(container.getCartUseCase().getPriceListId());
    }

    @FXML
    private void handleToggleCategoryMenu() { categoryManager.toggleMenu(); }

    @FXML
    private void handleTestIncident() {
        if (container != null) container.getEventBus().publishVerifactuIncident(java.util.Collections.singletonList(1), java.util.Collections.emptyList());
    }
}


