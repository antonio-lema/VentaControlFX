package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.application.service.ProductUseCase;
import com.mycompany.ventacontrolfx.model.Product;
import com.mycompany.ventacontrolfx.service.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.AppLogger;
import com.mycompany.ventacontrolfx.util.AsyncManager;
import com.mycompany.ventacontrolfx.util.Injectable;
import com.mycompany.ventacontrolfx.util.Searchable;
import com.mycompany.ventacontrolfx.control.ToggleSwitch;
import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.geometry.Pos;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.util.Duration;
import java.io.File;
import java.util.List;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

/**
 * Enterprise Product List Controller.
 * Optimized with debounce background filtering, batch rendering, and Weak
 * Memory Cache.
 * Reacts to EventBus changes for real-time synchronization.
 */
public class ProductController implements Injectable, Searchable {
    private static final String TAG = "ProductController";

    @FXML
    private TableView<Product> productsTable;
    @FXML
    private TableColumn<Product, String> colCategoryName, colName, colImage;
    @FXML
    private TableColumn<Product, Double> colPrice;
    @FXML
    private TableColumn<Product, Boolean> colFavorite, colVisible;
    @FXML
    private TableColumn<Product, Void> colActions;
    @FXML
    private TextField searchField, rowsPerPageField;

    private final WeakHashMap<String, Image> imageCache = new WeakHashMap<>();
    private ProductUseCase productUseCase;
    private ServiceContainer container;
    private ObservableList<Product> fullProductList = FXCollections.observableArrayList();

    // For Filter Optimization (debouncing and cancellation)
    private Task<List<Product>> currentFilterTask;
    private final PauseTransition debounce = new PauseTransition(Duration.millis(300));

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.productUseCase = container.getProductUseCase();

        setupTable();
        loadAllProducts();

