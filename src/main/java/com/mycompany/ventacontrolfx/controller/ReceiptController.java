package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.CartItem;
import com.mycompany.ventacontrolfx.model.SaleConfig;
import com.mycompany.ventacontrolfx.service.SaleConfigService;
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
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

public class ReceiptController {

    @FXML
    private VBox receiptContent;
    @FXML
    private VBox companyHeaderSection;
    @FXML
    private VBox ticketInfoSection;
    @FXML
    private javafx.scene.layout.HBox itemsHeaderHBox;
    @FXML
    private Label lblTicketTitle;
    @FXML
    private Label lblSuccessMessage;
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
    @FXML
    private ImageView imgAppLogoRight;
    @FXML
    private Label lblSuccessIcon;

    @FXML
    private VBox clientInfoSection;
    @FXML
    private Label lblClientName;
    @FXML
    private Label lblClientTaxId;
    @FXML
    private Label lblClientAddress;

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

    private Runnable onNewSale;
    private Runnable onBack;
    private boolean isGiftMode = false;

    private final SaleConfigService configService = new SaleConfigService();
    private SaleConfig cfg;

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
                if (this.currentClient != null) {
                    controller.setClientInfo(this.currentClient);
                }
                controller.setReceiptData(this.currentItems, this.currentTotal, this.currentPaid,
                        this.currentPaymentMethod, this.currentSaleId, this.isGiftMode);
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
    private int currentSaleId;
    private com.mycompany.ventacontrolfx.model.Client currentClient;

    public void setReceiptData(List<CartItem> items, double total, double paid, double change, String paymentMethod,
            int saleId, Runnable onNewSale, Runnable onBack) {
        this.currentItems = items;
        this.currentTotal = total;
        this.currentPaid = paid;
        this.currentPaymentMethod = paymentMethod;
        this.currentSaleId = saleId;

        this.cfg = configService.load();
        this.onNewSale = onNewSale;
        this.onBack = onBack;

        String sym = cfg.getCurrencySymbol();
        String fmt = "%." + cfg.getDecimalCount() + "f " + sym;

        // Fecha
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM, yyyy 'Hora:' HH:mm:ss");
        lblDate.setText("Fecha: " + now.format(formatter) + " Caja: 01");

        // Número de ticket
        String prefix = isGiftMode ? "Ticket regalo Nº: " : "Factura simplificada Nº: ";
        lblTicketTitle.setText(prefix + String.format("%03d", saleId));

        if (lblSuccessMessage != null) {
            if (isGiftMode) {
                lblSuccessMessage.setText("¡Ticket regalo creado con éxito!");
            } else if (currentClient != null) {
                lblSuccessMessage.setText("¡Factura creada con éxito!");
            } else {
                lblSuccessMessage.setText("¡Factura simplificada creada con éxito!");
            }
        }

        // Cabecera empresa en el ticket (si el label existe en el FXML)
        applyCompanyHeader();

        // Artículos
        itemsContainer.getChildren().clear();
        for (CartItem item : items) {
            addItemRow(item, sym);
        }

        // Visibilidad de bloques
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
        lblChange.setText(String.format(fmt, change));
        lblPaymentMethod.setText(paymentMethod);
        lblChangeRight.setText(String.format(fmt, change));

        if (lblTotalRight != null) {
            lblTotalRight.setText(String.format(fmt, total));
        }

        applyCompanyHeader();
        applyPaperFormat();
    }

