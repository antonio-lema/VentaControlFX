package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.CartItem;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class PrintPreviewController {

    @FXML
    private VBox paperSheet;
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
    private Label lblPaymentMethod;
    @FXML
    private ComboBox<Printer> cmbPrinters;
    @FXML
    private HBox barcodeContainer;
    @FXML
    private VBox totalsContainer;
    @FXML
    private VBox barcodeSection;
    @FXML
    private Label lblBarcodeValue;
    @FXML
    private Button btnGiftTicket;
    @FXML
    private Label lblGiftIcon;
    @FXML
    private Label lblGiftIndicator;
    @FXML
    private Label lblPVPHeader;
    @FXML
    private Label lblTotalHeader;
    @FXML
    private VBox paymentInfoContainer;

    private boolean isGiftMode = false;

    public void setReceiptData(List<CartItem> items, double total, double paid, String paymentMethod) {
        setReceiptData(items, total, paid, paymentMethod, false);
    }

    public void setReceiptData(List<CartItem> items, double total, double paid, String paymentMethod, boolean isGift) {
        this.isGiftMode = isGift;
        // Populate Printers
        cmbPrinters.setItems(FXCollections.observableArrayList(Printer.getAllPrinters()));
        Printer defaultPrinter = Printer.getDefaultPrinter();
        if (defaultPrinter != null) {
            cmbPrinters.getSelectionModel().select(defaultPrinter);
        }

        // Date and Time
        LocalDateTime now = LocalDateTime.now();
        // Format: 18 Feb, 2026 Hora: 12:20:38 Caja: 01
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM, yyyy 'Hora:' HH:mm:ss");
        lblDate.setText("Fecha: " + now.format(formatter) + " Caja: 01");

        // Random Ticket Number 01/XXXX
        int randomNum = 1000 + (int) (Math.random() * 9000);
        String prefix = isGiftMode ? "Ticket regalo Nº: 01/" : "Factura simplificada Nº: 01/";
        lblTicketTitle.setText(prefix + randomNum);

        // Items
        System.out.println("DEBUG: PrintPreviewController setReceiptData called with "
                + (items != null ? items.size() : "null") + " items.");
        itemsContainer.getChildren().clear();
        if (items != null) {
            for (CartItem item : items) {
                addItemRow(item);
            }
        }

        // Totals & Barcode Visibility
        totalsContainer.setVisible(!isGiftMode);
        totalsContainer.setManaged(!isGiftMode);
        barcodeSection.setVisible(false);
        barcodeSection.setManaged(false);

        lblGiftIndicator.setVisible(isGiftMode);
        lblGiftIndicator.setManaged(isGiftMode);
        lblPVPHeader.setVisible(!isGiftMode);
        lblPVPHeader.setManaged(!isGiftMode);
        lblTotalHeader.setVisible(!isGiftMode);
        lblTotalHeader.setManaged(!isGiftMode);
        paymentInfoContainer.setVisible(!isGiftMode);
        paymentInfoContainer.setManaged(!isGiftMode);

        // Totals
        // Assuming VAT is included in price for simplicity, or we can calculate it back
        // Let's assume 21% VAT included: Price = Base * 1.21 -> Base = Price / 1.21
        double subtotal = total / 1.21;
        double vat = total - subtotal;

        lblSubtotal.setText(String.format("%.2f €", subtotal));
        lblVat.setText(String.format("%.2f €", vat));
        lblTotal.setText(String.format("%.2f €", total));

        lblPaid.setText(String.format("%.2f €", paid));

        // Payment Method combined with amount if needed, but FXML has separate labels.
        // User example: "Efectivo 29,50 €"
        // We have separate Label lblPaymentMethod and Label lblPaid.
        // FXML structure: Method | Paid Amount
        // So we just set Method name.
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

        lblPaymentMethod.setText(paymentMethod);
    }

    @FXML
    private void handleGiftTicket() {
        this.isGiftMode = !this.isGiftMode;
        setReceiptData(currentItems, currentTotal, currentPaid, currentPaymentMethod, isGiftMode);
    }

    private List<CartItem> currentItems;
    private double currentTotal;
    private double currentPaid;
    private String currentPaymentMethod;

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

    @FXML
    private void confirmPrint() {
        Printer selectedPrinter = cmbPrinters.getSelectionModel().getSelectedItem();
        PrinterJob job = PrinterJob.createPrinterJob(selectedPrinter);

        if (job != null) {
            // Scale content to fit printable area width
            double pageWidth = job.getJobSettings().getPageLayout().getPrintableWidth();
            double contentWidth = paperSheet.getWidth();
            double scale = pageWidth / contentWidth;

            // Apply scale
            javafx.scene.transform.Scale scaleTransform = new javafx.scene.transform.Scale(scale, scale);
            paperSheet.getTransforms().add(scaleTransform);

            boolean success = job.printPage(paperSheet);

            paperSheet.getTransforms().remove(scaleTransform);

            if (success) {
                job.endJob();
                closeWindow();
            }
        }
    }

    @FXML
    private void cancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) paperSheet.getScene().getWindow();
        stage.close();
    }
}
