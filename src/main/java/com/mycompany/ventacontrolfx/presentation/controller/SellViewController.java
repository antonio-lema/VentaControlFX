package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.application.usecase.ProductFilterUseCase;
import com.mycompany.ventacontrolfx.domain.model.VisibilityFilter;
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
import com.mycompany.ventacontrolfx.application.usecase.ProductFilterUseCase.FilterType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;
import javafx.util.StringConverter;
import java.util.List;

public class SellViewController implements Injectable, CategoryMenuRenderer.CategorySelectionHandler,
        com.mycompany.ventacontrolfx.util.Searchable,
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
    private Label lblSelectedCategory;
    @FXML
    private Label filterLabel;
    @FXML
    private VBox loadingOverlay;

    private ServiceContainer container;
    private CategoryUseCase categoryUseCase;
    private PriceListUseCase priceListUseCase;
    private ProductFilterUseCase filterUseCase;
    private ProductGridRenderer productRenderer;

    private int selectedPriceListId = -1;
    private int currentOffset = 0;
    private static final int PAGE_SIZE = 60;
    private int totalProductCount = 0;
    private java.util.List<com.mycompany.ventacontrolfx.domain.model.Promotion> activePromotions = new java.util.ArrayList<>();

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.categoryUseCase = container.getCategoryUseCase();
        this.priceListUseCase = container.getPriceListUseCase();
        this.filterUseCase = container.createProductFilterUseCase();

        com.mycompany.ventacontrolfx.domain.model.SaleConfig config = container.getICompanyConfigRepository().load();
        this.productRenderer = new ProductGridRenderer(
                productsPane,
                null,
                config.getTaxRate(),
                config.isPricesIncludeTax(),
                this::getDiscountDescription,
                container.getBundle(),
                p -> {
                    try {
                        container.getCartUseCase().addItem(p);
                    } catch (IllegalArgumentException ex) {
                        com.mycompany.ventacontrolfx.util.AlertUtil
                                .showWarning(container.getBundle().getString("alert.validation"), ex.getMessage());
                    }
                });

        setupPriceListSelector();
        setupCategorySelector();

        // Listen for price list changes from other sources (like CartUseCase reacting
        // to client changes)
        container.getCartUseCase().priceListIdProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                int newId = newVal.intValue();

                // 1. Sincronizar el ComboBox
                PriceList selectedInCombo = comboPriceList.getSelectionModel().getSelectedItem();
                if (selectedInCombo == null || selectedInCombo.getId() != newId) {
                    for (PriceList pl : comboPriceList.getItems()) {
                        if (pl.getId() == newId) {
                            comboPriceList.getSelectionModel().select(pl);
                            break;
                        }
                    }
                }

                // 2. Sincronizar el estado interno y refrescar si es necesario
                if (selectedPriceListId != newId) {
                    selectedPriceListId = newId;
                    refreshProductsWithNewPriceList();
                }
            }
        });

        // Listen for category changes from Cart
        container.getCartUseCase().selectedCategoryIdProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                int catId = newVal.intValue();
                updateCategoryLabelFromId(catId);
                applyCategoryFilterById(catId);
            }
        });

        container.getEventBus().subscribe(this);

        loadInitialData();
        setupScrollListener();
    }

    private void setupScrollListener() {
        productsScrollPane.vvalueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() >= 0.95 && !loadingOverlay.isVisible()
                    && productsPane.getChildren().size() < totalProductCount) {
                loadNextPage();
            }
        });
    }

    private synchronized void loadNextPage() {
        this.currentOffset += PAGE_SIZE;
        loadingOverlay.setVisible(true);

        container.getAsyncManager().runAsyncTask(() -> {
            FilterType type = filterUseCase.getCurrentType();
            Object criteria = filterUseCase.getCurrentCriteria();
            List<Product> products;

            if (type == FilterType.CATEGORY && criteria != null) {
                products = container.getProductRepository().getByCategoryPaginated(((Category) criteria).getId(),
                        PAGE_SIZE,
                        currentOffset, selectedPriceListId);
            } else if (type == FilterType.SEARCH && criteria != null) {
                products = container.getProductRepository().searchPaginated((String) criteria, PAGE_SIZE, currentOffset,
                        selectedPriceListId, VisibilityFilter.VISIBLE);
            } else if (type == FilterType.FAVORITES) {
                products = container.getProductRepository().getFavoritesPaginated(PAGE_SIZE, currentOffset,
                        selectedPriceListId);
            } else if (type == FilterType.PROMOTIONS) {
                // Optimized paging for promotions
                List<Integer> productIds = activePromotions.stream()
                        .filter(p -> p.getScope() == com.mycompany.ventacontrolfx.domain.model.PromotionScope.PRODUCT)
                        .flatMap(p -> p.getAffectedIds().stream())
                        .collect(java.util.stream.Collectors.toList());

                List<Integer> categoryIds = activePromotions.stream()
                        .filter(p -> p.getScope() == com.mycompany.ventacontrolfx.domain.model.PromotionScope.CATEGORY)
                        .flatMap(p -> p.getAffectedIds().stream())
                        .collect(java.util.stream.Collectors.toList());

                boolean hasGlobal = activePromotions.stream()
                        .anyMatch(p -> p.getScope() == com.mycompany.ventacontrolfx.domain.model.PromotionScope.GLOBAL);

                products = container.getProductRepository().getPromotedPaginated(productIds, categoryIds, hasGlobal,
                        PAGE_SIZE, currentOffset, selectedPriceListId);
            } else {
                products = container.getProductRepository().searchPaginated("", PAGE_SIZE, currentOffset,
                        selectedPriceListId, VisibilityFilter.VISIBLE);
            }
            return products;
        }, products -> {
            productRenderer.append(products);
            loadingOverlay.setVisible(false);
        }, null);
    }

    @Override
    public void onDataChanged() {
        javafx.application.Platform.runLater(this::refreshProductsWithNewPriceList);
    }

    private void loadInitialData() {
        productRenderer.showSkeleton(12);
        this.selectedPriceListId = container.getCartUseCase().getPriceListId();

        // Load Promos in background
        container.getAsyncManager().runAsyncTask(() -> {
            try {
                return container.getPromotionUseCase().getActivePromotions();
            } catch (Exception e) {
                return new java.util.ArrayList<com.mycompany.ventacontrolfx.domain.model.Promotion>();
            }
        }, promos -> {
            this.activePromotions = promos;
            // No refresh needed here, it's started below
        }, null);

        // Load Products in parallel background
        int initialCatId = container.getCartUseCase().getSelectedCategoryId();
        updateCategoryLabelFromId(initialCatId);
        applyCategoryFilterById(initialCatId);
    }

    private void applyCategoryFilterById(int catId) {
        if (catId == -1) {
            filterUseCase.applyAll();
            refreshProductsWithNewPriceList();
        } else if (catId == -2) {
            filterUseCase.applyFavorites();
            refreshProductsWithNewPriceList();
        } else if (catId == -3) {
            filterUseCase.applyPromotions(activePromotions);
            refreshProductsWithNewPriceList();
        } else {
            // Categor\u00eda espec\u00edfica
            container.getAsyncManager().runAsyncTask(() -> categoryUseCase.getAll(), cats -> {
                cats.stream().filter(c -> c.getId() == catId).findFirst().ifPresent(c -> {
                    filterUseCase.applyCategory(c);
                    refreshProductsWithNewPriceList();
                });
            }, null);
        }
    }

    private void setupCategorySelector() {
        if (categoriesMegaFlowPane == null)
            return;

        // Show skeletons for categories
        categoriesMegaFlowPane.getChildren().clear();
        for (int i = 0; i < 6; i++) {
            categoriesMegaFlowPane.getChildren().add(new com.mycompany.ventacontrolfx.component.SkeletonCategoryBox());
        }

        container.getAsyncManager().runAsyncTask(() -> {
            List<Category> cats = categoryUseCase.getAll();
            Category all = new Category();
            all.setName(container.getBundle().getString("sell.all_categories"));
            all.setId(-1);
            Category favs = new Category();
            favs.setName("\u2605 " + container.getBundle().getString("sell.category.favorites"));
            favs.setId(-2);
            Category promos = new Category();
            promos.setName("% " + container.getBundle().getString("sell.category.promotions"));
            promos.setId(-3);

            java.util.List<Category> result = new java.util.ArrayList<>();
            result.add(all);
            result.add(favs);
            result.add(promos);
            result.addAll(cats);
            return result;
        }, categories -> {
            renderCategoryMegaMenu(categories);
            updateCategoryLabelFromId(container.getCartUseCase().getSelectedCategoryId());
        }, null);
    }

    private void renderCategoryMegaMenu(List<Category> categories) {
        categoriesMegaFlowPane.getChildren().clear();
        for (Category cat : categories) {
            Button btn = new Button(translateDynamic(cat.getName()));
            btn.getStyleClass().add("category-mega-button");
            // Set a more compact size for the cards
            btn.setPrefWidth(160);
            btn.setPrefHeight(48);

            btn.setOnAction(e -> {
                if (cat.getId() == -1)
                    onSpecialFilterSelected(FilterType.ALL);
                else if (cat.getId() == -2)
                    onSpecialFilterSelected(FilterType.FAVORITES);
                else if (cat.getId() == -3)
                    onSpecialFilterSelected(FilterType.PROMOTIONS);
                else
                    onCategorySelected(cat);

                // Actualizar estado global y cerrar men\u00fa
                container.getCartUseCase().setSelectedCategoryId(cat.getId());
                handleToggleCategoryMenu();
            });
            categoriesMegaFlowPane.getChildren().add(btn);
        }
    }

    private void updateCategoryLabelFromId(int catId) {
        if (catId == -1)
            lblSelectedCategory.setText(container.getBundle().getString("sell.all_categories"));
        else if (catId == -2)
            lblSelectedCategory.setText("\u2605 " + container.getBundle().getString("sell.category.favorites"));
        else if (catId == -3)
            lblSelectedCategory.setText("% " + container.getBundle().getString("sell.category.promotions"));
        else {
            // Intentar buscar nombre en cache o servicio (aqu\u00ed simplificado)
            container.getAsyncManager().runAsyncTask(() -> categoryUseCase.getAll(), cats -> {
                cats.stream().filter(c -> c.getId() == catId).findFirst()
                        .ifPresent(c -> lblSelectedCategory.setText(translateDynamic(c.getName())));
            }, null);
        }
    }

    @FXML
    private void handleToggleCategoryMenu() {
        boolean isVisible = categoryMenuContainer.isVisible();
        categoryMenuContainer.setVisible(!isVisible);
        categoryMenuContainer.setManaged(!isVisible);
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

            // Determinar la tarifa a seleccionar:
            // 1. La que ya tiene el CartUseCase (ej. por el cliente seleccionado)
            // 2. La marcada como default en la BD
            // 3. La primera de la lista
            int currentId = container.getCartUseCase().getPriceListId();
            PriceList toSelect = null;

            if (currentId > 0) {
                toSelect = priceLists.stream().filter(pl -> pl.getId() == currentId).findFirst().orElse(null);
            }

            if (toSelect == null) {
                toSelect = priceLists.stream().filter(PriceList::isDefault).findFirst()
                        .orElse(!priceLists.isEmpty() ? priceLists.get(0) : null);
            }

            if (toSelect != null) {
                comboPriceList.getSelectionModel().select(toSelect);
                selectedPriceListId = toSelect.getId();
                // Sincronizamos por si acaso ven\u00edamos de un ID <= 0
                if (container.getCartUseCase().getPriceListId() != selectedPriceListId) {
                    container.getCartUseCase().setPriceListId(selectedPriceListId);
                }
            }

            comboPriceList.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    // Si el cambio viene de la UI, lo propagamos al CartUseCase
                    if (newVal.getId() != container.getCartUseCase().getPriceListId()) {
                        container.getCartUseCase().setPriceListId(newVal.getId());
                    }

                    // Si el ID cambi\u00f3 respecto a lo que tenemos renderizado, refrescamos
                    if (newVal.getId() != selectedPriceListId) {
                        selectedPriceListId = newVal.getId();
                        refreshProductsWithNewPriceList();
                    }
                }
            });
        }, null);
    }

    private void refreshProductsWithNewPriceList() {
        this.currentOffset = 0;
        productsScrollPane.setVvalue(0);

        // Show skeleton instead of blocking overlay if possible, or use both
        productRenderer.showSkeleton(12);
        loadingOverlay.setVisible(true);
        loadingOverlay.setStyle("-fx-background-color: transparent;"); // Make it transparent to see skeletons

        container.getAsyncManager().runAsyncTask(() -> {
            FilterType type = filterUseCase.getCurrentType();
            Object criteria = filterUseCase.getCurrentCriteria();
            int count = 0;
            List<Product> products;

            if (type == FilterType.CATEGORY && criteria != null) {
                Category cat = (Category) criteria;
                count = container.getProductRepository().countByCategory(cat.getId(), VisibilityFilter.VISIBLE);
                products = container.getProductRepository().getByCategoryPaginated(cat.getId(), PAGE_SIZE,
                        currentOffset, selectedPriceListId);
            } else if (type == FilterType.FAVORITES) {
                count = container.getProductRepository().countFavorites();
                products = container.getProductRepository().getFavoritesPaginated(PAGE_SIZE, currentOffset,
                        selectedPriceListId);
            } else if (type == FilterType.PROMOTIONS) {
                // Optimized: Filtering on database level instead of memory
                List<Integer> productIds = activePromotions.stream()
                        .filter(p -> p.getScope() == com.mycompany.ventacontrolfx.domain.model.PromotionScope.PRODUCT)
                        .flatMap(p -> p.getAffectedIds().stream())
                        .collect(java.util.stream.Collectors.toList());

                List<Integer> categoryIds = activePromotions.stream()
                        .filter(p -> p.getScope() == com.mycompany.ventacontrolfx.domain.model.PromotionScope.CATEGORY)
                        .flatMap(p -> p.getAffectedIds().stream())
                        .collect(java.util.stream.Collectors.toList());

                boolean hasGlobal = activePromotions.stream()
                        .anyMatch(p -> p.getScope() == com.mycompany.ventacontrolfx.domain.model.PromotionScope.GLOBAL);

                count = container.getProductRepository().countPromoted(productIds, categoryIds, hasGlobal);
                products = container.getProductRepository().getPromotedPaginated(productIds, categoryIds, hasGlobal,
                        PAGE_SIZE, currentOffset, selectedPriceListId);
            } else if (type == FilterType.SEARCH && criteria != null) {
                String query = (String) criteria;
                count = container.getProductRepository().countSearch(query, VisibilityFilter.VISIBLE);
                products = container.getProductRepository().searchPaginated(query, PAGE_SIZE, currentOffset,
                        selectedPriceListId, VisibilityFilter.VISIBLE);
            } else {
                count = container.getProductRepository().countSearch("", VisibilityFilter.VISIBLE);
                products = container.getProductRepository().searchPaginated("", PAGE_SIZE, currentOffset,
                        selectedPriceListId, VisibilityFilter.VISIBLE);
            }

            return new Object[] { products, count };
        }, result -> {
            Object[] data = (Object[]) result;
            List<Product> products = (List<Product>) data[0];
            this.totalProductCount = (Integer) data[1];

            filterUseCase.updateSourceData(products);
            renderProducts(products);
            loadingOverlay.setVisible(false);
        }, null);
    }

    private void renderProducts(List<Product> products) {
        productRenderer.render(products);
        updateFilterUI(products);
    }

    private void updateFilterUI(List<Product> products) {
        FilterType type = filterUseCase.getCurrentType();
        Object criteria = filterUseCase.getCurrentCriteria();
        String lbl = container.getBundle().getString("sell.all_products");
        if (type == FilterType.FAVORITES)
            lbl = container.getBundle().getString("sell.category.favorites");
        else if (type == FilterType.PROMOTIONS)
            lbl = container.getBundle().getString("sell.category.promotions");
        else if (type == FilterType.CATEGORY)
            lbl = translateDynamic(((Category) criteria).getName());

        filterLabel.setText(lbl + " (" + totalProductCount + ")");
    }

    @Override
    public void onCategorySelected(Category category) {
        filterUseCase.applyCategory(category);
        refreshProductsWithNewPriceList();
    }

    @Override
    public void onSpecialFilterSelected(FilterType type) {
        if (type == FilterType.FAVORITES)
            filterUseCase.applyFavorites();
        else if (type == FilterType.PROMOTIONS)
            filterUseCase.applyPromotions(activePromotions);
        else
            filterUseCase.applyAll();

        refreshProductsWithNewPriceList();
    }

    @Override
    public void handleSearch(String text) {
        if (text == null || text.trim().isEmpty()) {
            filterUseCase.applyAll();
            refreshProductsWithNewPriceList();
            return;
        }

        String query = text.trim();
        filterUseCase.applySearch(query);
        refreshProductsWithNewPriceList();
    }

    private String getDiscountDescription(Product p) {
        if (activePromotions == null)
            return null;
        for (com.mycompany.ventacontrolfx.domain.model.Promotion promo : activePromotions) {
            if (promo.getScope() == com.mycompany.ventacontrolfx.domain.model.PromotionScope.GLOBAL) {
                // Si la promo global requiere código (ej. GIFT-), NO la mostramos en cada
                // producto
                if (promo.getCode() != null && !promo.getCode().trim().isEmpty()) {
                    continue;
                }
                return formatPromo(promo);
            }
            if (promo.getScope() == com.mycompany.ventacontrolfx.domain.model.PromotionScope.PRODUCT
                    && promo.getAffectedIds().contains(p.getId())) {
                return formatPromo(promo);
            }
            if (promo.getScope() == com.mycompany.ventacontrolfx.domain.model.PromotionScope.CATEGORY
                    && promo.getAffectedIds().contains(p.getCategoryId())) {
                return formatPromo(promo);
            }
        }
        return null;
    }

    private String formatPromo(com.mycompany.ventacontrolfx.domain.model.Promotion p) {
        if (p.getType() == com.mycompany.ventacontrolfx.domain.model.PromotionType.PERCENTAGE) {
            return String.format("%.0f%% " + container.getBundle().getString("sell.promo.discount_abbr"), p.getValue());
        } else if (p.getType() == com.mycompany.ventacontrolfx.domain.model.PromotionType.FIXED_DISCOUNT) {
            return String.format("%.2f\u20ac " + container.getBundle().getString("sell.promo.discount_abbr"),
                    p.getValue());
        } else if (p.getType() == com.mycompany.ventacontrolfx.domain.model.PromotionType.VOLUME_DISCOUNT) {
            return (p.getBuyQty() + p.getFreeQty()) + "x" + p.getBuyQty();
        }
        return p.getName();
    }

    @FXML
    private void handleTestIncident() {
        if (container != null) {
            container.getEventBus().publishVerifactuIncident(
                java.util.Collections.singletonList(1), 
                java.util.Collections.emptyList()
            );
        }
    }

    private String translateDynamic(String text) {
        if (text == null || text.isBlank())
            return text;
        if (container != null && container.getBundle() != null && container.getBundle().containsKey(text)) {
            return container.getBundle().getString(text);
        }
        return text;
    }
}
