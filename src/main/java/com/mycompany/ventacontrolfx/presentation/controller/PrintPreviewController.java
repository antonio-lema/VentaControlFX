package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.application.usecase.ConfigUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.domain.model.CartItem;
import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
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

public class PrintPreviewController implements Injectable {

    @FXML
    private VBox paperSheet;
    @FXML
    private VBox companyHeaderSection;
    @FXML
    private VBox ticketInfoSection;
    @FXML
    private HBox itemsHeaderHBox;
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

    @FXML
    private Label lblVatLabel;
    @FXML
    private Label lblAttendedBy;

    private boolean isGiftMode = false;
    private com.mycompany.ventacontrolfx.domain.model.Client currentClient;
    private ConfigUseCase configUseCase;
    private SaleConfig cfg;
    private ServiceContainer container;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.configUseCase = container.getConfigUseCase();
    }

    public void setReceiptData(List<CartItem> items, double total, double paid, double change, String paymentMethod,
            int saleId) {
        setReceiptData(items, total, paid, change, paymentMethod, saleId, false);
    }

    public void setReceiptData(List<CartItem> items, double total, double paid, double change, String paymentMethod,
            int saleId,
            boolean isGift) {
        this.isGiftMode = isGift;
        this.currentItems = items;
        this.currentTotal = total;
        this.currentPaid = paid;
        this.currentChange = change;
        this.currentPaymentMethod = paymentMethod;
        this.currentSaleId = saleId;
        if (configUseCase != null) {
            this.cfg = configUseCase.getConfig();
        }
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
        String prefix = isGiftMode ? "Ticket regalo Nº: " : "Factura simplificada Nº: ";
        lblTicketTitle.setText(prefix + String.format("%03d", saleId));

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

        // Totales con IVA dinámico — agrupado por tasa
        double totalVatAmount = 0.0;
        double totalSubtotal = 0.0;
        java.util.Map<Double, Double> vatByRate = new java.util.LinkedHashMap<>();

        if (currentItems != null) {
            for (CartItem item : currentItems) {
                double itemTotal = item.getTotal();
                double effectiveRate = item.getProduct().resolveEffectiveIva(cfg.getTaxRate());
                double itemSubtotal = itemTotal / (1.0 + (effectiveRate / 100.0));
                double itemVat = itemTotal - itemSubtotal;
                totalVatAmount += itemVat;
                totalSubtotal += itemSubtotal;
                vatByRate.merge(effectiveRate, itemVat, Double::sum);
            }
        }

        lblSubtotal.setText(String.format(fmt, totalSubtotal));
        // Construir texto de IVA: si hay varias tasas, mostrar desglose; si hay una
        // sola, mostrar el %
        if (vatByRate.size() == 1) {
            double singleRate = vatByRate.keySet().iterator().next();
            if (lblVatLabel != null)
                lblVatLabel.setText("IVA " + String.format("%.0f%%", singleRate));
        } else if (vatByRate.size() > 1) {
            if (lblVatLabel != null)
                lblVatLabel.setText("IVA (varios tipos)");
        } else {
            if (lblVatLabel != null)
                lblVatLabel.setText("IVA " + String.format("%.0f%%", cfg.getTaxRate()));
        }
        lblVat.setText(String.format(fmt, totalVatAmount));
        lblTotal.setText(String.format(fmt, total));
        lblPaid.setText(String.format(fmt, paid));
        if (lblChange != null)
            lblChange.setText(String.format(fmt, change));
        lblPaymentMethod.setText(paymentMethod);

        // Datos de empresa desde SaleConfig
        applyCompanyHeader();

        // Aplicar formato de papel
        applyPaperFormat();
    }

    private void applyPaperFormat() {
        if (cfg == null || paperSheet == null)
            return;

        String format = cfg.getTicketFormat();

        // Si hay un cliente asignado, es una Factura completa y la sacamos en A4
        if (currentClient != null) {
            format = "A4";
        }

        if (format == null)
            format = "80mm";

        if (format.contains("80mm")) {
            paperSheet.setMinWidth(300);
            paperSheet.setPrefWidth(300);
            paperSheet.setMaxWidth(300);
            paperSheet.setStyle(paperSheet.getStyle() + "; -fx-padding: 10 10 10 10;");
        } else if (format.contains("58mm")) {
            paperSheet.setMinWidth(220);
            paperSheet.setPrefWidth(220);
            paperSheet.setMaxWidth(220);
            paperSheet.setStyle(paperSheet.getStyle() + "; -fx-padding: 5 5 5 5;");
        } else { // A4
            paperSheet.setMinWidth(750);
            paperSheet.setPrefWidth(750);
            paperSheet.setMaxWidth(750);
            paperSheet.setStyle("-fx-background-color: white; -fx-padding: 40 50 40 50;");

            // Rediseñar el árbol visual para A4 de forma condicional
            paperSheet.getChildren().clear();

            // Cabecera top: Datos empresa (Izda) + Datos ticket (Derecha)
            HBox topRow = new HBox();
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
            HBox.setHgrow(topSpacer, javafx.scene.layout.Priority.ALWAYS);
            topRow.getChildren().addAll(leftCol, topSpacer, ticketInfoSection);

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
            HBox totalsRow = new HBox();
            totalsRow.setStyle("-fx-padding: 20 0 0 0;");
            javafx.scene.layout.Region totalsSpacer = new javafx.scene.layout.Region();
            HBox.setHgrow(totalsSpacer, javafx.scene.layout.Priority.ALWAYS);
            totalsContainer.setPrefWidth(250);
            if (lblTotal != null)
                lblTotal.setStyle("-fx-font-weight: bold; -fx-font-size: 18; -fx-text-fill: black;");
            totalsRow.getChildren().addAll(totalsSpacer, totalsContainer);

            HBox paymentRow = new HBox();
            paymentInfoContainer.setPrefWidth(250);
            javafx.scene.layout.Region paySpacer = new javafx.scene.layout.Region();
            HBox.setHgrow(paySpacer, javafx.scene.layout.Priority.ALWAYS);
            paymentRow.getChildren().addAll(paySpacer, paymentInfoContainer);

            javafx.scene.layout.Region footerSpacer = new javafx.scene.layout.Region();
            javafx.scene.layout.VBox.setVgrow(footerSpacer, javafx.scene.layout.Priority.ALWAYS);

            paperSheet.getChildren().addAll(
                    topRow,
                    itemsHeaderHBox,
                    itemsContainer,
                    totalsRow,
                    paymentRow,
                    footerSpacer,
                    barcodeSection);
        }
    }

    @FXML
    private void handleGiftTicket() {
        this.isGiftMode = !this.isGiftMode;
        setReceiptData(currentItems, currentTotal, currentPaid, currentChange, currentPaymentMethod, currentSaleId,
                isGiftMode);
    }

    private List<CartItem> currentItems;
    private double currentTotal;
    private double currentPaid;
    private double currentChange;
    private String currentPaymentMethod;
    private int currentSaleId;

    private void addItemRow(CartItem item, String sym) {
        HBox row = new HBox(10);
        // Sincronizar detección de A4 con applyPaperFormat
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
            HBox.setHgrow(lblDesc, javafx.scene.layout.Priority.ALWAYS);
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

        // Usuario en sesión (cajero que atiende)
        if (lblAttendedBy != null) {
            try {
                if (container != null && container.getUserSession() != null
                        && container.getUserSession().getCurrentUser() != null) {
                    String userName = container.getUserSession().getCurrentUser().getFullName();
                    if (userName == null || userName.isEmpty()) {
                        userName = container.getUserSession().getCurrentUser().getUsername();
                    }
                    setLabelText(lblAttendedBy, "Le ha atendido: " + userName);
                } else {
                    setLabelText(lblAttendedBy, "");
                }
            } catch (Exception e) {
                setLabelText(lblAttendedBy, "");
            }
        }
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

    public void setClientInfo(com.mycompany.ventacontrolfx.domain.model.Client client) {
        this.currentClient = client;
        if (client != null) {
            applyPaperFormat(); // Re-aplicar para cambiar a formato A4 si es necesario
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
        lblBarcodeValue.setText(code);
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
            // Forzar pass de layout antes de medir
            paperSheet.applyCss();
            paperSheet.layout();

            // Scale content to fit printable area width
            double pageWidth = job.getJobSettings().getPageLayout().getPrintableWidth();
            double contentWidth = paperSheet.getWidth();
            if (contentWidth <= 0)
                contentWidth = currentClient != null ? 750 : 300;

            double scale = pageWidth / contentWidth;

            // Evitar que los formatos de ticket se estiren al imprimir en papel grande (PDF
            // o A4)
            String format = cfg.getTicketFormat();
            if (currentClient != null)
                format = "A4"; // Forzar lógica A4 para facturas

            if (format != null && !format.contains("A4")) {
                // Ancho físico aproximado en puntos (58mm ~ 164pt, 80mm ~ 226pt)
                double maxTicketPoints = format.contains("58mm") ? 180 : 250;
                if (pageWidth > maxTicketPoints) {
                    scale = maxTicketPoints / contentWidth;
                }
            }

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
