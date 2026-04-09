package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.Category;
import com.mycompany.ventacontrolfx.domain.model.TaxGroup;
import com.mycompany.ventacontrolfx.domain.service.TaxEngineService;
import com.mycompany.ventacontrolfx.application.usecase.CategoryUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.sql.SQLException;
import java.util.List;

public class AddCategoryController implements Injectable {

    @FXML
    private TextField txtName, txtIva;
    @FXML
    private ComboBox<TaxGroup> cmbTaxGroup;
    @FXML
    private Label lblTitle, lblSubtitle;
    @FXML
    private StackPane rootStackPane;

    private ServiceContainer container;
    private CategoryUseCase categoryUseCase;
    private TaxEngineService taxEngineService;
    private Category categoryToEdit;

    private double xOffset = 0, yOffset = 0;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.categoryUseCase = container.getCategoryUseCase();
        this.taxEngineService = container.getTaxEngineService();
        setupTaxGroupComboBox();
        loadTaxGroups();

        // Listener para exclusi\u00c3\u00b3n mutua y sincronizaci\u00c3\u00b3n espejo
        cmbTaxGroup.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // Modo V2: Sincronizar campo IVA con el total del grupo
                double totalRate = newVal.getRates().stream()
                        .mapToDouble(com.mycompany.ventacontrolfx.domain.model.TaxRate::getRate)
                        .sum();
                txtIva.setText(String.valueOf(totalRate));
                txtIva.setDisable(true);
            } else {
                // Modo Legacy: Habilitar edici\u00c3\u00b3n manual
                txtIva.setDisable(false);
                if (categoryToEdit != null) {
                    txtIva.setText(String.valueOf(categoryToEdit.getDefaultIva()));
                } else {
                    txtIva.setText("21.0");
                }
            }
        });
    }

    public void setCategory(Category category) {
        this.categoryToEdit = category;
        if (category != null) {
            lblTitle.setText(container.getBundle().getString("category.form.edit_title"));
            lblSubtitle.setText(container.getBundle().getString("category.form.edit_subtitle"));
            txtName.setText(category.getName());
            txtIva.setText(String.valueOf(category.getDefaultIva()));

            // Seleccionar grupo de impuestos si existe
            if (category.getTaxGroupId() != null) {
                for (TaxGroup tg : cmbTaxGroup.getItems()) {
                    if (java.util.Objects.equals(tg.getId(), category.getTaxGroupId())) {
                        cmbTaxGroup.setValue(tg);
                        break;
                    }
                }
            } else {
                cmbTaxGroup.setValue(null);
            }

            // Forzar actualizaci\u00c3\u00b3n de estado y sincronizaci\u00c3\u00b3n del campo IVA
            if (category.getTaxGroupId() != null) {
                TaxGroup currentGroup = cmbTaxGroup.getValue();
                if (currentGroup != null) {
                    double totalRate = currentGroup.getRates().stream().mapToDouble(r -> r.getRate()).sum();
                    txtIva.setText(String.valueOf(totalRate));
                }
                txtIva.setDisable(true);
            } else {
                txtIva.setDisable(false);
                txtIva.setText(String.valueOf(category.getDefaultIva()));
            }
        } else {
            lblTitle.setText(container.getBundle().getString("category.form.new_title"));
            lblSubtitle.setText(container.getBundle().getString("category.form.new_subtitle"));
            txtIva.setText("21.0");
            cmbTaxGroup.setValue(null);
        }
    }

    @FXML
    private void handleSave() {
        String name = txtName.getText();
        String ivaStr = txtIva.getText();

        if (name == null || name.trim().isEmpty()) {
            AlertUtil.showWarning(container.getBundle().getString("alert.validation"),
                    container.getBundle().getString("category.error.name_required"));
            return;
        }

        double iva = 21.0;
        try {
            if (ivaStr != null && !ivaStr.trim().isEmpty()) {
                iva = Double.parseDouble(ivaStr.replace(",", "."));
            }
        } catch (NumberFormatException e) {
            AlertUtil.showWarning(container.getBundle().getString("alert.validation"),
                    container.getBundle().getString("category.error.invalid_iva"));
        }

        try {
            TaxGroup selectedGroup = cmbTaxGroup.getValue();
            Integer taxGroupId = (selectedGroup != null) ? selectedGroup.getId() : null;

            if (categoryToEdit == null) {
                Category newCat = new Category(name);
                newCat.setDefaultIva(iva);
                newCat.setTaxGroupId(taxGroupId);
                categoryUseCase.addCategory(newCat);
            } else {
                categoryToEdit.setName(name);
                categoryToEdit.setDefaultIva(iva);
                categoryToEdit.setTaxGroupId(taxGroupId);
                categoryUseCase.updateCategory(categoryToEdit);
            }
            handleCancel();
        } catch (SQLException e) {
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("error.save") + ": " + e.getMessage());
        }
    }

    private void setupTaxGroupComboBox() {
        cmbTaxGroup.setConverter(new StringConverter<>() {
            @Override
            public String toString(TaxGroup t) {
                return t != null ? t.getName() : container.getBundle().getString("category.form.tax_group.legacy");
            }

            @Override
            public TaxGroup fromString(String s) {
                return null;
            }
        });
    }

    private void loadTaxGroups() {
        try {
            List<TaxGroup> groups = taxEngineService.getAllGroups();
            cmbTaxGroup.setItems(FXCollections.observableArrayList(groups));
        } catch (Exception e) {
            // No bloquear UI si falla tax engine
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
