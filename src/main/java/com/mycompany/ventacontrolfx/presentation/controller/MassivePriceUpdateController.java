package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.application.usecase.MassivePriceUpdateUseCase;
import com.mycompany.ventacontrolfx.domain.model.Category;
import com.mycompany.ventacontrolfx.domain.repository.ICategoryRepository;
import com.mycompany.ventacontrolfx.domain.repository.IPriceRepository;
import com.mycompany.ventacontrolfx.infrastructure.persistence.JdbcCategoryRepository;
import com.mycompany.ventacontrolfx.infrastructure.persistence.JdbcPriceRepository;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class MassivePriceUpdateController {

    @FXML
    private ComboBox<Category> cmbCategory;
    @FXML
    private TextField txtPercentage;
    @FXML
    private TextField txtReason;
    @FXML
    private Button btnApply;

    private ICategoryRepository categoryRepository = new JdbcCategoryRepository();
    private MassivePriceUpdateUseCase massivePriceUpdateUseCase;

    // We will assume priceListId 1 as default for the UI since this is a
    // single-tiered bazaar usually.
    private final int DEFAULT_PRICE_LIST_ID = 1;
    private boolean dbUpdated = false;

    @FXML
    public void initialize() {
        IPriceRepository repo = new JdbcPriceRepository();
        massivePriceUpdateUseCase = new MassivePriceUpdateUseCase(repo);
        loadCategories();
    }

    private void loadCategories() {
        try {
            List<Category> categories = categoryRepository.getAll();

            // Un item nulo/vacio para significar "Todas" (Inventario completo)
            Category allCategory = new Category(0, "Todas (Inventario completo)", true, false, 0.0);
            cmbCategory.getItems().add(allCategory);
            cmbCategory.getItems().addAll(categories);
            cmbCategory.getSelectionModel().selectFirst();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error", "No se pudieron cargar las categorías: " + e.getMessage());
        }
    }

    @FXML
    private void handleApply(ActionEvent event) {
        if (!validateInput()) {
            return;
        }

        double percentage = Double.parseDouble(txtPercentage.getText());
        String reason = txtReason.getText();
        Category selectedCategory = cmbCategory.getSelectionModel().getSelectedItem();

        try {
            int updatedCount = 0;
            if (selectedCategory != null && selectedCategory.getId() > 0) {
                updatedCount = massivePriceUpdateUseCase.applyPercentageIncreaseToCategory(DEFAULT_PRICE_LIST_ID,
                        selectedCategory.getId(), percentage, reason);
            } else {
                updatedCount = massivePriceUpdateUseCase.applyPercentageIncreaseToAll(DEFAULT_PRICE_LIST_ID, percentage,
                        reason);
            }

            this.dbUpdated = true;
            showAlert(Alert.AlertType.INFORMATION, "Éxito",
                    "Precios actualizados masivamente con éxito!\nRegistros modificados: " + updatedCount);
            closeStage();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Error en Base de Datos",
                    "No se pudieron aplicar los precios masivos:\n" + e.getMessage());
        }
    }

    private boolean validateInput() {
        try {
            double p = Double.parseDouble(txtPercentage.getText());
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Error de Validación",
                    "El porcentaje ingresado no es un número válido.");
            return false;
        }

        if (txtReason.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Error de Validación", "Por favor ingresa un motivo para el registro.");
            return false;
        }
        return true;
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) btnApply.getScene().getWindow();
        stage.close();
    }

    public boolean isDbUpdated() {
        return dbUpdated;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
