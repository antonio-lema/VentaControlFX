package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.Product;
import com.mycompany.ventacontrolfx.service.ProductService;
import java.sql.SQLException;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
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
import java.io.IOException;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import java.util.Optional;
import javafx.scene.control.ButtonType;

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
            loadProducers();
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
                        imageView.setFitHeight(50);
                        imageView.setFitWidth(50);
                        imageView.setPreserveRatio(true);
                        setGraphic(imageView);
                    } else {
                        setGraphic(null); // Or a placeholder icon
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
                        // Revert if failed
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
                        // Revert if failed
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
                }
            }
        });

        // Actions Column
        colActions.setCellFactory(param -> new TableCell<Product, Void>() {
            private final Button btnEdit = new Button();
            private final Button btnDelete = new Button();
            private final HBox pane = new HBox(5, btnEdit, btnDelete);

            {
                pane.setAlignment(Pos.CENTER);

                // Edit Icon (Pencil)
                SVGPath editIcon = new SVGPath();
                editIcon.setContent(
                        "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z");
                editIcon.getStyleClass().add("svg-path");

                btnEdit.setGraphic(editIcon);
                btnEdit.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                btnEdit.getStyleClass().addAll("btn-icon", "btn-edit");
                btnEdit.setTooltip(new javafx.scene.control.Tooltip("Editar Producto"));

                // Delete Icon (Trash)
                SVGPath deleteIcon = new SVGPath();
                deleteIcon.setContent("M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z");
                deleteIcon.getStyleClass().add("svg-path");

                btnDelete.setGraphic(deleteIcon);
                btnDelete.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
                btnDelete.getStyleClass().addAll("btn-icon", "btn-delete");
                btnDelete.setTooltip(new javafx.scene.control.Tooltip("Eliminar Producto"));

                btnEdit.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    handleEditProduct(product);
                });

                btnDelete.setOnAction(event -> {
                    Product product = getTableView().getItems().get(getIndex());
                    handleDeleteProduct(product);
                });
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

    private void loadProducers() {
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
            stage.setTitle("Añadir Producto");
            stage.setScene(new Scene(root));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setResizable(false);

            stage.showAndWait();

            // Refresh table after dialog closes
            loadProducers(); // Note: Method name in existing code is 'loadProducers' (typo? implies loading
                             // products). I'll keep it as is or fix typo if I can. File view showed
                             // 'loadProducers'.
        } catch (java.io.IOException e) {
            e.printStackTrace();
            showAlert("Error", "No se pudo abrir el diálogo: " + e.getMessage());
        }
    }

    private void handleEditProduct(Product product) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/add_product.fxml"));
            Parent root = loader.load();

            // Access the controller to pass data
            AddProductController controller = loader.getController();
            controller.setProduct(product); // Need to implement this in
            // AddProductController

            Stage stage = new Stage();
            stage.setTitle("Editar Producto");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.showAndWait();

            loadProducers();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "No se pudo abrir el diálogo de edición: " + e.getMessage());
        }
    }

    private void handleDeleteProduct(Product product) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar Eliminación");
        alert.setHeaderText("Eliminar Producto");
        alert.setContentText("¿Está seguro de que desea eliminar el producto '" + product.getName() + "'?");

        // Add styling to dialog
        javafx.scene.control.DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/view/style.css").toExternalForm());
        dialogPane.getStyleClass().add("custom-alert");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                productService.deleteProduct(product.getId());
                loadProducers();
            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Error", "No se pudo eliminar el producto: " + e.getMessage());
            }
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
