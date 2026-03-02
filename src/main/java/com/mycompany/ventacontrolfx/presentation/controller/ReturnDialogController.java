package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.domain.model.SaleDetail;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class ReturnDialogController {

    @FXML
    private Label lblTicketInfo;
    @FXML
    private VBox itemsContainer;
    @FXML
    private TextArea txtReason;

    private Map<Integer, Spinner<Integer>> spinners = new HashMap<>();
    private Map<Integer, CheckBox> checkBoxes = new HashMap<>();
    private BiConsumer<String, Map<Integer, Integer>> onSuccess;

    public void init(Sale sale) {
        if (sale == null || sale.getDetails() == null)
            return;

        lblTicketInfo.setText("Seleccione los productos a devolver del ticket #" + sale.getSaleId());
        itemsContainer.getChildren().clear();

        for (SaleDetail detail : sale.getDetails()) {
            int availableToReturn = detail.getQuantity() - detail.getReturnedQuantity();
            if (availableToReturn <= 0)
                continue;

            HBox row = new HBox(15);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("return-item-row");

            CheckBox cb = new CheckBox();
            cb.getStyleClass().add("return-checkbox");

            VBox info = new VBox(2);
            Label nameLabel = new Label(detail.getProductName());
            nameLabel.getStyleClass().add("return-product-name");
            nameLabel.setWrapText(true);

            Label priceLabel = new Label(String.format("%.2f € / ud.", detail.getUnitPrice()));
            priceLabel.getStyleClass().add("return-product-price");
            info.getChildren().addAll(nameLabel, priceLabel);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label soldLabel = new Label("Vend: " + detail.getQuantity());
            soldLabel.getStyleClass().add("return-count-label");

            Label returnedLabel = new Label("Dev: " + detail.getReturnedQuantity());
            returnedLabel.getStyleClass().add("return-count-label");
            returnedLabel.getStyleClass().add("text-danger");

            Spinner<Integer> spinner = new Spinner<>(1, availableToReturn, 1);
            spinner.setPrefWidth(100);
            spinner.setDisable(true);
            spinner.getStyleClass().add("modern-spinner");

            cb.selectedProperty().addListener((obs, oldVal, newVal) -> {
                spinner.setDisable(!newVal);
                if (newVal)
                    row.getStyleClass().add("row-selected");
                else
                    row.getStyleClass().remove("row-selected");
            });

            checkBoxes.put(detail.getDetailId(), cb);
            spinners.put(detail.getDetailId(), spinner);

            row.getChildren().addAll(cb, info, soldLabel, returnedLabel, spinner);
            itemsContainer.getChildren().add(row);
        }
    }

    public void setOnSuccess(BiConsumer<String, Map<Integer, Integer>> callback) {
        this.onSuccess = callback;
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    @FXML
    private void handleConfirm() {
        Map<Integer, Integer> resultItems = new HashMap<>();
        for (Map.Entry<Integer, CheckBox> entry : checkBoxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                resultItems.put(entry.getKey(), spinners.get(entry.getKey()).getValue());
            }
        }

        if (resultItems.isEmpty()) {
            AlertUtil.showWarning("Aviso", "Debe seleccionar al menos un producto.");
            return;
        }

        String reason = txtReason.getText();
        if (reason == null || reason.trim().isEmpty()) {
            AlertUtil.showWarning("Aviso", "Debe ingresar un motivo.");
            return;
        }

        if (onSuccess != null) {
            onSuccess.accept(reason, resultItems);
        }
        closeWindow();
    }

    private void closeWindow() {
        ((Stage) itemsContainer.getScene().getWindow()).close();
    }
}
