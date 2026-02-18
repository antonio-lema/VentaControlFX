package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.CartItem;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class ReceiptController {

    @FXML
    private VBox receiptContent; // Based on FXML edit

    @FXML
    private Label lblTicketTitle;
    @FXML
    private Label lblDate;
    @FXML
    private VBox itemsContainer;
    @FXML
    private Label lblSubtotal;
    @FXML
    private Label lblVat;
    @FXML
    private Label lblTotal;
    @FXML
    private Label lblPaid;
    @FXML
    private Label lblChange;
    @FXML
    private Label lblPaymentMethod;
    @FXML
    private Label lblTotalRight;
    @FXML
    private Label lblChangeRight;
    @FXML
    private VBox totalsContainer;
    @FXML
    private VBox barcodeSection;
    @FXML
    private HBox barcodeContainer;
    @FXML
    private Label lblBarcodeValue;
    @FXML
    private Label lblGiftIndicator;
    @FXML
    private Label lblPVPHeader;
    @FXML
    private Label lblTotalHeader;
    @FXML
    private VBox paymentInfoContainer;
    @FXML
    private Button btnGiftTicket;
    @FXML
    private Label lblGiftIcon;

    private Runnable onNewSale;
    private Runnable onBack;
    private boolean isGiftMode = false;

    @FXML
    private void handlePrint() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/view/print_preview.fxml"));
            javafx.scene.Parent root = loader.load();

            PrintPreviewController controller = loader.getController();

            // Extract data from current view or passing it again?
            // Since we don't have the original 'items' list readily available as a field,
            // let's promote it or reconstruct it.
            // Wait, we do not store 'items' in a field in setReceiptData. We should have
            // stored it.
            // Let's modify setReceiptData to store items and total.

            if (this.currentItems != null) {
                controller.setReceiptData(this.currentItems, this.currentTotal, this.currentPaid,
                        this.currentPaymentMethod, this.isGiftMode);
            }

            Stage stage = new Stage();
            stage.setTitle("Vista Previa de Impresión");
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.setScene(new javafx.scene.Scene(root));
            stage.showAndWait();

        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private List<CartItem> currentItems;
    private double currentTotal;
    private double currentPaid;
    private String currentPaymentMethod;

    public void setReceiptData(List<CartItem> items, double total, double paid, double change, String paymentMethod,
            Runnable onNewSale, Runnable onBack) {
        this.currentItems = items;
        this.currentTotal = total;
        this.currentPaid = paid;
        this.currentPaymentMethod = paymentMethod;

        this.onNewSale = onNewSale;
        this.onBack = onBack;

        // ... existing code ...

        // Date and Time
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM, yyyy 'Hora:' HH:mm:ss");
        lblDate.setText("Fecha: " + now.format(formatter) + " Caja: 01");

        // Random Ticket Number (for demo)
        int randomNum = 1000 + (int) (Math.random() * 9000);
        String prefix = isGiftMode ? "Ticket regalo Nº: 01/" : "Factura simplificada Nº: 01/";
        lblTicketTitle.setText(prefix + randomNum);

        // Items
        itemsContainer.getChildren().clear();
        for (CartItem item : items) {
            addItemRow(item);
        }

        // Totals Visibility
        totalsContainer.setVisible(!isGiftMode);
        totalsContainer.setManaged(!isGiftMode);
        barcodeSection.setVisible(false);
        barcodeSection.setManaged(false);

        // Indicator & Headers Visibility
        lblGiftIndicator.setVisible(isGiftMode);
        lblGiftIndicator.setManaged(isGiftMode);
        lblPVPHeader.setVisible(!isGiftMode);
        lblPVPHeader.setManaged(!isGiftMode);
        lblTotalHeader.setVisible(!isGiftMode);
        lblTotalHeader.setManaged(!isGiftMode);
        paymentInfoContainer.setVisible(!isGiftMode);
        paymentInfoContainer.setManaged(!isGiftMode);

        // Sidebar Button Styling
        if (isGiftMode) {
            btnGiftTicket.setStyle(
                    "-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-background-radius: 25; -fx-font-weight: bold; -fx-padding: 8 20; -fx-cursor: hand;");
            lblGiftIcon.setStyle("-fx-font-size: 16; -fx-text-fill: white;");
        } else {
            btnGiftTicket.setStyle(
                    "-fx-background-color: white; -fx-text-fill: #1a73e8; -fx-border-color: #1a73e8; -fx-border-radius: 25; -fx-border-width: 1; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 25; -fx-cursor: hand;");
            lblGiftIcon.setStyle("-fx-font-size: 16; -fx-text-fill: #1a73e8;");
        }

        // Totals calculations
        double subtotal = total / 1.21;
        double vat = total - subtotal;

        lblSubtotal.setText(String.format("%.2f €", subtotal));
        lblVat.setText(String.format("%.2f €", vat));
        lblTotal.setText(String.format("%.2f €", total));

        lblPaid.setText(String.format("%.2f €", paid));
        lblChange.setText(String.format("%.2f €", change));
        lblPaymentMethod.setText(paymentMethod);

        // Right Side
        lblTotalRight.setText(String.format("%.2f €", total));
        lblChangeRight.setText(String.format("%.2f €", change));
    }

    @FXML
    private void handleGiftTicket() {
        this.isGiftMode = !this.isGiftMode;
        setReceiptData(currentItems, currentTotal, currentPaid,
                Double.parseDouble(lblChange.getText().replace(" €", "").replace(",", ".")), currentPaymentMethod,
                onNewSale, onBack);
    }

    private void generateBarcode(String code) {
        barcodeContainer.getChildren().clear();
        lblBarcodeValue.setText("01/" + code);
        for (int i = 0; i < 40; i++) {
            Rectangle rect = new Rectangle();
            rect.setHeight(40);
            rect.setWidth(1 + (int) (Math.random() * 3));
            rect.setFill(Color.BLACK);
            barcodeContainer.getChildren().add(rect);
        }
    }

    private void addItemRow(CartItem item) {
        HBox row = new HBox(5);
        row.setStyle("-fx-border-color: transparent transparent #eee transparent; -fx-padding: 2 0 2 0;");

        Label lblDesc = new Label(item.getProduct().getName());
        lblDesc.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(lblDesc, Priority.ALWAYS);
        lblDesc.setStyle("-fx-font-size: 10px; -fx-text-fill: black;");

        Label lblQty = new Label(String.valueOf(item.getQuantity()));
        lblQty.setPrefWidth(30);
        lblQty.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-size: 10px; -fx-text-fill: black;");

        row.getChildren().addAll(lblDesc, lblQty);

        if (!isGiftMode) {
            Label lblPrice = new Label(String.format("%.2f", item.getProduct().getPrice()));
            lblPrice.setPrefWidth(55);
            lblPrice.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-size: 10px; -fx-text-fill: black;");

            Label lblTotal = new Label(String.format("%.2f €", item.getTotal()));
            lblTotal.setPrefWidth(55);
            lblTotal.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-size: 10px; -fx-text-fill: black;");

            row.getChildren().addAll(lblPrice, lblTotal);
        }

        itemsContainer.getChildren().add(row);
    }

    @FXML
    private void handleNewSale() {
        if (onNewSale != null) {
            onNewSale.run();
        }
        closeWindow();
    }

    @FXML
    private void handleBack() {
        if (onBack != null) {
            onBack.run();
        }
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) lblTotal.getScene().getWindow();
        stage.close();
    }
}
