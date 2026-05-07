package com.mycompany.ventacontrolfx.presentation.controller.dialog;

import com.mycompany.ventacontrolfx.application.usecase.ConfigUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.domain.model.CashClosure;
import com.mycompany.ventacontrolfx.domain.model.CartItem;
import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
import com.mycompany.ventacontrolfx.presentation.controller.receipt.DocumentFormatter;
import com.mycompany.ventacontrolfx.presentation.controller.receipt.FiscalRenderer;
import com.mycompany.ventacontrolfx.presentation.controller.receipt.PrinterManager;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.print.Printer;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
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
    private VBox companyHeaderSection, ticketInfoSection, itemsContainer, totalsContainer, vatBreakdownContainer, barcodeSection, paymentInfoContainer, rewardSection;
    @FXML
    private HBox itemsHeaderHBox, barcodeContainer, hboxVatTotal;
    @FXML
    private Label lblTicketTitle, lblDate, lblSubtotal, lblVat, lblTotal, lblPaid, lblChange, lblPaymentMethod, lblBarcodeValue, lblGiftIcon, lblGiftIndicator, lblPVPHeader, lblTotalHeader, lblRewardMsg, lblRewardCode, lblVatLabel, lblAttendedBy;
    @FXML
    private Label lblCompanyIcon, lblCompanyBrand, lblCompanyName, lblCompanyAddress, lblCompanyPhone, lblCompanyCif, lblFooterMessage;
    @FXML
    private javafx.scene.image.ImageView imgCompanyLogo;
    @FXML
    private ComboBox<Printer> cmbPrinters;
    @FXML
    private Button btnGiftTicket, btnNewSale;
    @FXML
    private VBox clientInfoSection;
    @FXML
    private Label lblClientName, lblClientTaxId, lblClientAddress;

    private boolean isGiftMode = false;
    private com.mycompany.ventacontrolfx.domain.model.Client currentClient;
    private ConfigUseCase configUseCase;
    private SaleConfig cfg;
    private ServiceContainer container;
    
    // Managers
    private PrinterManager printerManager;
    private DocumentFormatter docFormatter;
    private FiscalRenderer fiscalRenderer;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.configUseCase = container.getConfigUseCase();
        this.cfg = configUseCase.getConfig();
        
        this.printerManager = new PrinterManager();
        this.docFormatter = new DocumentFormatter(container, cfg);
        this.fiscalRenderer = new FiscalRenderer(container, cfg);
    }


    public void setReceiptData(List<CartItem> items, double total, double paid, double change, String paymentMethod, int saleId) {
        setReceiptData(items, total, paid, change, paymentMethod, saleId, false, null, 0, null);
    }

    public void setReceiptData(List<CartItem> items, double total, double paid, double change, String paymentMethod, int saleId, boolean isGift, String rewardCode, double rAmount, LocalDateTime rExpiry) {
        this.rewardPromoCode = rewardCode;
        this.rewardAmount = rAmount;
        this.rewardExpiryDate = rExpiry;
        this.isGiftMode = isGift;
        this.currentItems = items;
        this.currentTotal = total;
        this.currentPaid = paid;
        this.currentChange = change;
        this.currentPaymentMethod = paymentMethod;
        this.currentSaleId = saleId;

        // 1. Configuración de Recompensas
        if (rewardSection != null) {
            boolean hasReward = rewardCode != null && !rewardCode.isEmpty();
            rewardSection.setVisible(hasReward);
            rewardSection.setManaged(hasReward);
            if (hasReward) {
                if (lblRewardCode != null) lblRewardCode.setText(rewardCode);
                if (lblRewardMsg != null && rExpiry != null) {
                    lblRewardMsg.setText(String.format("Cup\u00f3n de %.0f\u20ac para tu pr\u00f3xima compra\nV\u00e1lido hasta: %s", rAmount, rExpiry.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
                }
            }
        }

        // 2. Impresoras y Fecha
        cmbPrinters.setItems(FXCollections.observableArrayList(Printer.getAllPrinters()));
        if (Printer.getDefaultPrinter() != null) cmbPrinters.getSelectionModel().select(Printer.getDefaultPrinter());
        
        LocalDateTime now = LocalDateTime.now();
        String terminal = (cfg != null && cfg.getTerminalName() != null) ? cfg.getTerminalName() : "Caja 01";
        lblDate.setText("Fecha: " + now.format(DateTimeFormatter.ofPattern("dd MMM, yyyy 'Hora:' HH:mm:ss")) + " " + terminal);

        // 3. Título y Artículos
        String prefix = isGiftMode ? "Ticket regalo N\u00ba: " : "Factura simplificada N\u00ba: ";
        if (currentTotal < 0) prefix = "FACTURA RECTIFICATIVA N\u00ba: ";
        lblTicketTitle.setText(prefix + String.format("%03d", saleId));

        itemsContainer.getChildren().clear();
        String sym = cfg.getCurrencySymbol();
        if (items != null) items.forEach(item -> addItemRow(item, sym));

        // 4. Totales e IVA
        renderTotals(total, paid, change, paymentMethod);

        // 5. Visibilidad y Estilo
        updateUIStates();

        // 6. Datos de empresa, formato y bloque fiscal
        docFormatter.applyCompanyHeader(lblCompanyBrand, lblCompanyName, lblCompanyAddress, lblCompanyPhone, lblCompanyCif, imgCompanyLogo, lblCompanyIcon);
        docFormatter.applyPaperLayout(paperSheet, currentClient != null);
        fiscalRenderer.renderFiscalBlock(paperSheet, currentSaleId);
    }

    private void renderTotals(double total, double paid, double change, String paymentMethod) {
        String sym = cfg.getCurrencySymbol();
        String fmt = "%." + cfg.getDecimalCount() + "f " + sym;
        
        java.util.Map<Double, Double[]> vatBreakdown = new java.util.TreeMap<>();
        boolean isInclusive = cfg.isPricesIncludeTax();
        double totalVatAmount = 0.0, totalSubtotal = 0.0;

        if (currentItems != null) {
            for (CartItem item : currentItems) {
                double lineTotal = item.getTotal();
                double rate = item.getProduct().resolveEffectiveIva(cfg.getTaxRate());
                double base = isInclusive ? (lineTotal / (1.0 + (rate / 100.0))) : lineTotal;
                double vat = isInclusive ? (lineTotal - base) : (lineTotal * (rate / 100.0));
                
                totalVatAmount += vat; totalSubtotal += base;
                Double[] vals = vatBreakdown.getOrDefault(rate, new Double[]{0.0, 0.0});
                vals[0] += base; vals[1] += vat;
                vatBreakdown.put(rate, vals);
            }
        }

        if (vatBreakdownContainer != null) {
            vatBreakdownContainer.getChildren().clear();
            vatBreakdown.forEach((rate, vals) -> {
                HBox row = new HBox(new Label(String.format("IVA %.0f%%", rate)), new Label(String.format(fmt, vals[1])));
                ((Label)row.getChildren().get(0)).setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(row.getChildren().get(0), Priority.ALWAYS);
                vatBreakdownContainer.getChildren().add(row);
            });
        }

        if (hboxVatTotal != null) {
            hboxVatTotal.setVisible(vatBreakdown.size() > 1);
            hboxVatTotal.setManaged(vatBreakdown.size() > 1);
        }

        lblSubtotal.setText(String.format(fmt, totalSubtotal));
        lblVat.setText(String.format(fmt, totalVatAmount));
        lblTotal.setText(String.format(fmt, total));
        lblPaid.setText(String.format(fmt, paid));
        if (lblChange != null) lblChange.setText(String.format(fmt, change));
        lblPaymentMethod.setText(paymentMethod);
    }

    private void updateUIStates() {
        totalsContainer.setVisible(!isGiftMode); totalsContainer.setManaged(!isGiftMode);
        lblGiftIndicator.setVisible(isGiftMode); lblGiftIndicator.setManaged(isGiftMode);
        lblPVPHeader.setVisible(!isGiftMode); lblPVPHeader.setManaged(!isGiftMode);
        lblTotalHeader.setVisible(!isGiftMode); lblTotalHeader.setManaged(!isGiftMode);
        paymentInfoContainer.setVisible(!isGiftMode); paymentInfoContainer.setManaged(!isGiftMode);
        
        if (isGiftMode) {
            btnGiftTicket.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-background-radius: 25; -fx-font-weight: bold; -fx-padding: 8 20; -fx-cursor: hand;");
            lblGiftIcon.setStyle("-fx-font-size: 16; -fx-text-fill: white;");
        } else {
            btnGiftTicket.setStyle("-fx-background-color: white; -fx-text-fill: #1a73e8; -fx-border-color: #1a73e8; -fx-border-radius: 25; -fx-border-width: 1; -fx-font-weight: bold; -fx-padding: 8 20; -fx-background-radius: 25; -fx-cursor: hand;");
            lblGiftIcon.setStyle("-fx-font-size: 16; -fx-text-fill: #1a73e8;");
        }
    }

    public void setClosureData(CashClosure closure) {
        this.cfg = configUseCase.getConfig();
        cmbPrinters.setItems(FXCollections.observableArrayList(Printer.getAllPrinters()));
        if (Printer.getDefaultPrinter() != null) cmbPrinters.getSelectionModel().select(Printer.getDefaultPrinter());

        btnGiftTicket.setVisible(false); btnGiftTicket.setManaged(false);
        btnNewSale.setVisible(false); btnNewSale.setManaged(false);

        docFormatter.applyCompanyHeader(lblCompanyBrand, lblCompanyName, lblCompanyAddress, lblCompanyPhone, lblCompanyCif, imgCompanyLogo, lblCompanyIcon);
        lblTicketTitle.setText("TICKET DE CIERRE #" + closure.getClosureId());
        lblDate.setText("Fecha: " + (closure.getCreatedAt() != null ? closure.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "N/D"));

        // Ocultar secciones de venta
        itemsHeaderHBox.setVisible(false); itemsHeaderHBox.setManaged(false);
        itemsContainer.setVisible(false); itemsContainer.setManaged(false);
        totalsContainer.setVisible(false); totalsContainer.setManaged(false);
        paymentInfoContainer.setVisible(false); paymentInfoContainer.setManaged(false);

        VBox closureDetails = new VBox(10);
        closureDetails.setStyle("-fx-padding: 15 0;");
        Label title = new Label("RESUMEN DE CAJA");
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-text-fill: black;");
        
        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setVgap(5); grid.setHgap(10);
        int row = 0;
        addGridRow(grid, row++, "Cajero:", closure.getUsername() != null ? closure.getUsername() : "N/D");
        addGridRow(grid, row++, "Fondo Inicial:", String.format("%.2f \u20ac", closure.getInitialFund()));
        addGridRow(grid, row++, "(+) Ventas:", String.format("%.2f \u20ac", closure.getTotalCash() - closure.getInitialFund() - closure.getCashIn() + closure.getCashOut()));
        addGridRow(grid, row++, "(+) Entradas:", String.format("%.2f \u20ac", closure.getCashIn()));
        addGridRow(grid, row++, "(-) Retiradas:", String.format("%.2f \u20ac", closure.getCashOut()));
        addGridRow(grid, row++, "Total Esperado:", String.format("%.2f \u20ac", closure.getExpectedCash()));
        addGridRow(grid, row++, "Total Contado:", String.format("%.2f \u20ac", closure.getActualCash()));
        addGridRow(grid, row++, "Diferencia:", String.format("%+.2f \u20ac", closure.getDifference()));

        closureDetails.getChildren().addAll(title, grid);
        if (closure.getNotes() != null && !closure.getNotes().isEmpty()) {
            Label notes = new Label(closure.getNotes()); notes.setWrapText(true); notes.setStyle("-fx-font-size: 10px; -fx-text-fill: black;");
            closureDetails.getChildren().add(notes);
        }

        paperSheet.getChildren().add(closureDetails);
        docFormatter.applyPaperLayout(paperSheet, false);
    }

    private void addGridRow(javafx.scene.layout.GridPane grid, int row, String labelText, String valueText) {
        Label l = new Label(labelText); l.setStyle("-fx-font-size: 10px; -fx-text-fill: black;");
        Label v = new Label(valueText); v.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: black;");
        grid.add(l, 0, row); grid.add(v, 1, row);
    }

    @FXML
    private void confirmPrint() {
        Printer selectedPrinter = cmbPrinters.getSelectionModel().getSelectedItem();
        boolean isA4 = (cfg != null && cfg.getTicketFormat().contains("A4")) || currentClient != null;
        if (printerManager.print(paperSheet, selectedPrinter, isA4)) closeWindow();
    }

    public void setReturnData(com.mycompany.ventacontrolfx.domain.model.Return returnRecord, com.mycompany.ventacontrolfx.domain.model.Sale originalSale, List<com.mycompany.ventacontrolfx.domain.model.ReturnDetail> details) {
        this.cfg = configUseCase.getConfig();
        String sym = cfg.getCurrencySymbol(), fmt = "%." + cfg.getDecimalCount() + "f " + sym;

        cmbPrinters.setItems(FXCollections.observableArrayList(Printer.getAllPrinters()));
        if (Printer.getDefaultPrinter() != null) cmbPrinters.getSelectionModel().select(Printer.getDefaultPrinter());

        lblDate.setText("Fecha Dev: " + returnRecord.getReturnDatetime().format(DateTimeFormatter.ofPattern("dd MMM, yyyy 'Hora:' HH:mm:ss")));
        lblTicketTitle.setText((returnRecord.getDocNumber() != null ? "FACTURA RECTIFICATIVA N\u00ba: " : "Ticket de Devoluci\u00f3n N\u00ba: REF-") + returnRecord.getFullReference());

        itemsContainer.getChildren().clear();
        details.forEach(detail -> {
            HBox row = new HBox(10); row.setStyle("-fx-border-color: transparent transparent #eee transparent; -fx-padding: 4 0;");
            Label d = new Label(detail.getProductName()); d.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(d, Priority.ALWAYS); d.setStyle("-fx-font-size: 10px;");
            Label q = new Label(String.valueOf(detail.getQuantity())); q.setMinWidth(30); q.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-size: 10px;");
            Label p = new Label(String.format(fmt, detail.getUnitPrice())); p.setMinWidth(55); p.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-size: 10px;");
            Label t = new Label(String.format(fmt, detail.getSubtotal())); t.setMinWidth(55); t.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-size: 10px;");
            row.getChildren().addAll(d, q, p, t); itemsContainer.getChildren().add(row);
        });

        lblTotal.setText("-" + String.format(fmt, returnRecord.getTotalRefunded()));
        lblSubtotal.setText("-" + String.format(fmt, returnRecord.getTaxBasis()));
        lblVat.setText("-" + String.format(fmt, returnRecord.getTotalTax()));
        lblPaymentMethod.setText(returnRecord.getPaymentMethod());
        if (lblPaid != null) lblPaid.setText(String.format(fmt, returnRecord.getTotalRefunded()));

        docFormatter.applyCompanyHeader(lblCompanyBrand, lblCompanyName, lblCompanyAddress, lblCompanyPhone, lblCompanyCif, imgCompanyLogo, lblCompanyIcon);
        docFormatter.applyPaperLayout(paperSheet, originalSale.getClientId() != null);
        fiscalRenderer.renderFiscalBlock(paperSheet, cfg.getCif(), returnRecord.getFullReference(), returnRecord.getReturnDatetime(), -returnRecord.getTotalRefunded(), returnRecord.getControlHash());
    }

    public void setClientInfo(com.mycompany.ventacontrolfx.domain.model.Client client) {
        this.currentClient = client;
        if (client != null) {
            docFormatter.applyPaperLayout(paperSheet, true);
            if (clientInfoSection != null) { clientInfoSection.setVisible(true); clientInfoSection.setManaged(true); }
            if (lblClientName != null) lblClientName.setText(client.getName());
            if (lblClientTaxId != null) lblClientTaxId.setText("CIF: " + client.getTaxId());
            if (lblClientAddress != null) lblClientAddress.setText(client.getAddress() + "\n" + client.getPostalCode() + " " + client.getCity());
            if (!isGiftMode && lblTicketTitle != null) lblTicketTitle.setText(lblTicketTitle.getText().replace("Factura simplificada", "Factura"));
        }
    }

    private void addItemRow(CartItem item, String sym) {
        HBox row = new HBox(10);
        boolean isA4 = (cfg != null && cfg.getTicketFormat().contains("A4")) || currentClient != null;
        if (isA4) { row.setMinWidth(650); row.setPrefWidth(650); row.setAlignment(Pos.CENTER_LEFT); }
        row.setStyle("-fx-border-color: transparent transparent #eee transparent; -fx-padding: " + (isA4 ? "8 5" : "2 0") + ";");

        Label d = new Label(translateDynamic(item.getProduct().getName()));
        if (isA4) { d.setMinWidth(340); d.setPrefWidth(340); } else { d.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(d, Priority.ALWAYS); }
        d.setStyle("-fx-font-size: " + (isA4 ? "12px" : "10px") + ";");

        Label q = new Label(String.valueOf(item.getQuantity()));
        q.setMinWidth(isA4 ? 80 : 30); q.setStyle("-fx-alignment: " + (isA4 ? "CENTER" : "CENTER-RIGHT") + "; -fx-font-size: " + (isA4 ? "12px" : "10px") + ";");
        row.getChildren().addAll(d, q);

        if (!isGiftMode) {
            String fmt = "%." + cfg.getDecimalCount() + "f " + sym;
            double pUnit = item.getProduct().getPrice(), lTotal = item.getTotal();
            if (cfg != null && !cfg.isPricesIncludeTax()) {
                double r = item.getProduct().resolveEffectiveIva(cfg.getTaxRate());
                pUnit *= (1 + r/100); lTotal *= (1 + r/100);
            }
            Label p = new Label(String.format(fmt, pUnit)); p.setMinWidth(isA4 ? 100 : 55); p.setStyle("-fx-alignment: " + (isA4 ? "CENTER" : "CENTER-RIGHT") + ";");
            Label t = new Label(String.format(fmt, lTotal)); t.setMinWidth(isA4 ? 100 : 55); t.setStyle("-fx-alignment: " + (isA4 ? "CENTER" : "CENTER-RIGHT") + ";");
            row.getChildren().addAll(p, t);
        }
        itemsContainer.getChildren().add(row);
    }

    private String translateDynamic(String text) {
        if (text == null || text.isBlank()) return text;
        return (container != null && container.getBundle() != null && container.getBundle().containsKey(text)) ? container.getBundle().getString(text) : text;
    }

    @FXML private void cancel() { closeWindow(); }
    private void closeWindow() { if (paperSheet.getScene() != null) ((Stage) paperSheet.getScene().getWindow()).close(); }
    
    private List<CartItem> currentItems;
    private double currentTotal, currentPaid, currentChange;
    private String currentPaymentMethod, rewardPromoCode;
    private int currentSaleId;
    private double rewardAmount;
    private LocalDateTime rewardExpiryDate;
}

