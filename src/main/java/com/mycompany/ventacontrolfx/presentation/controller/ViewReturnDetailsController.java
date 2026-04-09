package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.Return;
import com.mycompany.ventacontrolfx.domain.model.ReturnDetail;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class ViewReturnDetailsController {

    @FXML private Label lblTitle, lblSubtitle, lblReason;
    @FXML private TableView<ReturnDetail> detailsTable;
    @FXML private TableColumn<ReturnDetail, String> colProduct;
    @FXML private TableColumn<ReturnDetail, Integer> colQuantity;
    @FXML private TableColumn<ReturnDetail, Double> colPrice, colSubtotal;

    public void setData(Return returnRecord) {
        if (returnRecord == null) return;

        if (lblTitle != null) lblTitle.setText("Devoluci\u00f3n #" + returnRecord.getReturnId());
        if (lblSubtitle != null) lblSubtitle.setText("Relacionada con el Ticket #" + returnRecord.getSaleId());
        if (lblReason != null) lblReason.setText(returnRecord.getReason() != null ? returnRecord.getReason() : "Sin motivo registrado");

        if (detailsTable != null) {
            setupTable();
            if (returnRecord.getDetails() != null) {
                detailsTable.setItems(FXCollections.observableArrayList(returnRecord.getDetails()));
            }
        }
    }

    private void setupTable() {
        colProduct.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colQuantity.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("unitPrice"));
        colSubtotal.setCellValueFactory(new PropertyValueFactory<>("subtotal"));

        // Formato moneda
        colPrice.setCellFactory(tc -> new TableCell<ReturnDetail, Double>() {
            @Override protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) setText(null);
                else setText(String.format("%.2f \u20ac", price));
            }
        });
        colSubtotal.setCellFactory(tc -> new TableCell<ReturnDetail, Double>() {
            @Override protected void updateItem(Double subtotal, boolean empty) {
                super.updateItem(subtotal, empty);
                if (empty || subtotal == null) setText(null);
                else setText(String.format("%.2f \u20ac", subtotal));
            }
        });
    }

    @FXML
    private void handleClose() {
        if (lblTitle != null && lblTitle.getScene() != null) {
            ((Stage) lblTitle.getScene().getWindow()).close();
        }
    }
}
