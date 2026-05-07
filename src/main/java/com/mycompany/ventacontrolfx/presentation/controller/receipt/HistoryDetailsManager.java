package com.mycompany.ventacontrolfx.presentation.controller.receipt;

import com.mycompany.ventacontrolfx.application.usecase.SaleUseCase;
import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.domain.model.SaleDetail;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import java.time.format.DateTimeFormatter;

/**
 * Gestiona el panel lateral de detalles de una venta en el historial.
 */
public class HistoryDetailsManager {

    private final ServiceContainer container;
    private final SaleUseCase saleUseCase;
    
    // UI References
    private final VBox detailsPanel, detailsItemsContainer;
    private final Label lblSaleId, lblSaleFullDate, lblPaymentMethod, lblTotalAmountDetail, lblReturnBadge;
    private final Button btnReturn, btnResendAeat;

    private final DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public HistoryDetailsManager(
            ServiceContainer container,
            SaleUseCase saleUseCase,
            VBox detailsPanel,
            VBox detailsItemsContainer,
            Label lblSaleId,
            Label lblSaleFullDate,
            Label lblPaymentMethod,
            Label lblTotalAmountDetail,
            Label lblReturnBadge,
            Button btnReturn,
            Button btnResendAeat) {
        this.container = container;
        this.saleUseCase = saleUseCase;
        this.detailsPanel = detailsPanel;
        this.detailsItemsContainer = detailsItemsContainer;
        this.lblSaleId = lblSaleId;
        this.lblSaleFullDate = lblSaleFullDate;
        this.lblPaymentMethod = lblPaymentMethod;
        this.lblTotalAmountDetail = lblTotalAmountDetail;
        this.lblReturnBadge = lblReturnBadge;
        this.btnReturn = btnReturn;
        this.btnResendAeat = btnResendAeat;
    }

    public void showDetails(Sale sale) {
        detailsPanel.setVisible(true);
        detailsPanel.setManaged(true);
        lblSaleId.setText(container.getBundle().getString("history.detail.ticket") + " #"
                + String.format("%04d", sale.getSaleId()));

        container.getAsyncManager().runAsyncTask(() -> {
            try {
                if (sale.getDetails() == null || sale.getDetails().isEmpty()) {
                    return saleUseCase.getSaleDetails(sale.getSaleId());
                }
                return sale;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, result -> {
            Sale detailedSale = (Sale) result;
            if (detailedSale != null) {
                updateDetailsUI(detailedSale);
            }
        }, null);
    }

    private void updateDetailsUI(Sale sale) {
        boolean hasAnyReturned = sale.getDetails().stream().anyMatch(d -> d.getReturnedQuantity() > 0);
        boolean allReturned = !sale.getDetails().isEmpty()
                && sale.getDetails().stream().allMatch(d -> d.getReturnedQuantity() >= d.getQuantity());

        if (sale.isReturn() || allReturned) {
            lblReturnBadge.setText(container.getBundle().getString("history.status.returned"));
            lblReturnBadge.setVisible(true);
            lblReturnBadge.getStyleClass().setAll("badge-danger");
        } else if (hasAnyReturned) {
            lblReturnBadge.setText(container.getBundle().getString("history.status.partial"));
            lblReturnBadge.setVisible(true);
            lblReturnBadge.getStyleClass().setAll("badge-warning");
        } else {
            lblReturnBadge.setVisible(false);
        }

        btnReturn.setDisable(sale.isReturn());

        String fStatus = sale.getFiscalStatus();
        boolean needsResend = "PENDING".equals(fStatus) || "REJECTED".equals(fStatus);
        if (btnResendAeat != null) {
            btnResendAeat.setVisible(needsResend);
            btnResendAeat.setManaged(needsResend);
        }

        lblSaleFullDate.setText(sale.getSaleDateTime().format(fullFormatter) + "\n"
                + container.getBundle().getString("receipt.attended_by") + ": " + sale.getUserName() + "\n"
                + "Cliente: "
                + (sale.getCustomerNameSnapshot() != null ? sale.getCustomerNameSnapshot() : "Consumidor Final") + "\n"
                + "NIF: " + (sale.getCustomerNifSnapshot() != null ? sale.getCustomerNifSnapshot() : "---"));

        String methodEmoji = container.getBundle().getString("payment.method.card")
                .equalsIgnoreCase(sale.getPaymentMethod()) ? "\ud83d\udcb3 " : "\ud83d\udcb5 ";
        lblPaymentMethod.setText(
                methodEmoji + (sale.getPaymentMethod().contains("Mixed") || sale.getPaymentMethod().contains("Mixto")
                        ? container.getBundle().getString("payment.method.mixed")
                        : sale.getPaymentMethod()));

        lblTotalAmountDetail.setText(String.format("%.2f \u20ac", sale.getTotal()));
        lblTotalAmountDetail.getStyleClass().removeAll("text-total", "text-error");
        lblTotalAmountDetail.getStyleClass().add(sale.isReturn() ? "text-error" : "text-total");

        detailsItemsContainer.getChildren().clear();

        // 1. Status AEAT / VeriFactu
        if (fStatus != null && !fStatus.isEmpty()) {
            String textStatus = "PENDING".equals(fStatus) ? "⏳ Pendiente envío AEAT"
                    : ("ACCEPTED".equals(fStatus)
                            ? "✅ VeriFactu OK ("
                                    + (sale.getAeatSubmissionId() != null ? sale.getAeatSubmissionId() : "S/D") + ")"
                            : ("REJECTED".equals(fStatus) ? "❌ Error de validación: " + sale.getFiscalMsg() : fStatus));
            Label lblFiscalStatus = new Label(textStatus);
            lblFiscalStatus.getStyleClass().add("badge-base");
            if ("ACCEPTED".equals(fStatus)) lblFiscalStatus.getStyleClass().add("badge-success");
            else if ("REJECTED".equals(fStatus)) lblFiscalStatus.getStyleClass().add("badge-danger");
            else lblFiscalStatus.getStyleClass().add("badge-warning");
            detailsItemsContainer.getChildren().add(lblFiscalStatus);
        }

        // 2. Items
        for (SaleDetail detail : sale.getDetails()) {
            detailsItemsContainer.getChildren().add(createDetailRow(detail));
        }
    }

    private HBox createDetailRow(SaleDetail detail) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 8 0; -fx-border-color: #f8f9fa; -fx-border-width: 0 0 1 0;");

        VBox nameBox = new VBox(2);
        Label nameLabel = new Label(detail.getProductName());
        String color = detail.getReturnedQuantity() > 0 ? "#f39c12" : "#2c3e50";
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: " + color + ";");

        Label qtyLabel = new Label("\ud83d\udce6 " + detail.getQuantity() + " un. x "
                + String.format("%.2f", detail.getUnitPrice()) + " \u20ac");
        qtyLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6;");
        nameBox.getChildren().addAll(nameLabel, qtyLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label priceLabel = new Label("\ud83d\udcb0 " + String.format("%.2f \u20ac", detail.getLineTotal()));
        priceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");

        row.getChildren().addAll(nameBox, spacer, priceLabel);
        return row;
    }

    public void hide() {
        detailsPanel.setVisible(false);
        detailsPanel.setManaged(false);
    }
}

