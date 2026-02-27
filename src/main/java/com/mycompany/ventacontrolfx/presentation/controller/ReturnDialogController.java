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
        lblTicketInfo.setText("Seleccione los productos a devolver del ticket #" + sale.getSaleId());

        for (SaleDetail detail : sale.getDetails()) {
            int availableToReturn = detail.getQuantity() - detail.getReturnedQuantity();
            if (availableToReturn <= 0)
                continue;

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10));
            row.setStyle(
                    "-fx-background-color: white; -fx-background-radius: 8; -fx-border-color: #f1f5f9; -fx-border-radius: 8;");

            CheckBox cb = new CheckBox();
            Label nameLabel = new Label(detail.getProductName());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #334155;");
            nameLabel.setWrapText(true);
            HBox.setHgrow(nameLabel, Priority.ALWAYS);
            nameLabel.setMaxWidth(Double.MAX_VALUE);

            Label soldLabel = new Label(String.valueOf(detail.getQuantity()));
            soldLabel.setPrefWidth(70);
            soldLabel.setAlignment(Pos.CENTER);

            Label returnedLabel = new Label(String.valueOf(detail.getReturnedQuantity()));
            returnedLabel.setPrefWidth(70);
            returnedLabel.setAlignment(Pos.CENTER);
            returnedLabel.setStyle("-fx-text-fill: #ef4444;");

            Spinner<Integer> spinner = new Spinner<>(1, availableToReturn, 1);
            spinner.setPrefWidth(90);
            spinner.setDisable(true);
            spinner.getStyleClass().add(Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);

            cb.selectedProperty().addListener((obs, oldVal, newVal) -> {
                spinner.setDisable(!newVal);
                row.setStyle(newVal
                        ? "-fx-background-color: #f0f9ff; -fx-border-color: #bae6fd; -fx-background-radius: 8; -fx-border-radius: 8;"
                        : "-fx-background-color: white; -fx-border-color: #f1f5f9; -fx-background-radius: 8; -fx-border-radius: 8;");
            });

            checkBoxes.put(detail.getDetailId(), cb);
            spinners.put(detail.getDetailId(), spinner);

            row.getChildren().addAll(cb, nameLabel, soldLabel, returnedLabel, spinner);
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
