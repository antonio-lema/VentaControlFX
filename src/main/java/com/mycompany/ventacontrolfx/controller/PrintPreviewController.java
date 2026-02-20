package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.CartItem;
import com.mycompany.ventacontrolfx.model.SaleConfig;
import com.mycompany.ventacontrolfx.service.SaleConfigService;
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

    // Labels de cabecera empresa (rellenados desde SaleConfig)
    @FXML
    private Label lblCompanyIcon;
    @FXML
    private javafx.scene.image.ImageView imgCompanyLogo;
    @FXML
    private Label lblCompanyBrand;
    @FXML
    private Label lblCompanyName;
    @FXML
    private Label lblCompanyAddress;
    @FXML
    private Label lblCompanyPhone;
    @FXML
    private Label lblCompanyCif;
    @FXML
    private Label lblFooterMessage;

    @FXML
    private VBox clientInfoSection;
    @FXML
    private Label lblClientName;
    @FXML
    private Label lblClientTaxId;
    @FXML
    private Label lblClientAddress;

    private boolean isGiftMode = false;
    private final SaleConfigService configService = new SaleConfigService();
    private SaleConfig cfg;

    public void setReceiptData(List<CartItem> items, double total, double paid, String paymentMethod) {
        setReceiptData(items, total, paid, paymentMethod, false);
    }

    public void setReceiptData(List<CartItem> items, double total, double paid, String paymentMethod, boolean isGift) {
        this.isGiftMode = isGift;
        this.cfg = configService.load();
        String sym = cfg.getCurrencySymbol();
        String fmt = "%." + cfg.getDecimalCount() + "f " + sym;

        // Impresoras disponibles
        cmbPrinters.setItems(FXCollections.observableArrayList(Printer.getAllPrinters()));
        Printer defaultPrinter = Printer.getDefaultPrinter();
        if (defaultPrinter != null) {
            cmbPrinters.getSelectionModel().select(defaultPrinter);
        }

        // Fecha y hora
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM, yyyy 'Hora:' HH:mm:ss");
        lblDate.setText("Fecha: " + now.format(formatter) + " Caja: 01");

        // Número de ticket
        int randomNum = 1000 + (int) (Math.random() * 9000);
        String prefix = isGiftMode ? "Ticket regalo Nº: 01/" : "Factura simplificada Nº: 01/";
        lblTicketTitle.setText(prefix + randomNum);

        // Artículos
        itemsContainer.getChildren().clear();
        if (items != null) {
            for (CartItem item : items) {
                addItemRow(item, sym);
            }
        }

        // Visibilidad
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

        // Estilo botón regalo
        if (isGiftMode) {
            btnGiftTicket.setStyle(
                    "-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-background-radius: 25; -fx-font-weight: bold; -fx-padding: 8 20; -fx-cursor: hand;");
            lblGiftIcon.setStyle("-fx-font-size: 16; -fx-text-fill: white;");
        } else {
            btnGiftTicket.setStyle(
                    "-fx-background-color: white; -fx-text-fill: #1a73e8; -fx-border-color: #1a73e8; -fx-border-radius: 25; -fx-border-width: 1; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 25; -fx-cursor: hand;");
            lblGiftIcon.setStyle("-fx-font-size: 16; -fx-text-fill: #1a73e8;");
        }

        // Totales con IVA real
        double taxDiv = cfg.getTaxDivisor();
        double subtotal = total / taxDiv;
        double vat = total - subtotal;

        lblSubtotal.setText(String.format(fmt, subtotal));
        lblVat.setText("(" + cfg.getTaxRate() + "%) " + String.format(fmt, vat));
        lblTotal.setText(String.format(fmt, total));
        lblPaid.setText(String.format(fmt, paid));
        lblPaymentMethod.setText(paymentMethod);

        // Datos de empresa desde SaleConfig
        applyCompanyHeader();
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

    private void addItemRow(CartItem item, String sym) {
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
            int dec = cfg != null ? cfg.getDecimalCount() : 2;
            String priceFmt = "%." + dec + "f";
            String totalFmt = "%." + dec + "f " + sym;

            Label lblPrice = new Label(String.format(priceFmt, item.getProduct().getPrice()));
            lblPrice.setPrefWidth(55);
            lblPrice.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-size: 10px; -fx-text-fill: black;");

            Label lblTotal = new Label(String.format(totalFmt, item.getTotal()));
            lblTotal.setPrefWidth(55);
            lblTotal.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-size: 10px; -fx-text-fill: black;");

            row.getChildren().addAll(lblPrice, lblTotal);
        }

        itemsContainer.getChildren().add(row);
    }

    /** Rellena la cabecera de empresa con los datos de SaleConfig */
    private void applyCompanyHeader() {
        if (cfg == null)
            return;

        // Logo
        if (cfg.isShowLogo()) {
            String logoPath = cfg.getLogoPath();
            if (logoPath != null && !logoPath.trim().isEmpty()) {
                java.io.File file = new java.io.File(logoPath);
                if (file.exists()) {
                    javafx.scene.image.Image image = new javafx.scene.image.Image(file.toURI().toString());
                    if (imgCompanyLogo != null) {
                        imgCompanyLogo.setImage(image);
                        imgCompanyLogo.setVisible(true);
                        imgCompanyLogo.setManaged(true);
                    }
                    if (lblCompanyIcon != null) {
                        lblCompanyIcon.setVisible(false);
                        lblCompanyIcon.setManaged(false);
                    }
                } else {
                    showDefaultIcon();
                }
            } else {
                showDefaultIcon();
            }
        } else {
            if (imgCompanyLogo != null) {
                imgCompanyLogo.setVisible(false);
                imgCompanyLogo.setManaged(false);
            }
            if (lblCompanyIcon != null) {
                lblCompanyIcon.setVisible(false);
                lblCompanyIcon.setManaged(false);
            }
        }

        String name = cfg.getCompanyName();
        if (lblCompanyBrand != null && name != null && !name.isEmpty()) {
            lblCompanyBrand.setText(name);
        }
        setLabelText(lblCompanyName, name);
        setLabelText(lblCompanyAddress, cfg.isShowAddress() ? cfg.getAddress() : "");
        setLabelText(lblCompanyPhone, cfg.isShowPhone() ? "Tel: " + cfg.getPhone() : "");
        setLabelText(lblCompanyCif, cfg.isShowCif() ? "CIF: " + cfg.getCif() : "");
        String footer = cfg.getFooterMessage();
        setLabelText(lblFooterMessage, (footer != null && !footer.isEmpty()) ? footer : "GRACIAS POR SU VISITA");
    }

    private void showDefaultIcon() {
        if (imgCompanyLogo != null) {
            imgCompanyLogo.setVisible(false);
            imgCompanyLogo.setManaged(false);
        }
        if (lblCompanyIcon != null) {
            lblCompanyIcon.setVisible(true);
            lblCompanyIcon.setManaged(true);
        }
    }

    private void setLabelText(Label lbl, String text) {
        if (lbl == null)
            return;
        boolean hasText = text != null && !text.isEmpty();
        lbl.setText(hasText ? text : "");
        lbl.setVisible(hasText);
        lbl.setManaged(hasText);
    }

    public void setClientInfo(com.mycompany.ventacontrolfx.model.Client client) {
        if (client != null) {
            if (clientInfoSection != null) {
                clientInfoSection.setVisible(true);
                clientInfoSection.setManaged(true);
            }
            if (lblClientName != null) {
                lblClientName.setText(client.getName());
            }
            if (lblClientTaxId != null) {
                lblClientTaxId.setText("CIF: " + client.getTaxId());
            }

            String fullAddress = client.getAddress();
            if (client.getPostalCode() != null && !client.getPostalCode().isEmpty()) {
                fullAddress += "\n" + client.getPostalCode() + " " + client.getCity() + " (" + client.getProvince()
                        + ")";
            }
            if (client.getCountry() != null && !client.getCountry().equalsIgnoreCase("España")) {
                fullAddress += "\n" + client.getCountry();
            }
            if (lblClientAddress != null) {
                lblClientAddress.setText(fullAddress);
            }

            // Upgrade title from simplified to full invoice
            if (!isGiftMode && lblTicketTitle != null) {
                String currentTitle = lblTicketTitle.getText();
                lblTicketTitle.setText(currentTitle.replace("Factura simplificada", "Factura"));
            }
        }
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
