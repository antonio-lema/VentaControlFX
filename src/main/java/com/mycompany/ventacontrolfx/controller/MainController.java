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

    private ProductService productService;
    private CategoryService categoryService;
    private List<Product> products = new ArrayList<>();
    private List<Product> allProducts = new ArrayList<>();
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
        simulateLoading(() -> {
            mainContent.setContent(productsPane);
            cartPanel.setVisible(true);
            cartPanel.setManaged(true);
            setActiveButton(btnSell);
            // Reload products and categories to ensure fresh data
            loadProductsFromDB();
            loadFavoriteCategories();
        });
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
        // Load data from DB
        loadProductsFromDB();
        loadFavoriteCategories();

        updateCartState();
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

        List<Product> favorites = new ArrayList<>();
        for (Product p : allProducts) {
            if (p.isFavorite()) {
                favorites.add(p);
            }
        }
        loadProducts(favorites);
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
        // Add to logic list and view
        subtotal += product.getPrice();
        itemCount++;

        createCartItemRow(product);
        updateCartState();
    }

    private void createCartItemRow(Product product) {
        HBox row = new HBox(15);
        row.getStyleClass().add("cart-item");
        row.getStyleClass().add("cart-item-selected"); // Default style
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 10, 10, 0));

        // 1. Indicator
        Rectangle indicator = new Rectangle(4, 40, Color.web("#039be5"));

        // 2. Image (Thumbnail)
        Node imageNode;
        if (product.getImagePath() != null && !product.getImagePath().isEmpty()) {
            File file = new File(product.getImagePath());
            if (file.exists()) {
                ImageView iv = new ImageView(new Image(file.toURI().toString()));
                iv.setFitHeight(40);
                iv.setFitWidth(40);
                iv.setPreserveRatio(true);
                imageNode = iv;
            } else {
                imageNode = new Circle(20, Color.LIGHTGRAY);
            }
        } else {
            imageNode = new Circle(20, Color.LIGHTGRAY);
        }

        // 3. Name
        Label nameLabel = new Label(product.getName());
        nameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #333;");

        // 4. Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // 5. Price
        Label priceLabel = new Label(String.format("%.2f €", product.getPrice()));
        priceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #555;");

        // 6. Delete Button (Trash Can) - Initially Hidden
        Button deleteBtn = new Button("🗑");
        deleteBtn.setStyle(
                "-fx-text-fill: #ff5274; -fx-font-size: 14px; -fx-cursor: hand; -fx-background-color: transparent; -fx-font-weight: bold;");
        deleteBtn.setVisible(false);

        // Logic to remove item
        deleteBtn.setOnAction(e -> {
            cartItemsContainer.getChildren().remove(row);
            subtotal -= product.getPrice();
            itemCount--;
            if (subtotal < 0)
                subtotal = 0; // Precision safety
            updateCartState();
        });

        // Hover listeners
        row.setOnMouseEntered(e -> deleteBtn.setVisible(true));
        row.setOnMouseExited(e -> deleteBtn.setVisible(false));

        row.getChildren().addAll(indicator, imageNode, nameLabel, spacer, priceLabel, deleteBtn);

        cartItemsContainer.getChildren().add(row);
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
