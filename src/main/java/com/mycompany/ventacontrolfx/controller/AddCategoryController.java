package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.Category;
import com.mycompany.ventacontrolfx.service.CategoryService;
import java.sql.SQLException;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import com.mycompany.ventacontrolfx.util.AlertUtil;

public class AddCategoryController {

    @FXML
    private TextField txtName;
    @FXML
    private javafx.scene.control.Label lblTitle;
    @FXML
    private javafx.scene.control.Label lblSubtitle;

    private CategoryService categoryService;
    private Category categoryToEdit;

    @FXML
    private javafx.scene.layout.StackPane rootStackPane;

    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    private void handleMousePressed(javafx.scene.input.MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML
    private void handleMouseDragged(javafx.scene.input.MouseEvent event) {
        Stage stage = (Stage) rootStackPane.getScene().getWindow();
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }

    @FXML
    private void handleCancel() {
        Stage stage = (Stage) rootStackPane.getScene().getWindow();
        stage.close();
    }

    public void initialize() {
        categoryService = new CategoryService();
    }

    public void setCategory(Category category) {
        this.categoryToEdit = category;
        if (category != null) {
            lblTitle.setText("Editar Categoría");
            lblSubtitle.setText("Modifica el nombre de la categoría");
            txtName.setText(category.getName());
        } else {
            lblTitle.setText("Nueva Categoría");
            lblSubtitle.setText("Introduce el nombre de la categoría");
        }
    }

    @FXML
    private void handleSave() {
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
            handleCancel(); // Close dialog using the existing handleCancel method
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "No se pudo guardar la categoría: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        AlertUtil.showWarning(title, content);
    }
}