    private void applyPaperFormat() {
        if (cfg == null || receiptContent == null)
            return;

        String format = cfg.getTicketFormat();
        if (currentClient != null)
            format = "A4"; // Facturas completas siempre A4
        if (format == null)
            format = "80mm";

        if (format.contains("80mm")) {
            receiptContent.setMinWidth(300);
            receiptContent.setPrefWidth(300);
            receiptContent.setMaxWidth(300);
            receiptContent.setStyle(receiptContent.getStyle() + "; -fx-padding: 10;");
        } else if (format.contains("58mm")) {
            receiptContent.setMinWidth(220);
            receiptContent.setPrefWidth(220);
            receiptContent.setMaxWidth(220);
            receiptContent.setStyle(receiptContent.getStyle() + "; -fx-padding: 5;");
        } else { // A4
            receiptContent.setMinWidth(750);
            receiptContent.setPrefWidth(750);
            receiptContent.setMaxWidth(750);
            receiptContent.setStyle("-fx-background-color: white; -fx-padding: 40 50 40 50;");

            // Rediseñar el árbol visual para A4 de forma condicional
            receiptContent.getChildren().clear();

            // Cabecera top: Datos empresa (Izda) + Datos ticket (Derecha)
            javafx.scene.layout.HBox topRow = new javafx.scene.layout.HBox();
            topRow.setSpacing(20);
            topRow.setStyle("-fx-padding: 0 0 30 0;");

            companyHeaderSection.setAlignment(javafx.geometry.Pos.TOP_LEFT);
            if (lblCompanyBrand != null)
                lblCompanyBrand.setStyle("-fx-font-weight: bold; -fx-font-size: 26; -fx-text-fill: #2196F3;");
            if (lblCompanyName != null)
                lblCompanyName.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: black;");
            if (lblCompanyAddress != null)
                lblCompanyAddress.setStyle("-fx-font-size: 12; -fx-text-fill: black;");
            if (lblCompanyPhone != null)
                lblCompanyPhone.setStyle("-fx-font-size: 12; -fx-text-fill: black;");
            if (lblCompanyCif != null)
                lblCompanyCif.setStyle("-fx-font-size: 12; -fx-text-fill: black;");

            clientInfoSection.setPrefWidth(300);
            clientInfoSection.setStyle(
                    "-fx-padding: 10; -fx-border-color: #ddd; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-color: #fcfcfc;");

            VBox leftCol = new VBox(15);
            leftCol.getChildren().addAll(companyHeaderSection, clientInfoSection);

            ticketInfoSection.setAlignment(javafx.geometry.Pos.TOP_RIGHT);
            if (lblTicketTitle != null)
                lblTicketTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 20; -fx-text-fill: #333;");
            if (lblDate != null)
                lblDate.setStyle("-fx-font-size: 12; -fx-text-fill: #555;");

            javafx.scene.layout.Region topSpacer = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(topSpacer, javafx.scene.layout.Priority.ALWAYS);
            topRow.getChildren().addAll(leftCol, topSpacer, ticketInfoSection);

            // Header de tabla
            itemsHeaderHBox.setSpacing(10);
            itemsHeaderHBox.setMinWidth(650);
            itemsHeaderHBox.setPrefWidth(650);
            itemsHeaderHBox.setMaxWidth(650);
            itemsHeaderHBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            itemsHeaderHBox.setStyle(
                    "-fx-border-color: transparent transparent #ccc transparent; -fx-border-width: 1; -fx-padding: 5; -fx-background-color: #f9f9f9;");

            for (javafx.scene.Node n : itemsHeaderHBox.getChildren()) {
                if (n instanceof Label) {
                    Label l = (Label) n;
                    l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: black;");
                    javafx.scene.layout.HBox.setHgrow(l, javafx.scene.layout.Priority.NEVER);
                    String text = l.getText().toLowerCase();
                    if (text.contains("desc")) {
                        l.setMinWidth(340);
                        l.setPrefWidth(340);
                        l.setMaxWidth(340);
                        l.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    } else if (text.contains("cant")) {
                        l.setMinWidth(80);
                        l.setPrefWidth(80);
                        l.setMaxWidth(80);
                        l.setAlignment(javafx.geometry.Pos.CENTER);
                    } else if (text.contains("pvp")) {
                        l.setMinWidth(100);
                        l.setPrefWidth(100);
                        l.setMaxWidth(100);
                        l.setAlignment(javafx.geometry.Pos.CENTER);
                    } else if (text.contains("total")) {
                        l.setMinWidth(100);
                        l.setPrefWidth(100);
                        l.setMaxWidth(100);
                        l.setAlignment(javafx.geometry.Pos.CENTER);
                    }
                }
            }

            // Contenedor de items y finales se re-agregan
            javafx.scene.layout.HBox totalsRow = new javafx.scene.layout.HBox();
            totalsRow.setStyle("-fx-padding: 20 0 0 0;");
            javafx.scene.layout.Region totalsSpacer = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(totalsSpacer, javafx.scene.layout.Priority.ALWAYS);
            totalsContainer.setPrefWidth(250);
            if (lblTotal != null)
                lblTotal.setStyle("-fx-font-weight: bold; -fx-font-size: 18; -fx-text-fill: black;");
            totalsRow.getChildren().addAll(totalsSpacer, totalsContainer);

            javafx.scene.layout.HBox paymentRow = new javafx.scene.layout.HBox();
            paymentInfoContainer.setPrefWidth(250);
            javafx.scene.layout.Region paySpacer = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(paySpacer, javafx.scene.layout.Priority.ALWAYS);
            paymentRow.getChildren().addAll(paySpacer, paymentInfoContainer);

            javafx.scene.layout.Region footerSpacer = new javafx.scene.layout.Region();
            javafx.scene.layout.VBox.setVgrow(footerSpacer, javafx.scene.layout.Priority.ALWAYS);

            receiptContent.getChildren().addAll(
                    topRow,
                    itemsHeaderHBox,
                    itemsContainer,
                    totalsRow,
                    paymentRow,
                    footerSpacer,
                    barcodeSection);
        }
    }

