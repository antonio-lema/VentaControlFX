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
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;

import com.mycompany.ventacontrolfx.util.AlertUtil;
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
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import com.mycompany.ventacontrolfx.service.SaleService;
import com.mycompany.ventacontrolfx.service.UserService;
import com.mycompany.ventacontrolfx.service.CashClosureService;
import com.mycompany.ventacontrolfx.service.ClientService;

public class MainController {

    private ProductService productService;
    private CategoryService categoryService;
    private CartService cartService;
    private ProductFilterService filterService;
    private SaleService saleService;
    private NavigationService navigationService;

    // Static instance to allow other controllers to update badges
    private static MainController instance;
    private UserService userService;
    private CashClosureService closureService;
    private ClientService clientService;
    private FilterType currentFilterType = FilterType.ALL;

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

    @FXML
    private FontAwesomeIconView expandIcon;

    @FXML
    private Label lblSelectedClient;
    @FXML
    private Button btnRemoveClient;

    @FXML
    private Label labelCountProducts;
    @FXML
    private Label labelCountCategories;
    @FXML
    private Label labelCountHistory;
    @FXML
    private Label labelCountClosures;
    @FXML
    private Label labelCountClients;
    @FXML
    private Label labelCountUsers;

    @FXML
    private HBox cardCountProducts;
    @FXML
    private HBox cardCountCategories;
    @FXML
    private HBox cardCountHistory;
    @FXML
    private HBox cardCountClosures;
    @FXML
    private HBox cardCountClients;
    @FXML
    private HBox cardCountUsers;

    private List<Product> products = new ArrayList<>();
    private List<Product> allProducts = new ArrayList<>();

    @FXML
    private VBox cartPanel;

    @FXML
    private VBox productsSubmenu;

    @FXML
    private MenuButton userMenuButton;

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

    @FXML
    private Button btnClearCart;

    // Other Sidebar Buttons

    @FXML
    private Button btnHistory;

    @FXML
    private Button btnClients;

    @FXML
    private Button btnUsers;
    @FXML
    private Button btnConfig;

    @FXML
    private Button btnLock;

    @FXML
    private Button btnClosures;

    @FXML
    private Label lblProductsArrow;
    @FXML
    private VBox loadingOverlay;
    @FXML
    private ImageView imgLogo;
    @FXML
    private HBox logoTextContainer;
    @FXML
    private Label lblAppPrefix;
    @FXML
    private Label lblAppName;

