package com.mycompany.ventacontrolfx.presentation.controller.dialog;

import com.mycompany.ventacontrolfx.domain.model.Return;
import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.domain.model.SaleDetail;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class ReturnDialogController {

    @FXML
    private Label lblTicketInfo;
    @FXML
    private VBox itemsContainer;
    @FXML
    private TextArea txtReason;
    @FXML
    private Label lblTotalRefund;
    @FXML
    private Label lblCashRefund;
    @FXML
    private Label lblCardRefund;
    @FXML
    private javafx.scene.layout.HBox boxBrokenDownRefund;

    private Sale currentSale;
    private List<Return> prevReturns;
    private Map<Integer, Spinner<Integer>> spinners = new HashMap<>();
    private Map<Integer, CheckBox> checkBoxes = new HashMap<>();
    private BiConsumer<String, Map<Integer, Integer>> onSuccess;

    public void init(Sale sale, com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer container) {
        if (sale == null || sale.getDetails() == null)
            return;

        this.currentSale = sale;
        try {
            // Cargar histórico de devoluciones para calcular el sobrante de
            // efectivo/tarjeta actual
            if (container != null) {
                this.prevReturns = container.getSaleUseCase().getReturnsBySaleId(sale.getSaleId());
            }
        } catch (Exception e) {
            this.prevReturns = new java.util.ArrayList<>();
        }

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

            double actualGrossPrice = detail.getLineTotal() / detail.getQuantity();
            Label priceLabel = new Label(String.format("%.2f € / ud.", actualGrossPrice));
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
                updateRefundSummary();
            });

            spinner.valueProperty().addListener((obs, oldV, newV) -> updateRefundSummary());

            checkBoxes.put(detail.getDetailId(), cb);
            spinners.put(detail.getDetailId(), spinner);

            row.getChildren().addAll(cb, info, soldLabel, returnedLabel, spinner);
            itemsContainer.getChildren().add(row);
        }
        updateRefundSummary();
    }

    private void updateRefundSummary() {
        double totalToRefund = 0;
        for (SaleDetail d : currentSale.getDetails()) {
            CheckBox cb = checkBoxes.get(d.getDetailId());
            if (cb != null && cb.isSelected()) {
                int qty = spinners.get(d.getDetailId()).getValue();
                double unitPrice = d.getLineTotal() / d.getQuantity();
                totalToRefund += qty * unitPrice;
            }
        }

        lblTotalRefund.setText(String.format("%.2f €", totalToRefund));

        // Mostrar desglose proporcional si la venta fue MIXTA
        if (currentSale.getCashAmount() > 0 && currentSale.getCardAmount() > 0) {
            boxBrokenDownRefund.setVisible(true);
            boxBrokenDownRefund.setManaged(true);

            double currentGrossTotal = currentSale.getTotal() + currentSale.getDiscountAmount();
            double cashRatio = (currentGrossTotal > 0) ? currentSale.getCashAmount() / currentGrossTotal : 1.0;

            double cashToRefund = Math.round((totalToRefund * cashRatio) * 100.0) / 100.0;
            double cardToRefund = Math.round((totalToRefund - cashToRefund) * 100.0) / 100.0;

            // Cap contra histórico
            if (prevReturns != null) {
                double totalCashReturnedSoFar = prevReturns.stream().mapToDouble(r -> r.getCashAmount()).sum();
                double totalCardReturnedSoFar = prevReturns.stream().mapToDouble(r -> r.getCardAmount()).sum();

                double availableCash = Math.max(0, currentSale.getCashAmount() - totalCashReturnedSoFar);
                double availableCard = Math.max(0, currentSale.getCardAmount() - totalCardReturnedSoFar);

                if (cashToRefund > availableCash) {
                    double excess = cashToRefund - availableCash;
                    cashToRefund = availableCash;
                    cardToRefund += excess;
                }
                if (cardToRefund > availableCard) {
                    double excess = cardToRefund - availableCard;
                    cardToRefund = availableCard;
                    cashToRefund += excess;
                }

                cashToRefund = Math.min(cashToRefund, availableCash);
                cardToRefund = Math.min(cardToRefund, availableCard);
            }

            lblCashRefund.setText(String.format("%.2f €", cashToRefund));
            lblCardRefund.setText(String.format("%.2f €", cardToRefund));
        } else {
            boxBrokenDownRefund.setVisible(false);
            boxBrokenDownRefund.setManaged(false);
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

