package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.Category;
import com.mycompany.ventacontrolfx.application.usecase.CategoryUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.sql.SQLException;

public class AddCategoryController implements Injectable {

    @FXML
    private TextField txtName, txtIva;
    @FXML
    private Label lblTitle, lblSubtitle;
    @FXML
    private StackPane rootStackPane;

    private CategoryUseCase categoryUseCase;
    private Category categoryToEdit;

    private double xOffset = 0, yOffset = 0;

    @Override
    public void inject(ServiceContainer container) {
        this.categoryUseCase = container.getCategoryUseCase();
    }

    public void setCategory(Category category) {
        this.categoryToEdit = category;
        if (category != null) {
            lblTitle.setText("Editar Categoría");
            lblSubtitle.setText("Modifica el nombre de la categoría");
            txtName.setText(category.getName());
            txtIva.setText(String.valueOf(category.getDefaultIva()));
        } else {
            lblTitle.setText("Nueva Categoría");
            lblSubtitle.setText("Introduce el nombre de la categoría");
            txtIva.setText("21.0");
        }
    }

    @FXML
    private void handleSave() {
        String name = txtName.getText();
        String ivaStr = txtIva.getText();

        if (name == null || name.trim().isEmpty()) {
            AlertUtil.showWarning("Validación", "Introduce un nombre.");
            return;
        }

        double iva = 21.0;
        try {
            iva = Double.parseDouble(ivaStr);
        } catch (NumberFormatException e) {
            AlertUtil.showWarning("Validación", "IVA inválido. Se usará 21.0%.");
        }

        try {
            if (categoryToEdit == null) {
                Category newCat = new Category(name);
                newCat.setDefaultIva(iva);
                categoryUseCase.addCategory(newCat);
            } else {
                categoryToEdit.setName(name);
                categoryToEdit.setDefaultIva(iva);
                categoryUseCase.updateCategory(categoryToEdit);
            }
            handleCancel();
        } catch (SQLException e) {
            AlertUtil.showError("Error", "No se pudo guardar: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        ((Stage) txtName.getScene().getWindow()).close();
    }

    @FXML
    private void handleMousePressed(javafx.scene.input.MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML
    private void handleMouseDragged(javafx.scene.input.MouseEvent event) {
        Stage stage = (Stage) txtName.getScene().getWindow();
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }
}
