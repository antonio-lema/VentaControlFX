package com.mycompany.ventacontrolfx.util;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Utilidad para gestionar la paginaci\u00c3\u00b3n y limitaci\u00c3\u00b3n de filas en TableViews.
 */
public class PaginationHelper<T> {

    private final TableView<T> table;
    private final ComboBox<Integer> comboLimit;
    private final Label lblCount;
    private final String entityNamePlural;
    private List<T> allItems = new ArrayList<>();
    private java.util.ResourceBundle bundle;

    public PaginationHelper(TableView<T> table, ComboBox<Integer> comboLimit, Label lblCount, String entityNamePlural) {
        this(table, comboLimit, lblCount, entityNamePlural, null);
    }

    public PaginationHelper(TableView<T> table, ComboBox<Integer> comboLimit, Label lblCount, String entityNamePlural,
            java.util.ResourceBundle bundle) {
        this.table = table;
        this.comboLimit = comboLimit;
        this.lblCount = lblCount;
        this.entityNamePlural = entityNamePlural;
        this.bundle = bundle;
        setup();
    }

    private void setup() {
        if (comboLimit != null) {
            comboLimit.setItems(FXCollections.observableArrayList(10, 25, 50, 100, 500));
            comboLimit.setValue(25);
            comboLimit.setOnAction(e -> applyFilter());
        }
    }

    public void setData(List<T> items) {
        this.allItems = items != null ? items : new ArrayList<>();
        applyFilter();
    }

    private void applyFilter() {
        if (allItems == null)
            return;

        int limit = (comboLimit != null) ? comboLimit.getValue() : allItems.size();

        List<T> filtered = allItems.stream()
                .limit(limit)
                .collect(Collectors.toList());

        table.setItems(FXCollections.observableArrayList(filtered));
        updateCountLabel(filtered.size(), allItems.size());
    }

    private void updateCountLabel(int showing, int total) {
        if (lblCount != null) {
            String format = (bundle != null && bundle.containsKey("pagination.showing_simple"))
                    ? bundle.getString("pagination.showing_simple")
                    : "\u00f0\u0178\u201d\u008d Mostrando %d de %d %s";
            lblCount.setText(String.format(format, showing, total, entityNamePlural));
        }
    }
}