        // Background Filtering optimization with Debounce (300ms)
        debounce.setOnFinished(e -> requestFilter());
        searchField.textProperty().addListener((obs, old, nv) -> debounce.playFromStart());
        rowsPerPageField.textProperty().addListener((obs, old, nv) -> debounce.playFromStart());
    }

    private void setupTable() {
        colCategoryName.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));

        setupImageColumn();
        setupToggleColumn(colFavorite, true);
        setupToggleColumn(colVisible, false);
        setupActionColumn();

        productsTable.getSortOrder().add(colFavorite);
        colFavorite.setSortType(TableColumn.SortType.DESCENDING);
    }

    private void setupImageColumn() {
        colImage.setCellFactory(column -> new TableCell<Product, String>() {
            private final ImageView imageView = new ImageView();
            private final javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(20, 20, 20);

            @Override
            protected void updateItem(String imagePath, boolean empty) {
                super.updateItem(imagePath, empty);
                if (empty || imagePath == null || imagePath.isEmpty()) {
                    setGraphic(null);
                } else {
                    File file = new File(imagePath);
                    if (file.exists()) {
                        String uri = file.toURI().toString();
                        Image image = imageCache.get(uri);
                        if (image == null) {
                            image = new Image(uri, 40, 40, false, true, true);
                            imageCache.put(uri, image);
                        }
                        imageView.setImage(image);
                        imageView.setFitHeight(40);
                        imageView.setFitWidth(40);
                        imageView.setClip(clip);
                        HBox container = new HBox(imageView);
                        container.setAlignment(Pos.CENTER);
                        container.setStyle("-fx-padding: 2; -fx-border-color: #e0e6ed; -fx-border-radius: 50%;");
                        setGraphic(container);
                    } else {
                        setGraphic(new de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView(
                                de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.IMAGE));
                    }
                }
            }
        });
    }

    private void setupToggleColumn(TableColumn<Product, Boolean> col, boolean isFavorite) {
        col.setCellValueFactory(new PropertyValueFactory<>(isFavorite ? "favorite" : "visible"));
        col.setCellFactory(column -> new TableCell<Product, Boolean>() {
            private final ToggleSwitch toggle = new ToggleSwitch();
            {
                toggle.setOnMouseClicked(event -> {
                    Product p = getTableView().getItems().get(getIndex());
                    boolean newState = !toggle.isSwitchedOn();
                    if (isFavorite)
                        p.setFavorite(newState);
                    else
                        p.setVisible(newState);
                    AsyncManager.execute(productUseCase.saveOrUpdateTask(p), v -> {
                    });
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null)
                    setGraphic(null);
                else {
                    toggle.setState(item);
                    setGraphic(toggle);
                    setAlignment(Pos.CENTER);
                }
            }
        });
    }

    private void setupActionColumn() {
        colActions.setCellFactory(param -> new TableCell<Product, Void>() {
            private final Button btnEdit = createActionButton("PENCIL", "#1e88e5");
            private final Button btnDelete = createActionButton("TRASH", "#e53935");
            private final HBox pane = new HBox(8, btnEdit, btnDelete);
            {
                pane.setAlignment(Pos.CENTER);
                btnEdit.setOnAction(e -> handleEditProduct(getTableView().getItems().get(getIndex())));
                btnDelete.setOnAction(e -> handleDeleteProduct(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private Button createActionButton(String iconName, String color) {
        Button btn = new Button();
        de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView icon = new de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView(
                de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.valueOf(iconName));
        icon.setSize("16");
        icon.setFill(javafx.scene.paint.Color.web(color));
        btn.setGraphic(icon);
        btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        return btn;
    }

    private void loadAllProducts() {
        AsyncManager.execute(productUseCase.getAllProductsTask(), products -> {
            fullProductList.setAll(products);
            requestFilter();
        });
    }

    private void requestFilter() {
        if (currentFilterTask != null && currentFilterTask.isRunning()) {
            currentFilterTask.cancel();
        }

        String query = searchField.getText();
        String limitStr = rowsPerPageField.getText();

        currentFilterTask = new Task<>() {
            @Override
            protected List<Product> call() {
                int limit = Integer.MAX_VALUE;
                try {
                    if (limitStr != null && !limitStr.trim().isEmpty())
                        limit = Integer.parseInt(limitStr.trim());
                } catch (Exception e) {
                }

                String finalQuery = (query != null) ? query.toLowerCase().trim() : "";

                return fullProductList.stream()
                        .filter(p -> {
                            if (isCancelled())
                                return false;
                            return finalQuery.isEmpty() ||
                                    p.getName().toLowerCase().contains(finalQuery) ||
                                    String.valueOf(p.getId()).contains(finalQuery) ||
                                    (p.getCategoryName() != null
                                            && p.getCategoryName().toLowerCase().contains(finalQuery));
                        })
                        .limit(limit)
                        .sorted((p1, p2) -> {
                            int favCompare = Boolean.compare(p2.isFavorite(), p1.isFavorite());
                            return favCompare != 0 ? favCompare : p1.getName().compareToIgnoreCase(p2.getName());
                        })
                        .collect(Collectors.toList());
            }
        };

        AsyncManager.execute(currentFilterTask, filtered -> {
            productsTable.setItems(FXCollections.observableArrayList(filtered));
        });
    }

    @Override
    public void handleSearch(String text) {
        searchField.setText(text);
    }

    @FXML
    private void handleAddProduct() {
        openProductDialog(null);
    }

    private void handleEditProduct(Product product) {
        openProductDialog(product);
    }

    private void openProductDialog(Product product) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/add_product.fxml"));
            Parent root = loader.load();
            AddProductController controller = loader.getController();
            controller.inject(container);
            if (product != null)
                controller.setProduct(product);

            Stage stage = new Stage();
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            stage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root);
            scene.setFill(null);
            scene.getStylesheets().add(getClass().getResource("/view/style.css").toExternalForm());
            stage.setScene(scene);
            stage.showAndWait();
            loadAllProducts();
        } catch (Exception e) {
            AppLogger.error(TAG, "Failed to open product dialog", e);
            AlertUtil.showError("Error UI", "No se pudo abrir la ventana del producto.");
        }
    }

    private void handleDeleteProduct(Product product) {
        if (AlertUtil.showConfirmation("Confirmar Eliminación", "¿Eliminar '" + product.getName() + "'?",
                "Esta acción no se puede deshacer.")) {
            AsyncManager.execute(productUseCase.deleteTask(product.getId()), v -> {
                loadAllProducts();
            });
        }
    }
}