    /** Rellena la cabecera de empresa del ticket con los datos de SaleConfig */
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
                    if (imgAppLogoRight != null) {
                        imgAppLogoRight.setImage(image);
                        imgAppLogoRight.setVisible(true);
                        imgAppLogoRight.setManaged(true);
                    }
                    if (lblCompanyIcon != null) {
                        lblCompanyIcon.setVisible(false);
                        lblCompanyIcon.setManaged(false);
                    }
                    if (lblSuccessIcon != null) {
                        lblSuccessIcon.setVisible(false);
                        lblSuccessIcon.setManaged(false);
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
            if (imgAppLogoRight != null) {
                imgAppLogoRight.setVisible(false);
                imgAppLogoRight.setManaged(false);
            }
            if (lblCompanyIcon != null) {
                lblCompanyIcon.setVisible(false);
                lblCompanyIcon.setManaged(false);
            }
            if (lblSuccessIcon != null) {
                lblSuccessIcon.setVisible(true);
                lblSuccessIcon.setManaged(true);
            }
        }

        String name = cfg.getCompanyName();
        // Brand (texto azul grande): mostrar nombre de empresa si existe
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
        if (imgAppLogoRight != null) {
            imgAppLogoRight.setVisible(false);
            imgAppLogoRight.setManaged(false);
        }
        if (lblCompanyIcon != null) {
            lblCompanyIcon.setVisible(true);
            lblCompanyIcon.setManaged(true);
        }
        if (lblSuccessIcon != null) {
            lblSuccessIcon.setVisible(true);
            lblSuccessIcon.setManaged(true);
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
        this.currentClient = client;
        applyPaperFormat(); // Cambiar a A4 si hay cliente
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

            if (!isGiftMode && lblSuccessMessage != null) {
                lblSuccessMessage.setText("¡Factura creada con éxito!");
            }
        }
    }

    @FXML
    private void handleGiftTicket() {
        this.isGiftMode = !this.isGiftMode;
        setReceiptData(currentItems, currentTotal, currentPaid,
                Double.parseDouble(lblChange.getText()
                        .replaceAll("[^0-9.,]", "").replace(",", ".")),
                currentPaymentMethod, currentSaleId, onNewSale, onBack);
    }

    private void addItemRow(CartItem item, String sym) {
        javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(10);
        boolean isA4 = (cfg != null && cfg.getTicketFormat().contains("A4")) || currentClient != null;

        if (isA4) {
            row.setMinWidth(650);
            row.setPrefWidth(650);
            row.setMaxWidth(650);
            row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        }

        row.setStyle(isA4
                ? "-fx-border-color: transparent transparent #eee transparent; -fx-padding: 8 5 8 5;"
                : "-fx-border-color: transparent transparent #eee transparent; -fx-padding: 2 0 2 0;");

        String fontSize = isA4 ? "12px" : "10px";

        Label lblDesc = new Label(item.getProduct().getName());
        if (isA4) {
            lblDesc.setMinWidth(340);
            lblDesc.setPrefWidth(340);
            lblDesc.setMaxWidth(340);
        } else {
            lblDesc.setMaxWidth(Double.MAX_VALUE);
            javafx.scene.layout.HBox.setHgrow(lblDesc, javafx.scene.layout.Priority.ALWAYS);
        }
        lblDesc.setStyle("-fx-font-size: " + fontSize + "; -fx-text-fill: black;");

        Label lblQty = new Label(String.valueOf(item.getQuantity()));
        lblQty.setMinWidth(isA4 ? 80 : 30);
        lblQty.setPrefWidth(isA4 ? 80 : 30);
        lblQty.setMaxWidth(isA4 ? 80 : 30);
        lblQty.setStyle("-fx-alignment: " + (isA4 ? "CENTER" : "CENTER-RIGHT") + "; -fx-font-size: " + fontSize
                + "; -fx-text-fill: black;");

        row.getChildren().addAll(lblDesc, lblQty);

        if (!isGiftMode) {
            int dec = cfg != null ? cfg.getDecimalCount() : 2;
            String priceFmt = "%." + dec + "f " + sym;
            String totalFmt = "%." + dec + "f " + sym;

            Label lblPrice = new Label(String.format(priceFmt, item.getProduct().getPrice()));
            lblPrice.setMinWidth(isA4 ? 100 : 55);
            lblPrice.setPrefWidth(isA4 ? 100 : 55);
            lblPrice.setMaxWidth(isA4 ? 100 : 55);
            lblPrice.setStyle("-fx-alignment: " + (isA4 ? "CENTER" : "CENTER-RIGHT") + "; -fx-font-size: " + fontSize
                    + "; -fx-text-fill: black;");

            Label lblTotal = new Label(String.format(totalFmt, item.getTotal()));
            lblTotal.setMinWidth(isA4 ? 100 : 55);
            lblTotal.setPrefWidth(isA4 ? 100 : 55);
            lblTotal.setMaxWidth(isA4 ? 100 : 55);
            lblTotal.setStyle("-fx-alignment: " + (isA4 ? "CENTER" : "CENTER-RIGHT") + "; -fx-font-size: " + fontSize
                    + "; -fx-text-fill: black;");

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
