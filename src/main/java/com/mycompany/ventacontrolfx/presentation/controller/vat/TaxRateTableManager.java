package com.mycompany.ventacontrolfx.presentation.controller.vat;

import com.mycompany.ventacontrolfx.domain.model.TaxRate;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor de la tabla dinámica de tasas impositivas.
 * Maneja la creación de filas, eliminación y cálculo de totales.
 */
public class TaxRateTableManager {

    private final VBox container;
    private final Label lblTotal;
    private final ObservableList<TaxRateRow> rows = FXCollections.observableArrayList();

    public TaxRateTableManager(VBox container, Label lblTotal) {
        this.container = container;
        this.lblTotal = lblTotal;
    }

    public void clear() {
        rows.clear();
        container.getChildren().clear();
        updateTotal();
    }

    public void addRow(TaxRate initial) {
        TaxRateRow row = new TaxRateRow(initial);
        rows.add(row);
        container.getChildren().add(row.getView());
        updateTotal();
    }

    public void updateTotal() {
        double total = rows.stream()
                .mapToDouble(row -> {
                    try { return Double.parseDouble(row.txtRate.getText().replace(",", ".")); }
                    catch (Exception e) { return 0.0; }
                }).sum();
        lblTotal.setText(String.format("%.2f%%", total));
    }

    public List<TaxRate> getRates() {
        List<TaxRate> rates = new ArrayList<>();
        for (TaxRateRow row : rows) {
            String name = row.txtName.getText().trim();
            String val = row.txtRate.getText().replace(",", ".").trim();
            if (!name.isEmpty() && !val.isEmpty()) {
                TaxRate tr = new TaxRate();
                tr.setName(name);
                tr.setRate(Double.parseDouble(val));
                tr.setTaxType(row.cmbType.getValue());
                rates.add(tr);
            }
        }
        return rates;
    }

    /**
     * Componente visual interno para una fila de tasa.
     */
    private class TaxRateRow {
        private final HBox view;
        private final TextField txtName, txtRate;
        private final ComboBox<TaxRate.TaxType> cmbType;

        public TaxRateRow(TaxRate initial) {
            view = new HBox(10); view.setAlignment(Pos.CENTER_LEFT); view.setPadding(new Insets(5, 0, 5, 0));

            txtName = new TextField(); txtName.setPromptText("Nombre (ej. IVA)");
            txtName.getStyleClass().add("modern-input-field"); HBox.setHgrow(txtName, Priority.ALWAYS);

            txtRate = new TextField(); txtRate.setPromptText("0.00"); txtRate.setPrefWidth(80);
            txtRate.getStyleClass().add("modern-input-field");
            txtRate.textProperty().addListener((obs, o, n) -> updateTotal());

            cmbType = new ComboBox<>(FXCollections.observableArrayList(TaxRate.TaxType.values()));
            cmbType.setPrefWidth(120); cmbType.getStyleClass().add("modern-combo-box");
            cmbType.setConverter(new StringConverter<TaxRate.TaxType>() {
                @Override public String toString(TaxRate.TaxType t) { return t != null ? t.name() : ""; }
                @Override public TaxRate.TaxType fromString(String s) { return null; }
            });
            cmbType.setValue(TaxRate.TaxType.VAT);

            Button btnRemove = new Button();
            btnRemove.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.TRASH));
            btnRemove.getStyleClass().add("action-button-danger-outline");
            btnRemove.setOnAction(e -> { rows.remove(this); container.getChildren().remove(view); updateTotal(); });

            if (initial != null) {
                txtName.setText(initial.getName());
                txtRate.setText(String.valueOf(initial.getRate()));
                cmbType.setValue(initial.getTaxType());
            }

            view.getChildren().addAll(cmbType, txtName, txtRate, btnRemove);
        }

        public HBox getView() { return view; }
    }
}

