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
import java.io.IOException;
import java.util.Optional;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import java.util.function.Consumer;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.animation.PauseTransition;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Modality;

import javafx.stage.Stage;

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
    private Label taxLabel;

    @FXML
    private Label itemsCountLabel;

    @FXML
    private Label totalButtonLabel;

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
    private MenuButton menuButton;

    @FXML
    private Button btnSell;
    @FXML
    private Button btnProducts;
    @FXML
    private Button btnProductsList;
    @FXML
    private Button btnCategories;

    @FXML
    private Button payButton;

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
    private ScrollPane categoriesScrollPane;
    @FXML
    private FlowPane categoriesFlowPane;
    @FXML
    private Button btnExpandCategories;

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
    private void handlePayButton() {
        // Button should be disabled if empty, but good to keep a check just in case,
        // OR simply rely on disable state. The user asked for disable, so we can
        // probably remove the alert
        // or just keep it as a fallback if binding fails (unlikely).
        // For cleaner UX, if disabled, this event shouldn't fire.

        if (cartService.getItemCount() == 0) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/payment.fxml"));
            Parent root = loader.load();

            PaymentController controller = loader.getController();
            controller.setTotalAmount(cartService.getGrandTotal(), (paid, change, method) -> {
                // On success, show receipt
                // Capture current cart items before clearing
                List<CartItem> items = new ArrayList<>(cartService.getCartItems());
                double total = cartService.getGrandTotal();

                showReceipt(items, total, paid, change, method,
                        () -> {
                            // After receipt is closed or new sale requested
                            cartService.clear();
                        },
                        () -> {
                            // On Back requested - reopen payment screen
                            Platform.runLater(this::handlePayButton);
                        });
            });

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.UNDECORATED); // Modern look without default OS chrome
            stage.setScene(new Scene(root));

            // Center on screen
            stage.centerOnScreen();

            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error al abrir pantalla de pago");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void showReceipt(List<CartItem> items, double total, double paid, double change, String method,
            Runnable onNewSale, Runnable onBack) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/receipt.fxml"));
            Parent root = loader.load();

            ReceiptController controller = loader.getController();
            controller.setReceiptData(items, total, paid, change, method, onNewSale, onBack);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            // stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            stage.setTitle("Factura simplificada");
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
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
                mainContent, loadingOverlay, cartPanel, categoriesFlowPane,
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

        // Disable Pay Button if Cart is Empty
        if (payButton != null) {
            payButton.disableProperty().bind(cartService.itemCountProperty().isEqualTo(0));
        }

        subtotalLabel.textProperty().bind(cartService.subtotalProperty().asString("%.2f €"));
        taxLabel.textProperty().bind(cartService.taxProperty().asString("%.2f €"));
        totalButtonLabel.textProperty().bind(Bindings.format("Total: %.2f €", cartService.grandTotalProperty()));
        itemsCountLabel.textProperty().bind(Bindings.createStringBinding(
                () -> String.format("Subtotal (%d item%s)", cartService.getItemCount(),
                        cartService.getItemCount() != 1 ? "s" : ""),
                cartService.itemCountProperty()));

        // Setup menu button hover behavior
        setupMenuButtonHover();

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
            categoriesFlowPane.getChildren().clear();

            // "Favoritos" Button - shows only favorite products
            Button btnFavorites = new Button("Favoritos");
            btnFavorites.getStyleClass().add("action-text-btn");
            btnFavorites.setUserData("FAVORITES");
            btnFavorites.setOnAction(e -> filterFavoriteProducts());
            setupCategoryButton(btnFavorites);
            categoriesFlowPane.getChildren().add(btnFavorites);

            // "Todos" (All) Button
            Button btnAll = new Button("Todos");
            btnAll.getStyleClass().add("action-text-btn");
            btnAll.setUserData(null);
            btnAll.setOnAction(e -> filterProducts(null));
            setupCategoryButton(btnAll);
            categoriesFlowPane.getChildren().add(btnAll);

            for (Category cat : favorites) {
                if (cat.isVisible()) {
                    Button btn = new Button(cat.getName());
                    btn.getStyleClass().add("action-text-btn");
                    btn.setUserData(cat);
                    btn.setOnAction(e -> filterProducts(cat));
                    setupCategoryButton(btn);
                    categoriesFlowPane.getChildren().add(btn);

                }
            }

            updateCategoryButtonStyles();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleExpandCategories() {
        if (categoriesScrollPane.getPrefHeight() == 85) {
            // Expand - Bind to content height so ALL content is shown
            categoriesScrollPane.prefHeightProperty().bind(categoriesFlowPane.heightProperty().add(20));
            categoriesScrollPane.setMaxHeight(Double.MAX_VALUE);
            btnExpandCategories.setText("▲");
        } else {
            // Collapse
            collapseCategories();
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

            if (btnData instanceof String && "FAVORITES".equals(btnData)) {
                if (type == FilterType.FAVORITES)
                    isActive = true;
            } else if (btnData == null) {
                if (type == FilterType.ALL)
                    isActive = true;
            } else if (btnData instanceof Category) {
                if (type == FilterType.CATEGORY && criteria != null && criteria.equals(btnData))
                    isActive = true;
            }

            if (!isActive) {
                btnHelperSetStyle(btn, HOVER_CAT_STYLE);
            }
        });
        btn.setOnMouseExited(ev -> {
            updateCategoryButtonStyles();
        });
    }

    private void collapseCategories() {
        // Always unbind first to allow setting prefHeight manually again
        categoriesScrollPane.prefHeightProperty().unbind();

        if (categoriesScrollPane.getPrefHeight() != 85) {
            categoriesScrollPane.setPrefHeight(85);
            categoriesScrollPane.setMaxHeight(85);
            categoriesScrollPane.setVvalue(0); // Scroll to top
            btnExpandCategories.setText("▼");
        }
    }

    // Helper to safely set style if btn is not null
    private void btnHelperSetStyle(Button btn, String style) {
        if (btn != null)
            btn.setStyle(style);
    }

    private void updateCategoryButtonStyles() {
        if (categoriesFlowPane == null)
            return;

        for (Node node : categoriesFlowPane.getChildren()) {
            if (node instanceof Button) {
                Button btn = (Button) node;
                Object btnData = btn.getUserData();

                boolean isActive = false;

                FilterType type = filterService.getCurrentType();
                Object criteria = filterService.getCurrentCriteria();

                if (btnData instanceof String && "FAVORITES".equals(btnData)) {
                    if (type == FilterType.FAVORITES)
                        isActive = true;
                } else if (btnData == null) {
                    if (type == FilterType.ALL)
                        isActive = true;
                } else if (btnData instanceof Category) {
                    // Check equality by ID or object? Assuming equals() works or compare IDs
                    // Previous code used equals(btnData), so we stick to that.
                    if (type == FilterType.CATEGORY && criteria != null && criteria.equals(btnData))
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
        collapseCategories(); // Auto-collapse
        updateCategoryButtonStyles();
        loadProducts(filterService.filter(allProducts));
    }

    private void filterFavoriteProducts() {
        filterService.setFilterFavorites();
        collapseCategories(); // Auto-collapse
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
                                () -> cartService.removeItem(item.getProduct()),
                                (newQty) -> cartService.setQuantity(item.getProduct(), newQty));
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

    private void setupMenuButtonHover() {
        if (menuButton != null) {
            // Open menu on hover - menu will close on click (default behavior)
            menuButton.setOnMouseEntered(e -> {
                if (!menuButton.isShowing()) {
                    menuButton.show();
                }
            });
        }
    }

    @FXML
    private void clearCart() {
        cartService.clear();
    }
}
