package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.application.usecase.ConfigUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.domain.model.CashClosure;
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
    private VBox totalsContainer, vatBreakdownContainer;
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
    private Button btnNewSale;
    @FXML
    private Label lblPVPHeader;
    @FXML
    private Label lblTotalHeader;
    @FXML
    private VBox paymentInfoContainer;
    @FXML
    private HBox hboxVatTotal;

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

        // N\u00famero de ticket
        String prefix = isGiftMode ? "Ticket regalo N\u00c2\u00ba: " : "Factura simplificada N\u00c2\u00ba: ";
        lblTicketTitle.setText(prefix + String.format("%03d", saleId));

        // Art\u00edculos
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

        // Estilo bot\u00f3n regalo
        if (isGiftMode) {
            btnGiftTicket.setStyle(
                    "-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-background-radius: 25; -fx-font-weight: bold; -fx-padding: 8 20; -fx-cursor: hand;");
            lblGiftIcon.setStyle("-fx-font-size: 16; -fx-text-fill: white;");
        } else {
            btnGiftTicket.setStyle(
                    "-fx-background-color: white; -fx-text-fill: #1a73e8; -fx-border-color: #1a73e8; -fx-border-radius: 25; -fx-border-width: 1; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 25; -fx-cursor: hand;");
            lblGiftIcon.setStyle("-fx-font-size: 16; -fx-text-fill: #1a73e8;");
        }

        // Totales con IVA din\u00e1mico \u00e2\u20ac\u201d agrupado por tasa
        java.util.Map<Double, Double[]> vatBreakdown = new java.util.TreeMap<>();
        boolean isInclusive = cfg != null && cfg.isPricesIncludeTax();
        double totalVatAmount = 0.0;
        double totalSubtotal = 0.0;

        if (currentItems != null) {
            for (CartItem item : currentItems) {
                double lineTotal = item.getTotal();
                double effectiveRate = item.getProduct().resolveEffectiveIva(cfg != null ? cfg.getTaxRate() : 21.0);

                double itemBase;
                double itemVat;
                if (isInclusive) {
                    itemBase = lineTotal / (1.0 + (effectiveRate / 100.0));
                    itemVat = lineTotal - itemBase;
                } else {
                    itemBase = lineTotal;
                    itemVat = lineTotal * (effectiveRate / 100.0);
                }

                totalVatAmount += itemVat;
                totalSubtotal += itemBase;

                Double[] vals = vatBreakdown.getOrDefault(effectiveRate, new Double[] { 0.0, 0.0 });
                vals[0] += itemBase;
                vals[1] += itemVat;
                vatBreakdown.put(effectiveRate, vals);
            }
        }

        // Render breakdown
        if (vatBreakdownContainer != null) {
            vatBreakdownContainer.getChildren().clear();
            for (java.util.Map.Entry<Double, Double[]> entry : vatBreakdown.entrySet()) {
                HBox row = new HBox();
                Label lblRate = new Label(String.format("IVA %.0f%%", entry.getKey()));
                lblRate.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(lblRate, Priority.ALWAYS);
                lblRate.setStyle("-fx-font-size: 10; -fx-text-fill: black;");

                Label lblAmount = new Label(String.format(fmt, entry.getValue()[1]));
                lblAmount.setStyle("-fx-font-size: 10; -fx-text-fill: black;");

                row.getChildren().addAll(lblRate, lblAmount);
                vatBreakdownContainer.getChildren().add(row);
            }
        }

        if (hboxVatTotal != null) {
            hboxVatTotal.setVisible(vatBreakdown.size() > 1);
            hboxVatTotal.setManaged(vatBreakdown.size() > 1);
        }

        lblSubtotal.setText(String.format(fmt, totalSubtotal));
        // Construir texto de IVA: si hay varias tasas, mostrar desglose; si hay una
        // sola, mostrar el %
        if (vatBreakdown.size() == 1) {
            if (lblVatLabel != null)
                lblVatLabel.setText("IVA " + String.format("%.0f%%", vatBreakdown.keySet().iterator().next()));
        } else if (vatBreakdown.size() > 1) {
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

    public void setClosureData(CashClosure closure) {
        if (configUseCase != null) {
            this.cfg = configUseCase.getConfig();
        }

        // Cargar impresoras disponibles
        if (cmbPrinters != null) {
            cmbPrinters.setItems(FXCollections.observableArrayList(javafx.print.Printer.getAllPrinters()));
            javafx.print.Printer defaultPrinter = javafx.print.Printer.getDefaultPrinter();
            if (defaultPrinter != null) {
                cmbPrinters.getSelectionModel().select(defaultPrinter);
            }
        }

        // Ocultar bot\u00f3n de ticket regalo que no aplica a arqueos
        if (btnGiftTicket != null) {
            btnGiftTicket.setVisible(false);
            btnGiftTicket.setManaged(false);
        }

        // Ocultar bot\u00f3n de Nueva Venta que no aplica a arqueos
        if (btnNewSale != null) {
            btnNewSale.setVisible(false);
            btnNewSale.setManaged(false);
        }

        applyCompanyHeader();
        lblTicketTitle.setText("TICKET DE CIERRE #" + closure.getClosureId());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        lblDate.setText(
                "Fecha: " + (closure.getCreatedAt() != null ? closure.getCreatedAt().format(formatter) : "N/D"));

        // Hide sales specific sections
        if (itemsHeaderHBox != null) {
            itemsHeaderHBox.setVisible(false);
            itemsHeaderHBox.setManaged(false);
        }
        itemsContainer.setVisible(false);
        itemsContainer.setManaged(false);
        totalsContainer.setVisible(false);
        totalsContainer.setManaged(false);
        paymentInfoContainer.setVisible(false);
        paymentInfoContainer.setManaged(false);
        if (barcodeSection != null) {
            barcodeSection.setVisible(false);
            barcodeSection.setManaged(false);
        }
        if (lblVatLabel != null) {
            lblVatLabel.setVisible(false);
            lblVatLabel.setManaged(false);
        }

        // Create Closure Details Section
        VBox closureDetails = new VBox(10);
        closureDetails.setStyle("-fx-padding: 15 0;");

        Label title = new Label("RESUMEN DE CAJA");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: black;");
        closureDetails.getChildren().add(title);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setVgap(5);
        grid.setHgap(10);

        int row = 0;
        addGridRow(grid, row++, "Cajero:", closure.getUsername() != null ? closure.getUsername() : "N/D");
        addGridRow(grid, row++, "Fondo Inicial:", String.format("%.2f \u20ac", closure.getInitialFund()));
        addGridRow(grid, row++, "(+) Ventas:", String.format("%.2f \u20ac",
                closure.getTotalCash() - closure.getInitialFund() - closure.getCashIn() + closure.getCashOut()));
        addGridRow(grid, row++, "(+) Entradas:", String.format("%.2f \u20ac", closure.getCashIn()));
        addGridRow(grid, row++, "(-) Retiradas:", String.format("%.2f \u20ac", closure.getCashOut()));
        addGridRow(grid, row++, "Total Esperado:", String.format("%.2f \u20ac", closure.getExpectedCash()));
        addGridRow(grid, row++, "Total Contado:", String.format("%.2f \u20ac", closure.getActualCash()));
        addGridRow(grid, row++, "Diferencia:", String.format("%+.2f \u20ac", closure.getDifference()));

        closureDetails.getChildren().add(grid);

        if (closure.getNotes() != null && !closure.getNotes().isEmpty()) {
            Label notesTitle = new Label("Observaciones:");
            notesTitle.setStyle(
                    "-fx-font-weight: bold; -fx-font-size: 11px; -fx-padding: 10 0 2 0; -fx-text-fill: black;");
            Label notes = new Label(closure.getNotes());
            notes.setWrapText(true);
            notes.setStyle("-fx-font-size: 10px; -fx-text-fill: black;");
            closureDetails.getChildren().addAll(notesTitle, notes);
        }

        paperSheet.getChildren().add(closureDetails);

        // Aplicar formato de papel
        applyPaperFormat();
    }

    private void addGridRow(javafx.scene.layout.GridPane grid, int row, String labelText, String valueText) {
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 10px; -fx-text-fill: black;");
        Label value = new Label(valueText);
        value.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: black;");
        grid.add(label, 0, row);
        grid.add(value, 1, row);
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

            // Redise\u00f1ar el \u00e1rbol visual para A4 de forma condicional
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
        // Sincronizar detecci\u00f3n de A4 con applyPaperFormat
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

        Label lblDesc = new Label(translateDynamic(item.getProduct().getName()));
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

            double unitPrice = item.getProduct().getPrice();
            double lineTotal = item.getTotal();

            if (cfg != null && !cfg.isPricesIncludeTax()) {
                double rate = item.getProduct().resolveEffectiveIva(cfg.getTaxRate());
                unitPrice = unitPrice * (1 + (rate / 100.0));
                lineTotal = lineTotal * (1 + (rate / 100.0));
            }

            Label lblPrice = new Label(String.format(priceFmt, unitPrice));
            lblPrice.setMinWidth(isA4 ? 100 : 55);
            lblPrice.setPrefWidth(isA4 ? 100 : 55);
            lblPrice.setMaxWidth(isA4 ? 100 : 55);
            lblPrice.setStyle("-fx-alignment: " + (isA4 ? "CENTER" : "CENTER-RIGHT") + "; -fx-font-size: " + fontSize
                    + "; -fx-text-fill: black;");

            Label lblTotal = new Label(String.format(totalFmt, lineTotal));
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

        // Usuario en sesi\u00f3n (cajero que atiende)
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
            if (client.getCountry() != null && !client.getCountry().equalsIgnoreCase("Espa\u00f1a")) {
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
                format = "A4"; // Forzar l\u00f3gica A4 para facturas

            if (format != null && !format.contains("A4")) {
                // Ancho f\u00edsico aproximado en puntos (58mm ~ 164pt, 80mm ~ 226pt)
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

    private com.mycompany.ventacontrolfx.domain.model.Return currentReturn;

    public void setReturnData(com.mycompany.ventacontrolfx.domain.model.Return returnRecord,
            com.mycompany.ventacontrolfx.domain.model.Sale originalSale,
            List<com.mycompany.ventacontrolfx.domain.model.ReturnDetail> details) {
        this.currentReturn = returnRecord;

        if (configUseCase != null) {
            this.cfg = configUseCase.getConfig();
        }
        String sym = cfg != null ? cfg.getCurrencySymbol() : "\u20ac";
        String fmt = "%." + (cfg != null ? cfg.getDecimalCount() : 2) + "f " + sym;

        // Impresoras disponibles
        cmbPrinters.setItems(FXCollections.observableArrayList(Printer.getAllPrinters()));
        Printer defaultPrinter = Printer.getDefaultPrinter();
        if (defaultPrinter != null) {
            cmbPrinters.getSelectionModel().select(defaultPrinter);
        }

        // Fecha y hora de la devoluci\u00f3n
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM, yyyy 'Hora:' HH:mm:ss");
        lblDate.setText("Fecha Dev: " + returnRecord.getReturnDatetime().format(formatter));

        // T\u00edtulo: Factura Rectificativa (Si tiene n\u00famero fiscal) o Ticket de Devoluci\u00f3n
        if (returnRecord.getDocNumber() != null) {
            lblTicketTitle.setText("Factura Rectificativa N\u00c2\u00ba: " + returnRecord.getFullReference());
        } else {
            lblTicketTitle.setText("Ticket de Devoluci\u00f3n N\u00c2\u00ba: REF-" + returnRecord.getReturnId());
        }

        // Informaci\u00f3n de referencia al ticket original
        Label lblRef = new Label("Ref. Ticket Original: #" + originalSale.getSaleId());
        lblRef.setStyle("-fx-font-size: 11px; -fx-text-fill: #666; -fx-padding: 5 0;");
        if (ticketInfoSection != null)
            ticketInfoSection.getChildren().add(lblRef);

        // Art\u00edculos devueltos
        itemsContainer.getChildren().clear();
        for (com.mycompany.ventacontrolfx.domain.model.ReturnDetail detail : details) {
            HBox row = new HBox(10);
            row.setStyle("-fx-border-color: transparent transparent #eee transparent; -fx-padding: 4 0 4 0;");

            Label lblDesc = new Label(
                    detail.getProductName() != null ? detail.getProductName() : "Producto #" + detail.getProductId());
            lblDesc.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(lblDesc, Priority.ALWAYS);
            lblDesc.setStyle("-fx-font-size: 10px; -fx-text-fill: black;");

            Label lblQty = new Label(String.valueOf(detail.getQuantity()));
            lblQty.setMinWidth(30);
            lblQty.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-size: 10px; -fx-text-fill: black;");

            Label lblPrice = new Label(String.format(fmt, detail.getUnitPrice()));
            lblPrice.setMinWidth(55);
            lblPrice.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-size: 10px; -fx-text-fill: black;");

            Label lblTotal = new Label(String.format(fmt, detail.getSubtotal()));
            lblTotal.setMinWidth(55);
            lblTotal.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-size: 10px; -fx-text-fill: black;");

            row.getChildren().addAll(lblDesc, lblQty, lblPrice, lblTotal);
            itemsContainer.getChildren().add(row);
        }

        // Totales (Negativos representan abono)
        lblTotal.setText("-" + String.format(fmt, returnRecord.getTotalRefunded()));
        lblSubtotal.setText("-" + String.format(fmt, returnRecord.getTotalRefunded())); // Simplificado
        lblVat.setText("0.00 " + sym); // En abonos simples se suele poner el total bruto

        lblPaymentMethod.setText(returnRecord.getPaymentMethod());
        if (lblPaid != null)
            lblPaid.setText(String.format(fmt, returnRecord.getTotalRefunded()));

        // Datos de empresa
        applyCompanyHeader();

        // Si la venta original ten\u00eda cliente, configurarlo para A4
        if (originalSale.getClientId() != null) {
            try {
                com.mycompany.ventacontrolfx.domain.model.Client client = container.getClientUseCase()
                        .getById(originalSale.getClientId());
                if (client != null)
                    setClientInfo(client);
            } catch (Exception ignored) {
            }
        }

        applyPaperFormat();
    }

    private String translateDynamic(String text) {
        if (text == null || text.isBlank()) return text;
        if (container != null && container.getBundle() != null && container.getBundle().containsKey(text)) {
            return container.getBundle().getString(text);
        }
        return text;
    }
}
