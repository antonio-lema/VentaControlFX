package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.Product;
import com.mycompany.ventacontrolfx.service.ProductService;
import java.sql.SQLException;
import java.util.List;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.shape.SVGPath;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;
import com.mycompany.ventacontrolfx.control.ToggleSwitch;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.control.TableCell;

public class ProductController {

    @FXML
    private TableView<Product> productsTable;
    @FXML
    private TableColumn<Product, String> colCategoryName;
    @FXML
    private TableColumn<Product, String> colName;
    @FXML
    private TableColumn<Product, Double> colPrice;
    @FXML
    private TableColumn<Product, Boolean> colFavorite;
    @FXML
    private TableColumn<Product, String> colImage;
    @FXML
    private TableColumn<Product, Boolean> colVisible;
    @FXML
    private TableColumn<Product, Void> colActions;
    @FXML
    private TextField searchField;
    @FXML
    private Button btnAdd;
    @FXML
    private TextField rowsPerPageField;

    private ProductService productService;
    private ObservableList<Product> productList;

    public void initialize() {
        productService = new ProductService();
        productList = FXCollections.observableArrayList();

        setupColumns();
        Platform.runLater(() -> {
            loadAllProducts();
        });

        // Search functionality

        // Search functionality
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterProducts(newValue, rowsPerPageField.getText());
        });

        // Rows per page functionality
        rowsPerPageField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterProducts(searchField.getText(), newValue);
        });
    }

    private void setupColumns() {
        colCategoryName.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));

        // Image Column
        colImage.setCellValueFactory(new PropertyValueFactory<>("imagePath"));
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
                        Image image = new Image(file.toURI().toString());
                        imageView.setImage(image);
                        imageView.setFitHeight(40);
                        imageView.setFitWidth(40);
                        imageView.setPreserveRatio(false); // Force square for perfect circle
                        imageView.setClip(clip);

                        // Wrap in a container to add a border/effect if needed
                        HBox container = new HBox(imageView);
                        container.setAlignment(Pos.CENTER);
                        container.setStyle(
                                "-fx-padding: 2; -fx-border-color: #e0e6ed; -fx-border-radius: 50%; -fx-border-width: 1;");

                        setGraphic(container);
                    } else {
                        // Placeholder icon
                        de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView placeholder = new de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView(
                                de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.IMAGE);
                        placeholder.setSize("24");
                        placeholder.setFill(javafx.scene.paint.Color.rgb(189, 189, 189));
                        setGraphic(placeholder);
                        setAlignment(Pos.CENTER);
                    }
                }
            }
        });

        // Favorite Column with ToggleSwitch
        colFavorite.setCellValueFactory(new PropertyValueFactory<>("favorite"));
        colFavorite.setCellFactory(column -> new TableCell<Product, Boolean>() {
            private final ToggleSwitch toggle = new ToggleSwitch();

            {
                toggle.setOnMouseClicked(event -> {
                    boolean newState = !toggle.isSwitchedOn();
                    toggle.setSwitchedOn(newState);

                    Product product = getTableView().getItems().get(getIndex());
                    product.setFavorite(newState);
                    try {
                        productService.updateProduct(product);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        toggle.setSwitchedOn(!newState);
                        product.setFavorite(!newState);
                        showAlert("Error", "No se pudo actualizar favorito: " + e.getMessage());
                    }
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    toggle.setState(item);
                    setGraphic(toggle);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Visible Column with ToggleSwitch
        colVisible.setCellValueFactory(new PropertyValueFactory<>("visible"));
        colVisible.setCellFactory(column -> new TableCell<Product, Boolean>() {
            private final ToggleSwitch toggle = new ToggleSwitch();

            {
                toggle.setOnMouseClicked(event -> {
                    boolean newState = !toggle.isSwitchedOn();
                    toggle.setSwitchedOn(newState);

                    Product product = getTableView().getItems().get(getIndex());
                    product.setVisible(newState);
                    try {
                        productService.updateProduct(product);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        toggle.setSwitchedOn(!newState);
                        product.setVisible(!newState);
                        showAlert("Error", "No se pudo actualizar visibilidad: " + e.getMessage());
                    }
                });
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    toggle.setState(item);
                    setGraphic(toggle);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // Actions Column
        colActions.setCellFactory(param -> new TableCell<Product, Void>() {
            private final Button btnEdit = new Button();
            private final Button btnDelete = new Button();
            private final HBox pane = new HBox(8, btnEdit, btnDelete);

            {
                pane.setAlignment(Pos.CENTER);

                // Edit Icon
                de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView editIcon = new de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView(
                        de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PENCIL);
                editIcon.setSize("16");
                editIcon.setFill(javafx.scene.paint.Color.web("#1e88e5"));

                btnEdit.setGraphic(editIcon);
                btnEdit.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                btnEdit.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 5;");
                btnEdit.setTooltip(new javafx.scene.control.Tooltip("Editar Producto"));

                // Delete Icon
                de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView deleteIcon = new de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView(
                        de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.TRASH);
                deleteIcon.setSize("16");
                deleteIcon.setFill(javafx.scene.paint.Color.web("#e53935"));

                btnDelete.setGraphic(deleteIcon);
                btnDelete.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                btnDelete.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-padding: 5;");
                btnDelete.setTooltip(new javafx.scene.control.Tooltip("Eliminar Producto"));

                btnEdit.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    handleEditProduct(product);
                });

                btnDelete.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    handleDeleteProduct(product);
                });

                // Hover effects
                btnEdit.setOnMouseEntered(e -> editIcon.setFill(javafx.scene.paint.Color.web("#1565c0")));
                btnEdit.setOnMouseExited(e -> editIcon.setFill(javafx.scene.paint.Color.web("#1e88e5")));
                btnDelete.setOnMouseEntered(e -> deleteIcon.setFill(javafx.scene.paint.Color.web("#c62828")));
                btnDelete.setOnMouseExited(e -> deleteIcon.setFill(javafx.scene.paint.Color.web("#e53935")));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(pane);
                }
            }
        });
    }

    private void loadAllProducts() {
        try {
            List<Product> products = productService.getAllProducts();
            productList.setAll(products);
            // Initial filter to respect default limit
            filterProducts(searchField.getText(), rowsPerPageField.getText());
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "No se pudieron cargar los productos: " + e.getMessage());
        }
    }

    private void filterProducts(String query, String limitStr) {
        int limit = Integer.MAX_VALUE;
        try {
            if (limitStr != null && !limitStr.trim().isEmpty()) {
                limit = Integer.parseInt(limitStr.trim());
            }
        } catch (NumberFormatException e) {
            // Ignore invalid input, use default (all)
        }

        ObservableList<Product> filtered = FXCollections.observableArrayList();
        String lowerCaseQuery = (query != null) ? query.toLowerCase() : "";

        for (Product p : productList) {
            if (filtered.size() >= limit) {
                break;
            }

            boolean matches = false;
            // If query is empty, everything matches (subject to limit)
            if (lowerCaseQuery.isEmpty()) {
                matches = true;
            } else {
                if (p.getName().toLowerCase().contains(lowerCaseQuery)) {
                    matches = true;
                } else if (String.valueOf(p.getId()).contains(lowerCaseQuery)) {
                    matches = true;
                } else if (p.getCategoryName() != null && p.getCategoryName().toLowerCase().contains(lowerCaseQuery)) {
                    matches = true;
                }
            }

            if (matches) {
                filtered.add(p);
            }
        }
        productsTable.setItems(filtered);
    }

    @FXML
    private void handleAddProduct() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/add_product.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            Scene scene = new Scene(root);
            scene.setFill(null);
            scene.getStylesheets().add(getClass().getResource("/view/style.css").toExternalForm());

            stage.setScene(scene);
            stage.showAndWait();
            loadAllProducts();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "No se pudo abrir la ventana de añadir producto: " + e.getMessage());
        }
    }

    private void handleEditProduct(Product product) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/add_product.fxml"));
            Parent root = loader.load();

            AddProductController controller = loader.getController();
            controller.setProduct(product);

            Stage stage = new Stage();
            stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            Scene scene = new Scene(root);
            scene.setFill(null);
            scene.getStylesheets().add(getClass().getResource("/view/style.css").toExternalForm());

            stage.setScene(scene);
            stage.showAndWait();
            loadAllProducts();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "No se pudo abrir la ventana de editar producto: " + e.getMessage());
        }
    }

    private void handleDeleteProduct(Product product) {
        boolean confirmed = AlertUtil.showConfirmation("Confirmar Eliminación",
                "¿Está seguro de que desea eliminar el producto '" + product.getName() + "'?",
                "Esta acción no se puede deshacer.");
        if (confirmed) {
            try {
                productService.deleteProduct(product.getId());
                loadAllProducts();
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Error", "No se pudo eliminar el producto: " + e.getMessage());
            }
        }
    }

    private void showAlert(String title, String content) {
        AlertUtil.showInfo(title, content);
    }
}
