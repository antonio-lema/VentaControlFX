package com.mycompany.ventacontrolfx.presentation.controller.product;

import com.mycompany.ventacontrolfx.domain.model.Category;
import com.mycompany.ventacontrolfx.domain.model.TaxGroup;
import com.mycompany.ventacontrolfx.domain.model.TaxRate;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import java.util.List;

/**
 * Gestor de impuestos y categorías para el formulario de productos.
 * Maneja la herencia de tasas y la sincronización de grupos fiscales.
 */
public class ProductTaxManager {

    private final ServiceContainer container;
    private final ComboBox<Category> cmbCategory;
    private final ComboBox<TaxGroup> cmbTaxGroup;
    private final TextField txtIva;

    public ProductTaxManager(ServiceContainer container, ComboBox<Category> cmbCategory, ComboBox<TaxGroup> cmbTaxGroup, TextField txtIva) {
        this.container = container;
        this.cmbCategory = cmbCategory;
        this.cmbTaxGroup = cmbTaxGroup;
        this.txtIva = txtIva;
    }

    public void init() {
        setupCategoryConverter();
        setupTaxGroupConverter();
        
        cmbCategory.getSelectionModel().selectedItemProperty().addListener((obs, old, nv) -> {
            if (nv != null && nv.getTaxGroupId() != null) {
                selectTaxGroupById(nv.getTaxGroupId());
            }
        });

        cmbTaxGroup.valueProperty().addListener((obs, old, nv) -> {
            if (nv != null) {
                double total = nv.getRates().stream().mapToDouble(TaxRate::getRate).sum();
                txtIva.setText(String.valueOf(total));
                txtIva.setDisable(true);
            } else {
                txtIva.setDisable(false);
            }
        });
    }

    private void setupCategoryConverter() {
        cmbCategory.setConverter(new StringConverter<>() {
            @Override public String toString(Category c) { return c != null ? c.getName() : ""; }
            @Override public Category fromString(String s) { return null; }
        });
    }

    private void setupTaxGroupConverter() {
        cmbTaxGroup.setConverter(new StringConverter<>() {
            @Override public String toString(TaxGroup t) { return t != null ? t.getName() : container.getBundle().getString("product.form.tax_group.legacy"); }
            @Override public TaxGroup fromString(String s) { return null; }
        });
    }

    public void loadData(List<Category> categories, List<TaxGroup> taxGroups) {
        cmbCategory.setItems(FXCollections.observableArrayList(categories));
        cmbTaxGroup.setItems(FXCollections.observableArrayList(taxGroups));
    }

    public void selectTaxGroupById(Integer id) {
        if (id == null) return;
        cmbTaxGroup.getItems().stream()
                .filter(tg -> tg.getId().equals(id))
                .findFirst()
                .ifPresent(cmbTaxGroup::setValue);
    }

    public void selectCategoryById(int id) {
        cmbCategory.getItems().stream()
                .filter(c -> c.getId() == id)
                .findFirst()
                .ifPresent(cmbCategory::setValue);
    }
}

