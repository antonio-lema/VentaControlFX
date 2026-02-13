package com.mycompany.ventacontrolfx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.image.ImageView;
import javafx.scene.image.Image;
import javafx.geometry.Pos;
import javafx.geometry.Insets;

import com.mycompany.ventacontrolfx.util.RippleEffect;
import com.mycompany.ventacontrolfx.model.Product;
import com.mycompany.ventacontrolfx.model.Category;
import com.mycompany.ventacontrolfx.model.CartItem;
import com.mycompany.ventacontrolfx.component.ProductBox;
import com.mycompany.ventacontrolfx.component.CartItemRow;
import com.mycompany.ventacontrolfx.service.ProductService;
import com.mycompany.ventacontrolfx.service.CartService;
import com.mycompany.ventacontrolfx.service.NavigationService;
import com.mycompany.ventacontrolfx.service.CategoryService;
import com.mycompany.ventacontrolfx.service.ProductFilterService;
import com.mycompany.ventacontrolfx.service.ProductFilterService.FilterType;
import java.sql.SQLException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.collections.ListChangeListener;
import javafx.animation.PauseTransition;
import javafx.beans.binding.Bindings;
import javafx.util.Duration;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import java.io.IOException;
import javafx.application.Platform;

public class MainController {

    @FXML
    private ScrollPane mainContent;

    @FXML
    private TilePane productsPane;

    @FXML
    private VBox cartItemsContainer;

    @FXML
    private VBox emptyCartView;

    @FXML
    private Label subtotalLabel;

    @FXML
    private Label itemsCountLabel;

    @FXML
    private Label totalButtonLabel;

    @FXML
    private HBox favoriteCategoriesBox;

    @FXML
    private HBox searchBarContainer;

    @FXML
    private HBox filterDisplayContainer;

    @FXML
    private Label filterLabel;

    @FXML
    private TextField searchField;

    private ProductService productService;
    private CategoryService categoryService;
    private CartService cartService;
    private ProductFilterService filterService;
    private NavigationService navigationService;
    private List<Product> products = new ArrayList<>();
    private List<Product> allProducts = new ArrayList<>();

    private static final String ACTIVE_CAT_STYLE = "-fx-background-color: #e3f2fd; -fx-text-fill: #1e88e5; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 5 15; -fx-border-color: transparent;";
    private static final String INACTIVE_CAT_STYLE = "-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 5 15; -fx-text-fill: #555;";
    private static final String HOVER_CAT_STYLE = "-fx-background-color: #f5f5f5; -fx-border-color: #ccc; -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 5 15; -fx-text-fill: #333;";

    @FXML
    private VBox cartPanel;

    @FXML
    private VBox productsSubmenu;

    @FXML
    private Button btnSell;
    @FXML
    private Button btnProducts;
    @FXML
    private Button btnProductsList;
    @FXML
    private Button btnCategories;

    // Other Sidebar Buttons
    @FXML
    private Button btnPanel;
    @FXML
    private Button btnHistory;

    @FXML
    private Button btnClients;

    @FXML
    private Button btnUsers;
    @FXML
    private Button btnConfig;

    @FXML
    private Label lblProductsArrow;
    @FXML
    private VBox loadingOverlay;

    @FXML
    private void showSellView() {
        navigationService.showSellView(() -> {
            loadProductsFromDB();
            loadFavoriteCategories();
        });

        // Setup search functionality (moved to Platform.runLater already in earlier
        // steps, but logical place is here)
        Platform.runLater(() -> {
            if (searchField != null) {
                setupSearchListener();
            } else {
                System.out.println("Warning: searchField is null in showSellView");
            }
        });
    }

    @FXML
    private void toggleProductsMenu() {
        navigationService.toggleProductsMenu();
    }

    @FXML
    private void showProductsView() {
        navigationService.showProductsView(null);
    }

    @FXML
    private void showCategoriesView() {
        navigationService.showCategoriesView(null);
    }

