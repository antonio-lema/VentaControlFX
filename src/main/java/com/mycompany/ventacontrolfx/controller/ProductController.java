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

public class ProductController {

    @FXML
    private TableView<Product> productsTable;
    @FXML
    private TableColumn<Product, Integer> colId;
    @FXML
    private TableColumn<Product, Integer> colCategoryId;
    @FXML
    private TableColumn<Product, String> colName;
    @FXML
    private TableColumn<Product, Double> colPrice;
    @FXML
    private TableColumn<Product, Boolean> colFavorite;
    @FXML
    private TextField searchField;
    @FXML
    private Button btnAdd;

    private ProductService productService;
    private ObservableList<Product> productList;

    public void initialize() {
        productService = new ProductService();
        productList = FXCollections.observableArrayList();

        setupColumns();
        loadProducers();

        // Search functionality
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterProducts(newValue);
        });
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colCategoryId.setCellValueFactory(new PropertyValueFactory<>("categoryId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("price"));

        // Favorite Column with Checkbox (Read-only or Interactive)
        colFavorite.setCellValueFactory(new PropertyValueFactory<>("isFavorite"));
        colFavorite.setCellFactory(column -> new TableCell<Product, Boolean>() {
            private final CheckBox checkBox = new CheckBox();

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    checkBox.setSelected(item);
                    checkBox.setDisable(true); // Read-only for list view
                    setGraphic(checkBox);
                }
            }
        });
    }

    private void loadProducers() {
        try {
            List<Product> products = productService.getAllProducts();
            productList.setAll(products);
            productsTable.setItems(productList);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "No se pudieron cargar los productos: " + e.getMessage());
        }
    }

    private void filterProducts(String query) {
        if (query == null || query.isEmpty()) {
            productsTable.setItems(productList);
        } else {
            ObservableList<Product> filtered = FXCollections.observableArrayList();
            for (Product p : productList) {
                if (p.getName().toLowerCase().contains(query.toLowerCase())) {
                    filtered.add(p);
                }
            }
            productsTable.setItems(filtered);
        }
    }

    @FXML
    private void handleAddProduct() {
        // Logic to open Add Product Dialog
        showAlert("Información", "Funcionalidad de añadir producto en desarrollo.");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
