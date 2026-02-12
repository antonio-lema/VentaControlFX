package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.Category;
import com.mycompany.ventacontrolfx.service.CategoryService;
import java.sql.SQLException;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AddCategoryController {

    @FXML
    private TextField txtName;

    private CategoryService categoryService;
    private Category categoryToEdit;

    public void initialize() {
        categoryService = new CategoryService();
    }

    public void setCategory(Category category) {
        this.categoryToEdit = category;
        if (category != null) {
            txtName.setText(category.getName());
        }
    }

    @FXML
    private void saveCategory() {
        String name = txtName.getText();

        // Validation
        if (name == null || name.trim().isEmpty()) {
            showAlert("Validación", "Por favor, introduce un nombre para la categoría.");
            return;
        }

        try {
            if (categoryToEdit == null) {
                Category category = new Category(name);
                categoryService.addCategory(category);
            } else {
                categoryToEdit.setName(name);
                categoryService.updateCategory(categoryToEdit);
            }
            closeDialog();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "No se pudo guardar la categoría: " + e.getMessage());
        }
    }

    @FXML
    private void cancel() {
        closeDialog();
    }

    private void closeDialog() {
        Stage stage = (Stage) txtName.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