    @FXML
    public void initialize() {
        productService = new ProductService();
        categoryService = new CategoryService();
        cartService = new CartService();
        filterService = new ProductFilterService();
        initCartListener();
        navigationService = new NavigationService(
                mainContent, loadingOverlay, cartPanel, favoriteCategoriesBox,
                searchBarContainer, filterDisplayContainer, productsPane,
                productsSubmenu, lblProductsArrow,
                btnSell, btnProducts, btnProductsList, btnCategories);

        // Apply Ripple Effect to Sidebar Buttons
        if (btnPanel != null)
            RippleEffect.applyTo(btnPanel);
        if (btnSell != null)
            RippleEffect.applyTo(btnSell);
        if (btnProducts != null)
            RippleEffect.applyTo(btnProducts);

        if (btnProductsList != null)
            RippleEffect.applyTo(btnProductsList);
        if (btnCategories != null)
            RippleEffect.applyTo(btnCategories);
        if (btnHistory != null)
            RippleEffect.applyTo(btnHistory);
        if (btnClients != null)
            RippleEffect.applyTo(btnClients);

        if (btnUsers != null)
            RippleEffect.applyTo(btnUsers);
        if (btnConfig != null)
            RippleEffect.applyTo(btnConfig);

        // Load data from DB
        loadProductsFromDB();
        loadFavoriteCategories();

        // Initialize Bindings
        cartItemsContainer.visibleProperty().bind(cartService.itemCountProperty().greaterThan(0));
        cartItemsContainer.managedProperty().bind(cartItemsContainer.visibleProperty());
        emptyCartView.visibleProperty().bind(cartService.itemCountProperty().isEqualTo(0));
        emptyCartView.managedProperty().bind(emptyCartView.visibleProperty());

        subtotalLabel.textProperty().bind(cartService.totalProperty().asString("%.2f €"));
        totalButtonLabel.textProperty().bind(Bindings.format("Total: %.2f €", cartService.totalProperty()));
        itemsCountLabel.textProperty().bind(Bindings.createStringBinding(
                () -> String.format("Subtotal (%d item%s)", cartService.getItemCount(),
                        cartService.getItemCount() != 1 ? "s" : ""),
                cartService.itemCountProperty()));

        // Setup search listener with a delay to ensure all components are loaded
        Platform.runLater(() -> {
            if (searchField != null) {
                setupSearchListener();
            } else {
                System.out.println("Warning: searchField is null in initialize");
            }
        });
    }

    private void loadProductsFromDB() {
        try {
            allProducts = productService.getAllVisibleProducts();
            // Show only favorite products by default on initial load
            filterService.setFilterFavorites();
            loadProducts(filterService.filter(allProducts));

            // Note: filterService.filterFavorites() will also be called if we update
            // buttons?
            // Actually, we need to ensure the UI state (buttons) matches.
            // loadFavoriteCategories() sets up buttons and calls
            // updateCategoryButtonStyles().
            // updateCategoryButtonStyles() relies on filterService state.
            // So setting filterService state here is correct.
        } catch (SQLException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Error al cargar productos: " + e.getMessage());
            alert.show();
        }
    }

