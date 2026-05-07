package com.mycompany.ventacontrolfx.presentation.controller.receipt;

import com.mycompany.ventacontrolfx.application.usecase.SaleUseCase;
import com.mycompany.ventacontrolfx.domain.model.Return;
import com.mycompany.ventacontrolfx.domain.model.ReturnDetail;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Gestiona el panel lateral de detalles de una factura rectificativa.
 */
public class ReturnDetailsManager {

    private final ServiceContainer container;
    private final SaleUseCase saleUseCase;
    
    private final VBox detailsPanel, detailsItemsContainer;
    private final Label lblDetailReturnId, lblDetailSaleId, lblDetailDate, lblDetailReason, lblDetailUser, lblDetailTotal;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public ReturnDetailsManager(
            ServiceContainer container,
            SaleUseCase saleUseCase,
            VBox detailsPanel,
            VBox detailsItemsContainer,
            Label lblDetailReturnId,
            Label lblDetailSaleId,
            Label lblDetailDate,
            Label lblDetailReason,
            Label lblDetailUser,
            Label lblDetailTotal) {
        this.container = container;
        this.saleUseCase = saleUseCase;
        this.detailsPanel = detailsPanel;
        this.detailsItemsContainer = detailsItemsContainer;
        this.lblDetailReturnId = lblDetailReturnId;
        this.lblDetailSaleId = lblDetailSaleId;
        this.lblDetailDate = lblDetailDate;
        this.lblDetailReason = lblDetailReason;
        this.lblDetailUser = lblDetailUser;
        this.lblDetailTotal = lblDetailTotal;
    }

    public void show(Return returnRecord) {
        if (returnRecord == null) return;

        try {
            List<ReturnDetail> details = saleUseCase.getReturnDetails(returnRecord.getReturnId());
            returnRecord.setDetails(details);

            lblDetailReturnId.setText(container.getBundle().getString("returns.detail.id_prefix") + returnRecord.getFullReference());
            lblDetailSaleId.setText(container.getBundle().getString("returns.detail.ticket_prefix") + returnRecord.getSaleId());
            lblDetailDate.setText(returnRecord.getReturnDatetime() != null ? returnRecord.getReturnDatetime().format(formatter) : "-");
            lblDetailReason.setText(returnRecord.getReason() != null ? returnRecord.getReason() : "-");
            lblDetailUser.setText(returnRecord.getUserName() != null ? returnRecord.getUserName() : "-");
            lblDetailTotal.setText(String.format("%.2f \u20ac", returnRecord.getTotalRefunded()));

            detailsItemsContainer.getChildren().clear();
            for (ReturnDetail detail : details) {
                detailsItemsContainer.getChildren().add(createItemRow(detail));
                Separator sep = new Separator();
                sep.setStyle("-fx-background-color: #f1f5f9; -fx-opacity: 0.5;");
                detailsItemsContainer.getChildren().add(sep);
            }

            detailsPanel.setVisible(true);
            detailsPanel.setManaged(true);
        } catch (Exception e) {
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("returns.error.details") + ": " + e.getMessage());
        }
    }

    private VBox createItemRow(ReturnDetail detail) {
        VBox itemBox = new VBox(2);
        itemBox.setStyle("-fx-background-color: transparent;");

        HBox topRow = new HBox();
        Label nameLabel = new Label(detail.getProductName() != null ? translateDynamic(detail.getProductName()) : "ID: " + detail.getProductId());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-font-size: 13px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label subtotalLabel = new Label(String.format("%.2f \u20ac", detail.getSubtotal()));
        subtotalLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");
        topRow.getChildren().addAll(nameLabel, spacer, subtotalLabel);

        Label qtyLabel = new Label(detail.getQuantity() + " x " + String.format("%.2f \u20ac", detail.getUnitPrice()));
        qtyLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        itemBox.getChildren().addAll(topRow, qtyLabel);
        return itemBox;
    }

    private String translateDynamic(String text) {
        if (text == null || text.isBlank()) return text;
        if (container.getBundle().containsKey(text)) return container.getBundle().getString(text);
        return text;
    }

    public void hide() {
        detailsPanel.setVisible(false);
        detailsPanel.setManaged(false);
    }
}

