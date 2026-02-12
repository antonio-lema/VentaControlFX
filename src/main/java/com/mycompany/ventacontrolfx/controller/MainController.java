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
import com.mycompany.ventacontrolfx.service.ProductService;
import com.mycompany.ventacontrolfx.service.CategoryService;
import java.sql.SQLException;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.PauseTransition;
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
    private List<Product> products = new ArrayList<>();
    private List<Product> allProducts = new ArrayList<>();
    private List<CartItem> cartItems = new ArrayList<>();
    private double subtotal = 0.0;
    private int itemCount = 0;
    private Object selectedCategory = null; // Can be Category, String ("FAVORITES"), or null ("Todos")

    private static final String ACTIVE_CAT_STYLE = "-fx-background-color: #e3f2fd; -fx-text-fill: #1e88e5; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 5 15; -fx-border-color: transparent;";
    private static final String INACTIVE_CAT_STYLE = "-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 5 15; -fx-text-fill: #555;";
    private static final String HOVER_CAT_STYLE = "-fx-background-color: #f5f5f5; -fx-border-color: #ccc; -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 5 15; -fx-text-fill: #333;";

    private Node productsView;
    private Node categoriesView;

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
        System.out.println("=== showSellView() called ===");
        System.out.println("searchField is null? " + (searchField == null));

        simulateLoading(() -> {
            mainContent.setContent(productsPane);
            cartPanel.setVisible(true);
            cartPanel.setManaged(true);
            // Show category bar, search, and filter only in sales view
            if (favoriteCategoriesBox != null) {
                favoriteCategoriesBox.setVisible(true);
                favoriteCategoriesBox.setManaged(true);
            }
            if (searchBarContainer != null) {
                searchBarContainer.setVisible(true);
                searchBarContainer.setManaged(true);
            }
            if (filterDisplayContainer != null) {
                filterDisplayContainer.setVisible(true);
                filterDisplayContainer.setManaged(true);
            }
            setActiveButton(btnSell);
            // Reload products and categories to ensure fresh data
            loadProductsFromDB();
            loadFavoriteCategories();
        });

        // Setup search functionality with retry logic
        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                try {
                    Thread.sleep(200); // Wait 200ms between attempts
                    System.out.println("Attempt " + (i + 1) + ": searchField is null? " + (searchField == null));
                    if (searchField != null) {
                        Platform.runLater(() -> {
                            setupSearchListener();
                        });
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (searchField == null) {
                System.out.println("ERROR: searchField is still null after 10 attempts!");
            }
        }).start();
    }

    @FXML
    private void toggleProductsMenu() {
        boolean isVisible = productsSubmenu.isVisible();
        productsSubmenu.setVisible(!isVisible);
        productsSubmenu.setManaged(!isVisible);

        // Update arrow
        if (!isVisible) {
            lblProductsArrow.setText("▼"); // Expanded (Open)
        } else {
            lblProductsArrow.setText("<"); // Collapsed (Closed)
        }

        setActiveButton(btnProducts);
    }

    @FXML
    private void showProductsView() {
        simulateLoading(() -> {
            try {
                // Always reload to get fresh data (or we could just refresh controller)
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/products.fxml"));
                productsView = loader.load();

                mainContent.setContent(productsView);
                cartPanel.setVisible(false);
                cartPanel.setManaged(false);
                // Hide category bar, search, and filter in products view
                if (favoriteCategoriesBox != null) {
                    favoriteCategoriesBox.setVisible(false);
                    favoriteCategoriesBox.setManaged(false);
                }
                if (searchBarContainer != null) {
                    searchBarContainer.setVisible(false);
                    searchBarContainer.setManaged(false);
                }
                if (filterDisplayContainer != null) {
                    filterDisplayContainer.setVisible(false);
                    filterDisplayContainer.setManaged(false);
                }
                setActiveButton(btnProductsList);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void showCategoriesView() {
        simulateLoading(() -> {
            try {
                // Always reload to get fresh data
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/categories.fxml"));
                categoriesView = loader.load();

                mainContent.setContent(categoriesView);
                cartPanel.setVisible(false);
                cartPanel.setManaged(false);
                // Hide category bar, search, and filter in categories view
                if (favoriteCategoriesBox != null) {
                    favoriteCategoriesBox.setVisible(false);
                    favoriteCategoriesBox.setManaged(false);
                }
                if (searchBarContainer != null) {
                    searchBarContainer.setVisible(false);
                    searchBarContainer.setManaged(false);
                }
                if (filterDisplayContainer != null) {
                    filterDisplayContainer.setVisible(false);
                    filterDisplayContainer.setManaged(false);
                }
                setActiveButton(btnCategories);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void simulateLoading(Runnable action) {
        loadingOverlay.setVisible(true);
        PauseTransition pause = new PauseTransition(Duration.millis(300));
        pause.setOnFinished(e -> {
            action.run();
            loadingOverlay.setVisible(false);
        });
        pause.play();
    }

    private void setActiveButton(Button activeButton) {
        // Remove active class from all known buttons
        if (btnSell != null)
            btnSell.getStyleClass().remove("active-sidebar-button");
        if (btnProducts != null)
            btnProducts.getStyleClass().remove("active-sidebar-button");
        if (btnProductsList != null)
            btnProductsList.getStyleClass().remove("active-sidebar-button");
        if (btnCategories != null)
            btnCategories.getStyleClass().remove("active-sidebar-button");

        // Add to the target button
        if (activeButton != null) {
            if (!activeButton.getStyleClass().contains("active-sidebar-button")) {
                activeButton.getStyleClass().add("active-sidebar-button");
            }
        }
    }

    @FXML
    public void initialize() {
        productService = new ProductService();
        categoryService = new CategoryService();

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

        updateCartState();

        // Setup search listener with a delay to ensure all components are loaded
        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                try {
                    Thread.sleep(200);
                    System.out.println(
                            "Initialize attempt " + (i + 1) + ": searchField is null? " + (searchField == null));
                    if (searchField != null) {
                        Platform.runLater(() -> {
                            setupSearchListener();
                        });
                        break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (searchField == null) {
                System.out.println("WARNING: searchField is still null after initialize attempts");
            }
        }).start();
    }

    private void loadProductsFromDB() {
        try {
            allProducts = productService.getAllVisibleProducts();
            // Show only favorite products by default on initial load
            List<Product> favorites = new ArrayList<>();
            for (Product p : allProducts) {
                if (p.isFavorite()) {
                    favorites.add(p);
                }
            }
            loadProducts(favorites);
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
            selectedCategory = "FAVORITES";
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

            if (selectedCategory == null && btnData == null) {
                isActive = true;
            } else if (selectedCategory != null && selectedCategory.equals(btnData)) {
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

                if (selectedCategory == null && btnData == null) {
                    // Both are null (Todos button)
                    isActive = true;
                } else if (selectedCategory != null && selectedCategory.equals(btnData)) {
                    // Direct match (Category or "FAVORITES" string)
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
        selectedCategory = category;
        updateCategoryButtonStyles();

        // Update filter label
        if (category == null) {
            filterLabel.setText("Todos los productos 📦");
        } else {
            filterLabel.setText(category.getName() + " 🏷️");
        }

        List<Product> filtered = new ArrayList<>();
        if (category == null) {
            // When "Todos" button is clicked, show all visible products
            filtered.addAll(allProducts);
        } else {
            // Category selected - show all products in that category
            for (Product p : allProducts) {
                if (p.getCategoryId() == category.getId()) {
                    filtered.add(p);
                }
            }
        }
        loadProducts(filtered);
    }

    private void filterFavoriteProducts() {
        selectedCategory = "FAVORITES";
        updateCategoryButtonStyles();

        // Update filter label
        filterLabel.setText("Favoritos ⭐");

        List<Product> favorites = new ArrayList<>();
        for (Product p : allProducts) {
            if (p.isFavorite()) {
                favorites.add(p);
            }
        }
        loadProducts(favorites);
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
        System.out.println("allProducts size: " + allProducts.size());

        if (searchText == null || searchText.trim().isEmpty()) {
            System.out.println("Search text is empty, restoring filter");
            // If search is empty, restore current filter (favorites, category, or all)
            if ("FAVORITES".equals(selectedCategory)) {
                filterFavoriteProducts();
            } else if (selectedCategory == null) {
                filterProducts(null); // Show all
            } else if (selectedCategory instanceof Category) {
                filterProducts((Category) selectedCategory);
            }
            return;
        }

        List<Product> searchResults = new ArrayList<>();
        String query = searchText.toLowerCase();

        for (Product p : allProducts) {
            boolean matches = false;

            // Search by Name
            if (p.getName() != null && p.getName().toLowerCase().contains(query)) {
                matches = true;
                System.out.println("Match found by name: " + p.getName());
            }
            // Search by ID (Code)
            else if (String.valueOf(p.getId()).contains(query)) {
                matches = true;
                System.out.println("Match found by ID: " + p.getId());
            }
            // Search by Category Name
            else if (p.getCategoryName() != null && p.getCategoryName().toLowerCase().contains(query)) {
                matches = true;
                System.out.println("Match found by category: " + p.getCategoryName());
            }

            if (matches) {
                searchResults.add(p);
            }
        }

        System.out.println("Search results: " + searchResults.size() + " products found");

        // Update filter label
        filterLabel.setText("Búsqueda: \"" + searchText + "\" 🔍");

        // Load search results
        loadProducts(searchResults);
    }

    public void loadProducts(List<Product> newProducts) {
        products.clear();
        products.addAll(newProducts);

        productsPane.getChildren().clear();
        for (Product p : products) {
            VBox productBox = createProductBox(p);
            productsPane.getChildren().add(productBox);
        }
    }

    private VBox createProductBox(Product product) {
        VBox box = new VBox();
        box.getStyleClass().add("product-box");
        box.setPrefWidth(180);
        // box.setPrefHeight(220); // Let content determine height or fixed

        // Image Container
        StackPane imageContainer = new StackPane();
        imageContainer.getStyleClass().add("product-image-container");
        imageContainer.setPrefHeight(140);
        imageContainer.setMinHeight(140);
        imageContainer.setMaxHeight(140);
        imageContainer.setAlignment(Pos.CENTER);

        // Clip for top corners
        Rectangle clip = new Rectangle(180, 140);
        clip.setArcWidth(15); // Match CSS radius roughly
        clip.setArcHeight(15);
        imageContainer.setClip(clip);

        // Image Logic
        if (product.getImagePath() != null && !product.getImagePath().isEmpty()) {
            File file = new File(product.getImagePath());
            if (file.exists()) {
                ImageView imageView = new ImageView(new Image(file.toURI().toString()));
                imageView.setFitHeight(140);
                imageView.setFitWidth(180);
                imageView.setPreserveRatio(true); // Crop approach might be better for uniform cards
                // To fill the area, we might want visible true?
                // For "tal cual esta" (as is), user showed an image that fills the width.
                // Let's try to fill width/height

                // If we want cover behavior:
                // imageView.setPreserveRatio(true);
                // But if ratio differs, we have empty space.
                // The sample image shows full width fill.
                // Let's use a centered crop if possible or just fit center.

                imageContainer.getChildren().add(imageView);
            } else {
                // Placeholder
                Label placeholder = new Label("No Image");
                placeholder.setTextFill(Color.GRAY);
                imageContainer.getChildren().add(placeholder);
            }
        } else {
            Label placeholder = new Label("No Image");
            placeholder.setTextFill(Color.GRAY);
            imageContainer.getChildren().add(placeholder);
        }

        // Info Container
        VBox infoBox = new VBox(2); // Reduced spacing
        infoBox.getStyleClass().add("product-info");

        Label nameLabel = new Label(product.getName());
        nameLabel.getStyleClass().add("product-name");
        nameLabel.setWrapText(true);
        nameLabel.setMaxHeight(40); // Limit height for 2 lines approx
        nameLabel.setAlignment(Pos.TOP_LEFT);

        Label priceLabel = new Label(String.format("%.2f €", product.getPrice()));
        priceLabel.getStyleClass().add("product-price");

        infoBox.getChildren().addAll(nameLabel, priceLabel);

        box.getChildren().addAll(imageContainer, infoBox);

        box.setOnMouseClicked((MouseEvent e) -> addToCart(product));

        return box;
    }

    private void addToCart(Product product) {
        // Check if product is already in cart
        boolean found = false;
        for (Node node : cartItemsContainer.getChildren()) {
            if (node.getUserData() instanceof Object[]) {
                Object[] data = (Object[]) node.getUserData();
                if (data.length >= 2 && data[0] instanceof Product && data[1] instanceof int[]) {
                    Product existingProduct = (Product) data[0];
                    if (existingProduct.getId() == product.getId()) {
                        // Increment quantity
                        int[] quantity = (int[]) data[1];
                        quantity[0]++;

                        // Update UI label
                        if (node instanceof HBox) {
                            HBox row = (HBox) node;
                            // Find quantity label (it's inside the second child, which is a VBox)
                            if (row.getChildren().size() > 1 && row.getChildren().get(1) instanceof VBox) {
                                VBox infoBox = (VBox) row.getChildren().get(1);
                                if (infoBox.getChildren().size() > 1 && infoBox.getChildren().get(1) instanceof HBox) {
                                    HBox sensitiveBox = (HBox) infoBox.getChildren().get(1); // Quantity box
                                    if (sensitiveBox.getChildren().size() > 1
                                            && sensitiveBox.getChildren().get(1) instanceof Label) {
                                        Label quantityLabel = (Label) sensitiveBox.getChildren().get(1);
                                        quantityLabel.setText(String.valueOf(quantity[0]));
                                    }
                                }
                            }
                        }
                        found = true;
                        break;
                    }
                }
            }
        }

        if (!found) {
            createCartItemRow(product);
        }

        updateCartTotals();
    }

    private void createCartItemRow(Product product) {
        // Main container - Increased padding for bigger feel
        HBox mainRow = new HBox();
        mainRow.getStyleClass().add("cart-item");
        mainRow.setAlignment(Pos.CENTER_LEFT);
        mainRow.setPadding(new Insets(15)); // Increased from 10
        mainRow.setSpacing(15); // Add spacing between elements

        // 1. Circular Product Image (BIGGER)
        Node imageNode;
        if (product.getImagePath() != null && !product.getImagePath().isEmpty()) {
            File file = new File(product.getImagePath());
            if (file.exists()) {
                ImageView iv = new ImageView(new Image(file.toURI().toString()));
                iv.setFitHeight(70); // Increased from 50
                iv.setFitWidth(70); // Increased from 50
                iv.setPreserveRatio(false);

                // Create circular clip
                Circle clip = new Circle(35, 35, 35); // Increased radius to 35
                iv.setClip(clip);
                iv.getStyleClass().add("cart-product-image");
                imageNode = iv;
            } else {
                Circle placeholder = new Circle(35, Color.LIGHTGRAY); // Increased radius to 35
                placeholder.getStyleClass().add("cart-product-image");
                imageNode = placeholder;
            }
        } else {
            Circle placeholder = new Circle(35, Color.LIGHTGRAY); // Increased radius to 35
            placeholder.getStyleClass().add("cart-product-image");
            imageNode = placeholder;
        }

        // 2. Product Info (Name + Quantity Controls)
        VBox infoBox = new VBox(8); // Increased spacing
        infoBox.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(product.getName());
        nameLabel.getStyleClass().add("cart-product-name");
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;"); // Directly increase font size

        // Quantity controls - CENTERED
        HBox quantityBox = new HBox(12); // Increased spacing
        quantityBox.setAlignment(Pos.CENTER_LEFT);

        Button decreaseBtn = new Button("-");
        decreaseBtn.getStyleClass().add("quantity-btn");
        decreaseBtn.setStyle("-fx-min-width: 30px; -fx-min-height: 30px; -fx-font-size: 14px;"); // Bigger buttons

        Label quantityLabel = new Label("1");
        quantityLabel.getStyleClass().add("quantity-label");
        quantityLabel
                .setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-min-width: 20px; -fx-alignment: center;"); // Bigger
                                                                                                                      // label

        Button increaseBtn = new Button("+");
        increaseBtn.getStyleClass().add("quantity-btn");
        increaseBtn.setStyle("-fx-min-width: 30px; -fx-min-height: 30px; -fx-font-size: 14px;"); // Bigger buttons

        // Quantity button actions
        final int[] quantity = { 1 };
        decreaseBtn.setOnAction(e -> {
            if (quantity[0] > 1) {
                quantity[0]--;
                quantityLabel.setText(String.valueOf(quantity[0]));
                updateCartTotals();
            }
        });

        increaseBtn.setOnAction(e -> {
            quantity[0]++;
            quantityLabel.setText(String.valueOf(quantity[0]));
            updateCartTotals();
        });

        quantityBox.getChildren().addAll(decreaseBtn, quantityLabel, increaseBtn);
        infoBox.getChildren().addAll(nameLabel, quantityBox);

        // 3. Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 4. Right side with price and delete button (this part moves)
        StackPane rightSide = new StackPane();
        rightSide.setMinWidth(120); // Increased width

        // Price label (will slide left on hover)
        Label priceLabel = new Label(String.format("%.2f €", product.getPrice()));
        priceLabel.getStyleClass().add("cart-product-price");
        priceLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;"); // Bigger price
        StackPane.setAlignment(priceLabel, Pos.CENTER_RIGHT);

        // Delete Button (hidden behind price)
        Button deleteBtn = new Button("🗑");
        deleteBtn.getStyleClass().add("cart-delete-btn");
        deleteBtn.setStyle("-fx-font-size: 16px; -fx-padding: 8 12;"); // Bigger delete button
        StackPane.setAlignment(deleteBtn, Pos.CENTER_RIGHT);
        deleteBtn.setOpacity(0);

        // Delete action
        deleteBtn.setOnAction(e -> {
            mainRow.setVisible(false);
            mainRow.setManaged(false);
            cartItemsContainer.getChildren().remove(mainRow);
            updateCartTotals();
        });

        rightSide.getChildren().addAll(deleteBtn, priceLabel);

        // Add all elements to main row
        mainRow.getChildren().addAll(imageNode, infoBox, spacer, rightSide);

        // Hover effect: only move price label to reveal delete button
        mainRow.setOnMouseEntered(e -> {
            priceLabel.setTranslateX(-50);
            deleteBtn.setOpacity(1);
        });

        mainRow.setOnMouseExited(e -> {
            priceLabel.setTranslateX(0);
            deleteBtn.setOpacity(0);
        });

        // Store product and quantity for total calculation
        mainRow.setUserData(new Object[] { product, quantity });

        cartItemsContainer.getChildren().add(mainRow);
    }

    private void updateCartTotals() {
        // Recalculate totals from all cart items
        subtotal = 0;
        itemCount = 0;

        for (Node node : cartItemsContainer.getChildren()) {
            if (node.getUserData() instanceof Object[]) {
                Object[] data = (Object[]) node.getUserData();
                if (data.length >= 2 && data[0] instanceof Product && data[1] instanceof int[]) {
                    Product product = (Product) data[0];
                    int[] quantity = (int[]) data[1];

                    subtotal += product.getPrice() * quantity[0];
                    itemCount += quantity[0];
                }
            }
        }

        updateCartState();
    }

    private void updateCartState() {
        if (itemCount > 0) {
            cartItemsContainer.setVisible(true);
            emptyCartView.setVisible(false);
        } else {
            cartItemsContainer.setVisible(false);
            emptyCartView.setVisible(true);
        }

        String totalText = String.format("%.2f €", subtotal);
        subtotalLabel.setText(totalText);
        totalButtonLabel.setText(totalText);
        itemsCountLabel.setText("Subtotal (" + itemCount + " item" + (itemCount != 1 ? "s" : "") + ")");
    }
}
