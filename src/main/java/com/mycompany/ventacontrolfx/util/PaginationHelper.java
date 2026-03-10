package com.mycompany.ventacontrolfx.util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Utilidad para gestionar la paginación y limitación de filas en TableViews.
 */
public class PaginationHelper<T> {

    private final TableView<T> table;
    private final ComboBox<Integer> comboLimit;
    private final Label lblCount;
    private final String entityNamePlural;

    private List<T> allItems = new ArrayList<>();

    public PaginationHelper(TableView<T> table, ComboBox<Integer> comboLimit, Label lblCount, String entityNamePlural) {
        this.table = table;
        this.comboLimit = comboLimit;
        this.lblCount = lblCount;
        this.entityNamePlural = entityNamePlural;
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
            String text = String.format("🔍 Mostrando %d de %d %s", showing, total, entityNamePlural);
            lblCount.setText(text);
        }
    }
}
