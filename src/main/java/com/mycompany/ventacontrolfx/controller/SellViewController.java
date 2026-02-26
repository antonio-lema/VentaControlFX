package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.application.service.ProductFilterUseCase;
import com.mycompany.ventacontrolfx.model.Category;
import com.mycompany.ventacontrolfx.model.Product;
import com.mycompany.ventacontrolfx.service.ServiceContainer;
import com.mycompany.ventacontrolfx.service.ProductFilterService.FilterType;
import com.mycompany.ventacontrolfx.util.AsyncManager;
import com.mycompany.ventacontrolfx.util.Injectable;
import com.mycompany.ventacontrolfx.util.Searchable;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import java.sql.SQLException;
import java.util.List;

/**
 * Enterprise Sales View Controller.
 * Clean: delegates filtering logic to ProductFilterUseCase.
 */
public class SellViewController implements Searchable, Injectable, CategoryMenuRenderer.CategorySelectionHandler {
    @FXML
    private TilePane productsPane;
    @FXML
    private FlowPane categoriesFlowPane;
    @FXML
    private ScrollPane categoriesScrollPane;
    @FXML
    private Label filterLabel;
    @FXML
    private FontAwesomeIconView expandIcon;
    @FXML
    private VBox loadingOverlay;
    @FXML
    private Button btnExpandCategories;

    private ServiceContainer container;
    private ProductGridRenderer productRenderer;
    private CategoryMenuRenderer categoryRenderer;
    private ProductFilterUseCase filterUseCase;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.filterUseCase = container.getProductFilterUseCase();
        this.productRenderer = new ProductGridRenderer(productsPane, null, container.getCartService()::addItem);
        this.categoryRenderer = new CategoryMenuRenderer(categoriesFlowPane, filterUseCase, this);

        // Hide expand button if categories fit in one row
        categoriesFlowPane.heightProperty().addListener((obs, oldVal, newVal) -> {
            boolean needsScroll = newVal.doubleValue() > 105; // 100 + threshold
            btnExpandCategories.setVisible(needsScroll);
            btnExpandCategories.setManaged(needsScroll);
            if (!needsScroll && categoriesScrollPane.getMaxHeight() > 100) {
                collapseCategories();
            }
        });

        loadInitialData();
    }

    private void loadInitialData() {
        if (loadingOverlay != null)
            loadingOverlay.setVisible(true);

        AsyncManager.execute(container.getProductUseCase().getVisibleProductsTask(), products -> {
            filterUseCase.updateSourceData(products);
            try {
                refreshCategoryMenu();
                onSpecialFilterSelected(FilterType.FAVORITES); // Initial filter
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            if (loadingOverlay != null)
                loadingOverlay.setVisible(false);
        });
    }

    @Override
    public void handleSearch(String text) {
        renderProducts(filterUseCase.applySearch(text));
    }

    @Override
    public void onCategorySelected(Category category) {
        renderProducts(filterUseCase.applyCategory(category));
        collapseCategories();
    }

    @Override
    public void onSpecialFilterSelected(FilterType type) {
        List<Product> products;
        if (type == FilterType.FAVORITES)
            products = filterUseCase.applyFavorites();
        else
            products = filterUseCase.applyAll();

        renderProducts(products);
        collapseCategories();
    }

    private void renderProducts(List<Product> products) {
        updateFilterUI(products);
        productRenderer.render(products);
    }

    private void updateFilterUI(List<Product> products) {
        FilterType type = filterUseCase.getCurrentType();
        Object criteria = filterUseCase.getCurrentCriteria();

        String lbl = "Todos los productos";
        if (type == FilterType.SEARCH)
            lbl = "Búsqueda: \"" + criteria + "\"";
        else if (type == FilterType.FAVORITES)
            lbl = "Favoritos";
        else if (type == FilterType.CATEGORY)
            lbl = ((Category) criteria).getName();

        filterLabel.setText(lbl + " (" + (products != null ? products.size() : 0) + ")");
        categoryRenderer.updateStyles();
    }

    private void refreshCategoryMenu() throws SQLException {
        categoryRenderer.render(container.getCategoryService().getFavoriteCategories());
    }

    @FXML
    private void handleExpandCategories() {
        if (categoriesScrollPane.getPrefHeight() == 100) {
            categoriesScrollPane.prefHeightProperty().bind(categoriesFlowPane.heightProperty().add(20));
            categoriesScrollPane.setMaxHeight(Double.MAX_VALUE);
            if (expandIcon != null)
                expandIcon.setGlyphName("CHEVRON_DOWN");
        } else {
            collapseCategories();
        }
    }

    private void collapseCategories() {
        categoriesScrollPane.prefHeightProperty().unbind();
        categoriesScrollPane.setPrefHeight(100);
        categoriesScrollPane.setMaxHeight(100);
        if (expandIcon != null)
            expandIcon.setGlyphName("CHEVRON_UP");
    }
}
