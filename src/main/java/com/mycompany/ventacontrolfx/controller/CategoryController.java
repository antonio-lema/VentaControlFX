package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.Category;
import com.mycompany.ventacontrolfx.service.CategoryService;
import java.sql.SQLException;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class CategoryController {

    @FXML
    private TableView<Category> categoriesTable;
    @FXML
    private TableColumn<Category, Integer> colId;
    @FXML
    private TableColumn<Category, String> colName;
    @FXML
    private TextField searchField;
    @FXML
    private Button btnAdd;

    private CategoryService categoryService;
    private ObservableList<Category> categoryList;

    public void initialize() {
        categoryService = new CategoryService();
        categoryList = FXCollections.observableArrayList();

        setupColumns();
        loadCategories();

        // Search functionality
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterCategories(newValue);
        });
    }

    private void setupColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
    }

    private void loadCategories() {
        try {
            List<Category> categories = categoryService.getAllCategories();
            categoryList.setAll(categories);
            categoriesTable.setItems(categoryList);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "No se pudieron cargar las categorías: " + e.getMessage());
        }
    }

    private void filterCategories(String query) {
        if (query == null || query.isEmpty()) {
            categoriesTable.setItems(categoryList);
        } else {
            ObservableList<Category> filtered = FXCollections.observableArrayList();
            for (Category c : categoryList) {
                if (c.getName().toLowerCase().contains(query.toLowerCase())) {
                    filtered.add(c);
                }
            }
            categoriesTable.setItems(filtered);
        }
    }

    @FXML
    private void handleAddCategory() {
        // Logic to open Add Category Dialog
        showAlert("Información", "Funcionalidad de añadir categoría en desarrollo.");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
