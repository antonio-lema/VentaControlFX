package com.mycompany.ventacontrolfx.presentation.controller.vat;

import com.mycompany.ventacontrolfx.domain.model.TaxGroup;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.util.List;
import java.util.function.Consumer;

/**
 * Gestor de la lista lateral de grupos de impuestos.
 */
public class TaxGroupListManager {

    private final ServiceContainer container;
    private final ListView<TaxGroup> listView;
    private final ObservableList<TaxGroup> data = FXCollections.observableArrayList();

    public TaxGroupListManager(ServiceContainer container, ListView<TaxGroup> listView) {
        this.container = container;
        this.listView = listView;
    }

    public void setup(Consumer<TaxGroup> onSelectionChanged) {
        listView.setCellFactory(lv -> new TaxGroupCell());
        listView.setItems(data);
        listView.getSelectionModel().selectedItemProperty().addListener((obs, old, nv) -> onSelectionChanged.accept(nv));
    }

    public void update(List<TaxGroup> groups) {
        data.setAll(groups);
    }

    public void select(TaxGroup group) {
        listView.getSelectionModel().select(group);
    }

    /**
     * Celda personalizada para representar un grupo de impuestos de forma premium.
     */
    private class TaxGroupCell extends ListCell<TaxGroup> {
        @Override
        protected void updateItem(TaxGroup item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                VBox box = new VBox(2);
                Label lblName = new Label(item.getName());
                lblName.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
                
                double total = item.getRates().stream().mapToDouble(r -> r.getRate()).sum();
                Label lblRates = new Label(item.getRates().size() + " tasas - Total: " + String.format("%.2f%%", total));
                lblRates.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

                box.getChildren().addAll(lblName, lblRates);
                setGraphic(box);
            }
        }
    }
}