    @FXML
    private HBox favoriteCategoriesBox;
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
            }
        });
    }

    @FXML
    private void showHistoryView() {
        navigationService.showHistoryView(null);
    }

    @FXML
    private void handleShowClosures() {
        navigationService.showClosureHistoryView(null);
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
                // On success, save to DB and show receipt
                List<CartItem> items = new ArrayList<>(cartService.getCartItems());
                double total = cartService.getGrandTotal();
                com.mycompany.ventacontrolfx.model.Client client = cartService.getSelectedClient();
                Integer clientId = client != null ? client.getId() : null;

                try {
                    int saleId = saleService.saveSale(items, total, method, clientId);
                    Platform.runLater(() -> cartService.clear()); // Vaciar inmediatamente

                    showReceipt(items, total, paid, change, method, client, saleId,
                            () -> {
                                // cartService.clear() ya llamado
                            },
                            () -> {
                                // Platform.runLater(this::handlePayButton); ya no aplica porque se guardó la
                                // venta
                            });
                } catch (SQLException e) {
                    AlertUtil.showError("No se pudo guardar la venta",
                            "Ocurrió un error al guardar la venta en la base de datos: " + e.getMessage());
                }
            });

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initStyle(javafx.stage.StageStyle.UNDECORATED); // Modern look without default OS chrome
            stage.setScene(new Scene(root));

            // Center on screen
            stage.centerOnScreen();

            stage.showAndWait();

        } catch (IOException e) {
            AlertUtil.showError("Error al abrir pantalla de pago", e.getMessage());
        }
    }

    private void showReceipt(List<CartItem> items, double total, double paid, double change, String method,
            com.mycompany.ventacontrolfx.model.Client client, int saleId,
            Runnable onNewSale, Runnable onBack) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/receipt.fxml"));
            Parent root = loader.load();

            ReceiptController controller = loader.getController();
            // If client is present, we MUST set it BEFORE setReceiptData so item rows know
            // the format
            if (client != null) {
                controller.setClientInfo(client);
            }
            controller.setReceiptData(items, total, paid, change, method, saleId, onNewSale, onBack);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            // stage.initStyle(javafx.stage.StageStyle.UNDECORATED);
            stage.setTitle(client != null ? "Factura" : "Factura simplificada");
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

    public void initialize() {
        instance = this;
        productService = new ProductService();
        categoryService = new CategoryService();
        cartService = new CartService();
        filterService = new ProductFilterService();
        saleService = new SaleService();
        userService = new UserService();
        closureService = new CashClosureService();
        clientService = new ClientService();
        loadLogo();
        initCartListener();
        initClientSelectionListener();
        refreshCounts();
        navigationService = new NavigationService(
                mainContent, loadingOverlay, cartPanel, favoriteCategoriesBox,
                searchBarContainer, filterDisplayContainer, productsPane,
                productsSubmenu, lblProductsArrow,
                btnSell, btnProducts, btnProductsList, btnCategories, btnHistory, btnUsers, btnClients, btnConfig,
                btnClosures,
                cartService,
                cardCountProducts, cardCountCategories, cardCountHistory,
                cardCountClosures, cardCountClients, cardCountUsers);

        // Asegurar que el contador de productos sea visible al iniciar
        if (navigationService != null) {
            navigationService.setActiveButton(btnSell);
            // Hacer visible el card del contador de productos manualmente al inicio
            if (cardCountProducts != null) {
                cardCountProducts.setVisible(true);
                cardCountProducts.setManaged(true);
            }
        }

        // Apply Ripple Effect to Sidebar Buttons

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
        if (btnClosures != null)
            RippleEffect.applyTo(btnClosures);
        if (btnLock != null)
            RippleEffect.applyTo(btnLock);

        // Load data from DB
        loadProductsFromDB();
        loadFavoriteCategories();

        // Initialize Bindings
        cartItemsContainer.visibleProperty().bind(cartService.itemCountProperty().greaterThan(0));
        cartItemsContainer.managedProperty().bind(cartItemsContainer.visibleProperty());
        emptyCartView.visibleProperty().bind(cartService.itemCountProperty().isEqualTo(0));
        emptyCartView.managedProperty().bind(emptyCartView.visibleProperty());

        // Hide elements if Cart is Empty
        if (payButton != null) {
            payButton.disableProperty().bind(cartService.itemCountProperty().isEqualTo(0));
            payButton.visibleProperty().bind(cartService.itemCountProperty().greaterThan(0));
            payButton.managedProperty().bind(payButton.visibleProperty());
        }
        if (btnClearCart != null) {
            btnClearCart.visibleProperty().bind(cartService.itemCountProperty().greaterThan(0));
            btnClearCart.managedProperty().bind(btnClearCart.visibleProperty());
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
        // Setup search listener with a delay to ensure all components are loaded
        Platform.runLater(() -> {
            if (searchField != null) {
                setupSearchListener();
            }
        });

        // Check User Role for UI adjustments
        checkAdminRole();
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
            AlertUtil.showError("Error", "Error al cargar productos: " + e.getMessage());
        }
    }

    private void loadFavoriteCategories() {
        try {
            List<Category> favorites = categoryService.getFavoriteCategories();
            categoriesFlowPane.getChildren().clear();

            // "Favoritos" Button - shows only favorite products
            Button btnFavorites = new Button("Favoritos");
            btnFavorites.getStyleClass().add("category-btn");
            btnFavorites.setUserData("FAVORITES");
            btnFavorites.setOnAction(e -> filterFavoriteProducts());
            setupCategoryButton(btnFavorites);
            categoriesFlowPane.getChildren().add(btnFavorites);

            // "Todos" (All) Button
            Button btnAll = new Button("Todos");
            btnAll.getStyleClass().add("category-btn");
            btnAll.setUserData(null);
            btnAll.setOnAction(e -> filterProducts(null));
            setupCategoryButton(btnAll);
            categoriesFlowPane.getChildren().add(btnAll);

            for (Category cat : favorites) {
                if (cat.isVisible()) {
                    Button btn = new Button(cat.getName());
                    btn.getStyleClass().add("category-btn");
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
        if (categoriesScrollPane.getPrefHeight() == 100) {
            // Expand - Bind to content height so ALL content is shown
            categoriesScrollPane.prefHeightProperty().bind(categoriesFlowPane.heightProperty().add(20));
            categoriesScrollPane.setMaxHeight(Double.MAX_VALUE);
            if (expandIcon != null)
                expandIcon.setGlyphName("CHEVRON_DOWN");
        } else {
            // Collapse
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

    private void setupCategoryButton(Button btn) {
        // Hover effects are handled via CSS :hover
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
                    if (!btn.getStyleClass().contains("active-category-btn")) {
                        btn.getStyleClass().add("active-category-btn");
                    }
                } else {
                    btn.getStyleClass().remove("active-category-btn");
                }
            }
        }
    }

    private void filterProducts(Category category) {
        if (category == null) {
            filterService.setFilterAll();
            filterLabel.setText("Todos los productos");
        } else {
            filterService.setFilterCategory(category);
            filterLabel.setText(category.getName());
        }
        collapseCategories(); // Auto-collapse
        updateCategoryButtonStyles();
        loadProducts(filterService.filter(allProducts));
    }

    private void filterFavoriteProducts() {
        filterService.setFilterFavorites();
        collapseCategories(); // Auto-collapse
        updateCategoryButtonStyles();
        filterLabel.setText("Favoritos");
        loadProducts(filterService.filter(allProducts));
    }

    private void setupSearchListener() {
        if (searchField == null || searchField.getProperties().get("listenerAdded") != null) {
            return;
        }

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            performSearch(newValue);
        });

        searchField.getProperties().put("listenerAdded", true);
    }

    private void performSearch(String searchText) {

        filterService.setFilterSearch(searchText);

        // Update filter label
        if (filterService.getCurrentType() == FilterType.SEARCH) {
            filterLabel.setText("Búsqueda: \"" + searchText + "\"");
        } else {
            // If empty, it restored ALL (logic in service setFilterSearch)
            // Restore label based on type
            if (filterService.getCurrentType() == FilterType.ALL) {
                filterLabel.setText("Todos los productos");
            } else if (filterService.getCurrentType() == FilterType.FAVORITES) {
                filterLabel.setText("Favoritos");
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

        // Actualizar el contador de productos mostrados basándonos en el filtro
        if (labelCountProducts != null) {
            labelCountProducts.setText(String.valueOf(products.size()));
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
        if (userMenuButton != null) {
            // Open menu on hover - menu will close on click (default behavior)
            userMenuButton.setOnMouseEntered(e -> {
                if (!userMenuButton.isShowing()) {
                    userMenuButton.show();
                }
            });
        }
    }

    @FXML
    private void clearCart() {
        cartService.clear();
    }

    @FXML
    private void handleCashClosure() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/cash_closure.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Cierre de Caja");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showAddClientDialog() {
        if (navigationService != null) {
            // Navigate to Clients View so user can select or add a client
            navigationService.showClientsView(null);
        }
    }

    private void initClientSelectionListener() {
        if (cartService != null && lblSelectedClient != null) {
            cartService.selectedClientProperty().addListener((obs, oldClient, newClient) -> {
                if (newClient != null) {
                    lblSelectedClient.setText(newClient.getName());
                    lblSelectedClient.setStyle("-fx-text-fill: #1e88e5; -fx-font-weight: bold;");
                    btnRemoveClient.setVisible(true);
                    btnRemoveClient.setManaged(true);
                } else {
                    lblSelectedClient.setText("Añadir empresa");
                    lblSelectedClient.setStyle("");
                    btnRemoveClient.setVisible(false);
                    btnRemoveClient.setManaged(false);
                }
            });
        }
    }

    @FXML
    private void handleRemoveSelectedClient() {
        if (cartService != null) {
            cartService.setSelectedClient(null);
        }
    }

    @FXML
    private void handleLogout() {
        boolean result = AlertUtil.showConfirmation("Cerrar Sesión", "¿Estás seguro de que quieres salir?",
                "Se cerrará la sesión actual.");
        if (result) {
            // Clear session
            com.mycompany.ventacontrolfx.util.UserSession.getInstance().logout();

            // Switch to Login View using SceneNavigator
            Stage stage = (Stage) mainContent.getScene().getWindow();
            com.mycompany.ventacontrolfx.util.SceneNavigator.loadScene(
                    stage,
                    "/view/login.fxml",
                    "Login - TPV Bazar Electrónico",
                    900,
                    600);
            stage.setMaximized(false); // Login window shouldn't be maximized usually
        }
    }

    @FXML
    private void handleShowConfig() {
        // Verificar rol de administrador por seguridad
        com.mycompany.ventacontrolfx.model.User currentUser = com.mycompany.ventacontrolfx.util.UserSession
                .getInstance().getCurrentUser();
        if (currentUser == null || !"admin".equalsIgnoreCase(currentUser.getRole())) {
            AlertUtil.showError("Acceso Denegado", "No tienes permisos para acceder a la configuración.");
            return;
        }

        if (navigationService != null) {
            navigationService.showConfigView(null);
        }
    }

    @FXML
    private void handleShowUsers() {
        // Verificar rol de administrador de nuevo por seguridad
        com.mycompany.ventacontrolfx.model.User currentUser = com.mycompany.ventacontrolfx.util.UserSession
                .getInstance().getCurrentUser();
        if (currentUser == null || !"admin".equalsIgnoreCase(currentUser.getRole())) {
            AlertUtil.showError("Acceso Denegado", "No tienes permisos para acceder a esta sección.");
            return;
        }

        // Use navigationService to show view in main content area
        if (navigationService != null) {
            navigationService.showUsersView(() -> {
                // Actions after loading if needed
                if (filterLabel != null) {
                    filterLabel.setText("Gestión de Usuarios");
                }
            });
        }
    }

    // Check for admin role in initialize
    private void checkAdminRole() {
        com.mycompany.ventacontrolfx.model.User currentUser = com.mycompany.ventacontrolfx.util.UserSession
                .getInstance().getCurrentUser();
        if (currentUser != null && "admin".equalsIgnoreCase(currentUser.getRole())) {
            if (btnUsers != null) {
                btnUsers.setVisible(true);
                btnUsers.setManaged(true);
            }
            if (btnConfig != null) {
                btnConfig.setVisible(true);
                btnConfig.setManaged(true);
            }
            // Update user label
            updateUserLabel(currentUser.getUsername() + " (Admin)");
        } else {
            if (btnUsers != null) {
                btnUsers.setVisible(false);
                btnUsers.setManaged(false);
            }
            if (btnConfig != null) {
                btnConfig.setVisible(false);
                btnConfig.setManaged(false);
            }
            if (currentUser != null)
                updateUserLabel(currentUser.getUsername());
        }
    }

    @FXML
    private void handleLockApp() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/lock_screen.fxml"));
            Parent root = loader.load();

            // LockScreenController controller = loader.getController();

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.initOwner(mainContent.getScene().getWindow());
            stage.initStyle(StageStyle.UNDECORATED);

            // Make the lock screen cover the entire application window
            Stage owner = (Stage) mainContent.getScene().getWindow();

            Scene scene = new Scene(root, owner.getWidth(), owner.getHeight());
            scene.getStylesheets().add(getClass().getResource("/view/style.css").toExternalForm());

            stage.setScene(scene);

            // Position exactly over the main window
            stage.setX(owner.getX());
            stage.setY(owner.getY());
            stage.setWidth(owner.getWidth());
            stage.setHeight(owner.getHeight());

            stage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("Error al bloquear la sesión", e.getMessage());
        }
    }

    // Helper to update the text in the menu button
    private void updateUserLabel(String text) {
        if (userMenuButton != null && userMenuButton.getGraphic() instanceof HBox) {
            HBox hbox = (HBox) userMenuButton.getGraphic();
            for (Node child : hbox.getChildren()) {
                if (child instanceof Label) {
                    Label lbl = (Label) child;
                    // Heuristic to find the label with text (not the arrow)
                    if (!"▼".equals(lbl.getText())) {
                        lbl.setText(text);
                        break;
                    }
                }
            }
        }
    }

    private void loadLogo() {
        com.mycompany.ventacontrolfx.service.SaleConfigService configService = new com.mycompany.ventacontrolfx.service.SaleConfigService();
        com.mycompany.ventacontrolfx.model.SaleConfig cfg = configService.load();
        String logoPath = cfg.getLogoPath();
        String appName = cfg.getAppName();

        if (logoPath != null && !logoPath.isEmpty()) {
            java.io.File file = new java.io.File(logoPath);
            if (file.exists()) {
                try {
                    imgLogo.setImage(new Image(file.toURI().toString()));
                    imgLogo.setManaged(true);
                    imgLogo.setVisible(true);
                    logoTextContainer.setManaged(false);
                    logoTextContainer.setVisible(false);
                } catch (Exception e) {
                    System.err.println("Error loading logo: " + e.getMessage());
                    showDefaultLogo(appName);
                }
            } else {
                showDefaultLogo(appName);
            }
        } else {
            showDefaultLogo(appName);
        }
    }

    private void showDefaultLogo(String appName) {
        imgLogo.setManaged(false);
        imgLogo.setVisible(false);
        logoTextContainer.setManaged(true);
        logoTextContainer.setVisible(true);
        if (lblAppName != null && appName != null && !appName.isEmpty()) {
            lblAppName.setText(appName);
        }
    }

    public void refreshCounts() {
        Platform.runLater(() -> {
            try {
                if (labelCountProducts != null) {
                    // Si estamos en la vista de venta (productsPane es el contenido actual),
                    // mostramos la cantidad de productos filtrados en lugar del total de la BD.
                    // Si estamos en la vista de venta (el contenido del scroll es el panel de
                    // productos),
                    // mostramos la cantidad de productos filtrados.
                    if (productsPane != null && mainContent != null && mainContent.getContent() == productsPane) {
                        labelCountProducts.setText(String.valueOf(products.size()));
                    } else {
                        // En cualquier otro caso (o si no podemos determinarlo), mostramos el total de
                        // la base de datos
                        labelCountProducts.setText(String.valueOf(productService.getCount()));
                    }
                }
                if (labelCountCategories != null)
                    labelCountCategories.setText(String.valueOf(categoryService.getCount()));
                if (labelCountHistory != null)
                    labelCountHistory.setText(String.valueOf(saleService.getTotalCount()));
                if (labelCountClosures != null)
                    labelCountClosures.setText(String.valueOf(closureService.getCount()));
                if (labelCountClients != null)
                    labelCountClients.setText(String.valueOf(clientService.getCount()));
                if (labelCountUsers != null)
                    labelCountUsers.setText(String.valueOf(userService.getCount()));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public static void updateCounts() {
        if (instance != null) {
            instance.refreshCounts();
        }
    }

    public static void updateProductCount(int count) {
        if (instance != null && instance.labelCountProducts != null) {
            Platform.runLater(() -> instance.labelCountProducts.setText(String.valueOf(count)));
        }
    }

}
