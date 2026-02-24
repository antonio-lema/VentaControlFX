package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.Sale;
import com.mycompany.ventacontrolfx.model.SaleDetail;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Pair;
import java.util.HashMap;
import java.util.Map;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import com.mycompany.ventacontrolfx.util.AlertUtil;

public class ReturnDialogController {

    @FXML
    private Label lblTicketInfo;
    @FXML
    private VBox itemsContainer;
    @FXML
    private TextArea txtReason;

    private Sale sale;
    private boolean confirmClicked = false;
    private Map<Integer, Integer> resultItems = new HashMap<>();
    private String reason;

    private Map<Integer, Spinner<Integer>> spinners = new HashMap<>();
    private Map<Integer, CheckBox> checkBoxes = new HashMap<>();

    public void init(Sale sale) {
        this.sale = sale;
        lblTicketInfo.setText("Seleccione los productos a devolver del ticket #" + sale.getSaleId());

        for (SaleDetail detail : sale.getDetails()) {
            int availableToReturn = detail.getQuantity() - detail.getReturnedQuantity();
            if (availableToReturn <= 0)
                continue;

            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 10, 10, 10));
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
            soldLabel.setStyle("-fx-text-fill: #64748b;");

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
                if (newVal) {
                    row.setStyle(
                            "-fx-background-color: #f0f9ff; -fx-border-color: #bae6fd; -fx-background-radius: 8; -fx-border-radius: 8;");
                } else {
                    row.setStyle(
                            "-fx-background-color: white; -fx-border-color: #f1f5f9; -fx-background-radius: 8; -fx-border-radius: 8;");
                }
            });

            checkBoxes.put(detail.getDetailId(), cb);
            spinners.put(detail.getDetailId(), spinner);

            row.getChildren().addAll(cb, nameLabel, soldLabel, returnedLabel, spinner);
            itemsContainer.getChildren().add(row);
        }
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    @FXML
    private void handleConfirm() {
        resultItems.clear();
        for (Map.Entry<Integer, CheckBox> entry : checkBoxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                int detailId = entry.getKey();
                int qty = spinners.get(detailId).getValue();
                resultItems.put(detailId, qty);
            }
        }

        if (resultItems.isEmpty()) {
            AlertUtil.showWarning("Aviso", "Debe seleccionar al menos un producto para devolver.");
            return;
        }

        this.reason = txtReason.getText();
        if (this.reason == null || this.reason.trim().isEmpty()) {
            AlertUtil.showWarning("Aviso", "Debe ingresar un motivo para la devolución.");
            return;
        }

        this.confirmClicked = true;
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) itemsContainer.getScene().getWindow();
        stage.close();
    }

    public boolean isConfirmClicked() {
        return confirmClicked;
    }

    public Pair<String, Map<Integer, Integer>> getResult() {
        return new Pair<>(reason, resultItems);
    }
}