    private void loadFavoriteCategories() {
        try {
            List<Category> favorites = categoryService.getFavoriteCategories();
            favoriteCategoriesBox.getChildren().clear();

            // "Favoritos" Button - shows only favorite products (default selected)
            Button btnFavorites = new Button("Favoritos");
            btnFavorites.getStyleClass().add("action-text-btn");
            btnFavorites.setUserData("FAVORITES"); // Special marker for favorites
            btnFavorites.setOnAction(e -> filterFavoriteProducts());
            setupCategoryButton(btnFavorites);
            favoriteCategoriesBox.getChildren().add(btnFavorites);

            // "All" Button
            Button btnAll = new Button("Todos");
            btnAll.getStyleClass().add("action-text-btn");
            btnAll.setUserData(null); // userData null represents "All"
            btnAll.setOnAction(e -> filterProducts(null));
            setupCategoryButton(btnAll);
            favoriteCategoriesBox.getChildren().add(btnAll);

            for (Category cat : favorites) {
                Button btn = new Button(cat.getName());
                btn.getStyleClass().add("action-text-btn");
                btn.setUserData(cat);
                btn.setOnAction(e -> filterProducts(cat));
                setupCategoryButton(btn);
                favoriteCategoriesBox.getChildren().add(btn);
            }

            // Set "Favoritos" as the default selected button
            // Logic moved to loadProductsFromDB or defaults?
            // Actually, loadProductsFromDB sets filterFavorites.

            updateCategoryButtonStyles();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupCategoryButton(Button btn) {
        // Hover effects handling
        btn.setOnMouseEntered(ev -> {
            // Only apply hover style if NOT active
            Object btnData = btn.getUserData();
            boolean isActive = false;

            FilterType type = filterService.getCurrentType();
            Object criteria = filterService.getCurrentCriteria();

            if (type == FilterType.ALL && btnData == null) {
                isActive = true;
            } else if (type == FilterType.FAVORITES && "FAVORITES".equals(btnData)) {
                isActive = true;
            } else if (type == FilterType.CATEGORY && criteria != null && criteria.equals(btnData)) {
                isActive = true;
            }

            if (!isActive) {
                btn.setStyle(HOVER_CAT_STYLE);
            }
        });
        btn.setOnMouseExited(ev -> {
            // Re-apply correct style (Active or Inactive) based on state
            updateCategoryButtonStyles();
        });
    }

    private void updateCategoryButtonStyles() {
        for (Node node : favoriteCategoriesBox.getChildren()) {
            if (node instanceof Button) {
                Button btn = (Button) node;
                Object btnData = btn.getUserData();

                boolean isActive = false;

                FilterType type = filterService.getCurrentType();
                Object criteria = filterService.getCurrentCriteria();

                if (type == FilterType.ALL && btnData == null) {
                    isActive = true;
                } else if (type == FilterType.FAVORITES && "FAVORITES".equals(btnData)) {
                    isActive = true;
                } else if (type == FilterType.CATEGORY && criteria != null && criteria.equals(btnData)) {
                    isActive = true;
                }

                if (isActive) {
                    btn.setStyle(ACTIVE_CAT_STYLE);
                } else {
                    btn.setStyle(INACTIVE_CAT_STYLE);
                }
            }
        }
    }

    private void filterProducts(Category category) {
        if (category == null) {
            filterService.setFilterAll();
            filterLabel.setText("Todos los productos 📦");
        } else {
            filterService.setFilterCategory(category);
            filterLabel.setText(category.getName() + " 🏷️");
        }
        updateCategoryButtonStyles();
        loadProducts(filterService.filter(allProducts));
    }

    private void filterFavoriteProducts() {
        filterService.setFilterFavorites();
        updateCategoryButtonStyles();
        filterLabel.setText("Favoritos ⭐");
        loadProducts(filterService.filter(allProducts));
    }

    private void setupSearchListener() {
        // Avoid adding multiple listeners
        if (searchField == null) {
            System.out.println("ERROR: searchField is null!");
            return;
        }

        if (searchField.getProperties().get("listenerAdded") != null) {
            System.out.println("Search listener already added");
            return;
        }

        System.out.println("Setting up search listener...");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            System.out.println("Search text changed: '" + newValue + "'");
            performSearch(newValue);
        });

        searchField.getProperties().put("listenerAdded", true);
        System.out.println("Search listener configured successfully");
    }

    private void performSearch(String searchText) {
        System.out.println("performSearch called with: '" + searchText + "'");

        filterService.setFilterSearch(searchText);

        // Update filter label
        if (filterService.getCurrentType() == FilterType.SEARCH) {
            filterLabel.setText("Búsqueda: \"" + searchText + "\" 🔍");
        } else {
            // If empty, it restored ALL (logic in service setFilterSearch)
            // Restore label based on type
            if (filterService.getCurrentType() == FilterType.ALL) {
                filterLabel.setText("Todos los productos 📦");
            } else if (filterService.getCurrentType() == FilterType.FAVORITES) {
                filterLabel.setText("Favoritos ⭐");
            }
        }
        updateCategoryButtonStyles();
        loadProducts(filterService.filter(allProducts));
    }

    public void loadProducts(List<Product> newProducts) {
        products.clear();
        products.addAll(newProducts);

        productsPane.getChildren().clear();
        for (Product p : products) {
            ProductBox productBox = new ProductBox(p, this::addToCart);
            productsPane.getChildren().add(productBox);
        }
    }

    private void initCartListener() {
        cartService.getCartItems().addListener((ListChangeListener.Change<? extends CartItem> c) -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (CartItem item : c.getAddedSubList()) {
                        CartItemRow row = new CartItemRow(
                                item,
                                () -> cartService.incrementQuantity(item.getProduct()),
                                () -> cartService.decrementQuantity(item.getProduct()),
                                () -> cartService.removeItem(item.getProduct()));
                        cartItemsContainer.getChildren().add(row);
                    }
                }
                if (c.wasRemoved()) {
                    for (CartItem item : c.getRemoved()) {
                        cartItemsContainer.getChildren().removeIf(node -> node instanceof CartItemRow
                                && ((CartItemRow) node).getCartItem().getProduct().getId() == item.getProduct()
                                        .getId());
                    }
                }
            }

        });

    }

    private void addToCart(Product product) {
        cartService.addItem(product);
    }
}
