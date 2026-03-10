package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.application.usecase.ProductFilterUseCase;
import com.mycompany.ventacontrolfx.application.usecase.ProductUseCase;
import com.mycompany.ventacontrolfx.application.usecase.CategoryUseCase;
import com.mycompany.ventacontrolfx.domain.model.Category;
import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.renderer.CategoryMenuRenderer;
import com.mycompany.ventacontrolfx.presentation.renderer.ProductGridRenderer;
import com.mycompany.ventacontrolfx.application.usecase.PriceListUseCase;
import com.mycompany.ventacontrolfx.domain.model.PriceList;
import com.mycompany.ventacontrolfx.shared.async.AsyncManager;
import com.mycompany.ventacontrolfx.application.usecase.ProductFilterUseCase.FilterType;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.util.List;

public class SellViewController implements Injectable, CategoryMenuRenderer.CategorySelectionHandler,
        com.mycompany.ventacontrolfx.util.Searchable {

    @FXML
    private TilePane productsPane;
    @FXML
    private FlowPane categoriesFlowPane;
    @FXML
    private ScrollPane categoriesScrollPane;
    @FXML
    private Label filterLabel;
    @FXML
    private VBox loadingOverlay;
    @FXML
    private javafx.scene.control.Button btnExpandCategories;
    @FXML
    private de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView expandIcon;
    @FXML
    private ComboBox<PriceList> comboPriceList;

    private ServiceContainer container;
    private ProductUseCase productUseCase;
    private CategoryUseCase categoryUseCase;
    private PriceListUseCase priceListUseCase;
    private ProductFilterUseCase filterUseCase;
    private ProductGridRenderer productRenderer;
    private CategoryMenuRenderer categoryRenderer;

    private int selectedPriceListId = -1;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.productUseCase = container.getProductUseCase();
        this.categoryUseCase = container.getCategoryUseCase();
        this.priceListUseCase = container.getPriceListUseCase();
        this.filterUseCase = container.createProductFilterUseCase();

        com.mycompany.ventacontrolfx.domain.model.SaleConfig config = container.getICompanyConfigRepository().load();
        this.productRenderer = new ProductGridRenderer(
                productsPane,
                null,
                config.getTaxRate(),
                config.isPricesIncludeTax(),
                container.getCartUseCase()::addItem);

        this.categoryRenderer = new CategoryMenuRenderer(categoriesFlowPane, filterUseCase, this);

        setupPriceListSelector();

        // Listen for price list changes from other sources (like CartUseCase reacting
        // to client changes)
        container.getCartUseCase().priceListIdProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && (comboPriceList.getSelectionModel().getSelectedItem() == null ||
                    comboPriceList.getSelectionModel().getSelectedItem().getId() != newVal.intValue())) {

                // Find matching price list in combo and select it
                for (PriceList pl : comboPriceList.getItems()) {
                    if (pl.getId() == newVal.intValue()) {
                        comboPriceList.getSelectionModel().select(pl);
                        break;
                    }
                }
            }
        });

        if (btnExpandCategories != null) {
            btnExpandCategories.setVisible(false);
            btnExpandCategories.setManaged(false);
            categoriesFlowPane.heightProperty().addListener((obs, oldVal, newVal) -> {
                boolean needsExpansion = newVal.doubleValue() > 110;
                btnExpandCategories.setVisible(needsExpansion);
                btnExpandCategories.setManaged(needsExpansion);
            });
        }

        loadInitialData();
    }

    private void loadInitialData() {
        loadingOverlay.setVisible(true);
        container.getAsyncManager().runAsyncTask(() -> productUseCase.getVisibleProducts(selectedPriceListId),
                products -> {
                    filterUseCase.updateSourceData(products);
                    try {
                        categoryRenderer.render(categoryUseCase.getFavorites());
                        onSpecialFilterSelected(FilterType.FAVORITES);

                        // We will rely on the FlowPane height to show/hide the button
                        // It will be handled in the inject() or initialized block,
                        // but we can set up the bindings here or in inject.
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    loadingOverlay.setVisible(false);
                }, null);
    }

    private void setupPriceListSelector() {
        if (comboPriceList == null)
            return;

        comboPriceList.setConverter(new StringConverter<PriceList>() {
            @Override
            public String toString(PriceList object) {
                return object == null ? "" : object.getName();
            }

            @Override
            public PriceList fromString(String string) {
                return null;
            }
        });

        container.getAsyncManager().runAsyncTask(() -> priceListUseCase.getAll(), priceLists -> {
            comboPriceList.getItems().setAll(priceLists);
            PriceList defaultList = priceLists.stream().filter(PriceList::isDefault).findFirst()
                    .orElse(!priceLists.isEmpty() ? priceLists.get(0) : null);
            if (defaultList != null) {
                comboPriceList.getSelectionModel().select(defaultList);
                selectedPriceListId = defaultList.getId();
                container.getCartUseCase().setPriceListId(selectedPriceListId);
            }

            comboPriceList.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && newVal.getId() != selectedPriceListId) {
                    selectedPriceListId = newVal.getId();
                    container.getCartUseCase().setPriceListId(selectedPriceListId);
                    refreshProductsWithNewPriceList();
                }
            });
        }, null);
    }

    private void refreshProductsWithNewPriceList() {
        loadingOverlay.setVisible(true);
        container.getAsyncManager().runAsyncTask(() -> productUseCase.getVisibleProducts(selectedPriceListId),
                products -> {
                    filterUseCase.updateSourceData(products);
                    // Aplicar el filtro actual de nuevo
                    renderProducts(filterUseCase.applyCurrentFilter());
                    loadingOverlay.setVisible(false);
                }, null);
    }

    @Override
    public void onCategorySelected(Category category) {
        renderProducts(filterUseCase.applyCategory(category));
    }

    @Override
    public void onSpecialFilterSelected(FilterType type) {
        if (type == FilterType.FAVORITES)
            renderProducts(filterUseCase.applyFavorites());
        else
            renderProducts(filterUseCase.applyAll());
    }

    private void renderProducts(List<Product> products) {
        productRenderer.render(products);
        updateFilterUI(products);
    }

    @Override
    public void handleSearch(String text) {
        if (text == null || text.trim().isEmpty()) {
            renderProducts(filterUseCase.applyAll());
            return;
        }
        String query = text.toLowerCase().trim();
        container.getAsyncManager().runAsyncTask(() -> productUseCase.getVisibleProducts(selectedPriceListId),
                allProducts -> {
                    List<Product> filtered = allProducts.stream()
                            .filter(p -> p.getName().toLowerCase().contains(query) ||
                                    (p.getCategoryName() != null && p.getCategoryName().toLowerCase().contains(query)))
                            .toList();
                    renderProducts(filtered);
                    filterLabel.setText("Búsqueda: " + text + " (" + filtered.size() + ")");
                }, null);
    }

    private void updateFilterUI(List<Product> products) {
        FilterType type = filterUseCase.getCurrentType();
        Object criteria = filterUseCase.getCurrentCriteria();
        String lbl = "Todos los productos";
        if (type == FilterType.FAVORITES)
            lbl = "Favoritos";
        else if (type == FilterType.CATEGORY)
            lbl = ((Category) criteria).getName();

        filterLabel.setText(lbl + " (" + products.size() + ")");
        categoryRenderer.updateStyles();
    }

    private boolean categoriesExpanded = false;

    @FXML
    private void handleExpandCategories() {
        categoriesExpanded = !categoriesExpanded;
        if (categoriesExpanded) {
            categoriesScrollPane.setPrefHeight(Region.USE_COMPUTED_SIZE);
            categoriesScrollPane.setMaxHeight(Region.USE_COMPUTED_SIZE);
            if (expandIcon != null) {
                expandIcon.setGlyphName("CHEVRON_DOWN");
            }
        } else {
            categoriesScrollPane.setPrefHeight(110);
            categoriesScrollPane.setMaxHeight(110);
            if (expandIcon != null) {
                expandIcon.setGlyphName("CHEVRON_UP");
            }
        }
    }
}
