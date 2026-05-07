package com.mycompany.ventacontrolfx.presentation.controller.receipt;

import com.mycompany.ventacontrolfx.domain.model.*;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.controller.dialog.PrintPreviewController;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import com.mycompany.ventacontrolfx.presentation.navigation.ModalService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReceiptController implements Injectable {

    @FXML private VBox receiptContent, itemsContainer, totalsContainer, vatBreakdownContainer, hboxSavings, observationSection, rewardSection, paymentInfoContainer, clientInfoSection;
    @FXML private HBox hboxVatTotal;
    @FXML private Label lblTicketTitle, lblSuccessMessage, lblDate, lblSubtotal, lblVat, lblTotal, lblPaid, lblChange, lblPaymentMethod, lblTotalRight, lblChangeRight, lblGiftIndicator, lblClientName, lblClientTaxId, lblClientAddress, lblCompanyBrand, lblCompanyName, lblCompanyAddress, lblCompanyPhone, lblCompanyCif, lblFooterMessage, lblAttendedBy, lblSavings, lblObservations, lblRewardMsg, lblRewardCode;
    @FXML private ImageView imgCompanyLogo;
    @FXML private Button btnGiftTicket, btnSendEmail;

    private ServiceContainer container;
    private ReceiptFiscalManager fiscalManager;
    private ReceiptTaxManager taxManager;
    private ReceiptMailManager mailManager;
    private SaleConfig cfg;

    private List<CartItem> currentItems;
    private double currentTotal, currentPaid, currentChange;
    private String currentPaymentMethod;
    private int currentSaleId;
    private Client currentClient;
    private boolean isGiftMode = false;
    private String rewardPromoCode;
    private double rewardAmount;
    private LocalDateTime rewardExpiryDate;
    private Runnable onNewSaleAction, onBackAction;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.cfg = container.getConfigUseCase().getConfig();
        this.fiscalManager = new ReceiptFiscalManager(container);
        this.taxManager = new ReceiptTaxManager(container);
        this.mailManager = new ReceiptMailManager(container);
    }

    public void setReceiptData(List<CartItem> items, double total, double paid, double change, String paymentMethod, int saleId, Runnable onNewSale, Runnable onBack, String observations, String rewardCode, double rewardAmount, LocalDateTime expiryDate) {
        this.currentItems = items; this.currentTotal = total; this.currentPaid = paid; this.currentChange = change;
        this.currentPaymentMethod = paymentMethod; this.currentSaleId = saleId; this.onNewSaleAction = onNewSale; this.onBackAction = onBack;
        this.rewardPromoCode = rewardCode; this.rewardAmount = rewardAmount; this.rewardExpiryDate = expiryDate;

        renderHeader(saleId);
        renderItems(items);
        renderTotals(paid, change, paymentMethod, observations);
        renderReward(rewardCode, rewardAmount, expiryDate);
        fiscalManager.renderVerifactuSection(receiptContent, saleId, cfg);
        applyPaperFormat();
    }

    private void renderHeader(int saleId) {
        lblDate.setText(container.getBundle().getString("receipt.info.date") + ": " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM, yyyy 'Hora:' HH:mm:ss")));
        String title = isGiftMode ? container.getBundle().getString("receipt.title.gift") : container.getBundle().getString("receipt.title.invoice");
        if (currentTotal < 0) title = "FACTURA RECTIFICATIVA";
        lblTicketTitle.setText(title + " N\u00ba: " + String.format("%03d", saleId));
        if (lblSuccessMessage != null) lblSuccessMessage.setText(isGiftMode ? container.getBundle().getString("receipt.success.gift_title") : container.getBundle().getString("receipt.success.title"));
        applyCompanyInfo();
    }

    private void applyCompanyInfo() {
        if (cfg.isShowLogo() && cfg.getLogoPath() != null) {
            File f = new File(cfg.getLogoPath());
            if (f.exists()) imgCompanyLogo.setImage(new Image(f.toURI().toString()));
        }
        lblCompanyBrand.setText(cfg.getCompanyName());
        setLabel(lblCompanyName, cfg.getCompanyName());
        setLabel(lblCompanyAddress, cfg.isShowAddress() ? cfg.getAddress() : "");
        setLabel(lblCompanyPhone, cfg.isShowPhone() ? "Tel: " + cfg.getPhone() : "");
        setLabel(lblCompanyCif, cfg.isShowCif() ? "CIF: " + cfg.getCif() : "");
        setLabel(lblFooterMessage, cfg.getFooterMessage() != null ? cfg.getFooterMessage() : container.getBundle().getString("receipt.footer.thanks"));
        if (lblAttendedBy != null && container.getUserSession().getCurrentUser() != null) setLabel(lblAttendedBy, container.getBundle().getString("receipt.attended_by") + ": " + container.getUserSession().getCurrentUser().getUsername());
    }

    private void renderItems(List<CartItem> items) {
        itemsContainer.getChildren().clear();
        String sym = cfg.getCurrencySymbol();
        if (items == null || items.isEmpty()) return;

        for (CartItem item : items) {
            HBox row = new HBox(10); row.setAlignment(Pos.CENTER_LEFT); row.setStyle("-fx-border-color: transparent transparent #eee transparent; -fx-padding: 5 0;");
            Label desc = new Label(item.getProduct().getName()); HBox.setHgrow(desc, Priority.ALWAYS); desc.setMaxWidth(Double.MAX_VALUE);
            Label qty = new Label(String.valueOf(item.getQuantity())); qty.setPrefWidth(30); qty.setAlignment(Pos.CENTER_RIGHT);
            row.getChildren().addAll(desc, qty);
            if (!isGiftMode) {
                double priceVal = item.getTotal();
                if (!cfg.isPricesIncludeTax()) priceVal *= (1 + (item.getProduct().resolveEffectiveIva(cfg.getTaxRate()) / 100.0));
                Label price = new Label(String.format("%.2f %s", priceVal, sym)); price.setPrefWidth(70); price.setAlignment(Pos.CENTER_RIGHT);
                row.getChildren().add(price);
            }
            itemsContainer.getChildren().add(row);
            if (!isGiftMode && item.getDiscountAmount() > 0) addDiscountRow(item, sym);
        }
    }

    private void addDiscountRow(CartItem item, String sym) {
        HBox row = new HBox(10); row.setAlignment(Pos.CENTER_LEFT); row.setStyle("-fx-padding: 2 0 5 15;");
        Label desc = new Label("[DESC] " + container.getBundle().getString("receipt.savings_label"));
        desc.setStyle("-fx-font-size: 10; -fx-text-fill: #666; -fx-font-style: italic;"); HBox.setHgrow(desc, Priority.ALWAYS);
        double disc = item.getDiscountAmount();
        if (!cfg.isPricesIncludeTax()) disc *= (1 + (item.getProduct().resolveEffectiveIva(cfg.getTaxRate()) / 100.0));
        Label price = new Label(String.format("-%.2f %s", disc, sym));
        price.setStyle("-fx-font-size: 10; -fx-text-fill: #666; -fx-font-style: italic;"); price.setPrefWidth(70); price.setAlignment(Pos.CENTER_RIGHT);
        row.getChildren().addAll(desc, price); itemsContainer.getChildren().add(row);
    }

    private void renderTotals(double paid, double change, String method, String observations) {
        String sym = cfg.getCurrencySymbol(); String fmt = "%." + cfg.getDecimalCount() + "f " + sym;
        totalsContainer.setVisible(!isGiftMode); totalsContainer.setManaged(!isGiftMode);
        paymentInfoContainer.setVisible(!isGiftMode); paymentInfoContainer.setManaged(!isGiftMode);
        lblGiftIndicator.setVisible(isGiftMode); lblGiftIndicator.setManaged(isGiftMode);

        if (!isGiftMode) {
            taxManager.renderVatBreakdown(vatBreakdownContainer, currentItems, cfg, fmt);
            double savings = taxManager.calculateTotalSavings(currentItems, cfg);
            lblSavings.setText(String.format("- " + fmt, savings));
            hboxSavings.setVisible(savings > 0); hboxSavings.setManaged(savings > 0);
            lblTotal.setText(String.format(fmt, currentTotal)); lblPaid.setText(String.format(fmt, paid));
            lblChange.setText(String.format(fmt, change)); lblPaymentMethod.setText(method);
            if (lblTotalRight != null) lblTotalRight.setText(String.format(fmt, currentTotal));
            if (lblChangeRight != null) lblChangeRight.setText(String.format(fmt, change));
        }
        if (observationSection != null) {
            boolean hasObs = observations != null && !observations.trim().isEmpty();
            observationSection.setVisible(hasObs); observationSection.setManaged(hasObs);
            if (hasObs) lblObservations.setText(observations);
        }
    }

    private void renderReward(String code, double amount, LocalDateTime expiry) {
        if (rewardSection == null) return;
        boolean has = code != null && !code.isEmpty();
        rewardSection.setVisible(has); rewardSection.setManaged(has);
        if (has) {
            lblRewardCode.setText(code);
            lblRewardMsg.setText(String.format("Cup\u00f3n de %.0f\u20ac para pr\u00f3xima compra\nV\u00e1lido hasta: %s", amount, expiry.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))));
        }
    }

    public void setClientInfo(Client client) {
        this.currentClient = client;
        if (client != null) {
            clientInfoSection.setVisible(true); clientInfoSection.setManaged(true);
            lblClientName.setText(client.getName()); lblClientTaxId.setText("CIF: " + client.getTaxId()); lblClientAddress.setText(client.getAddress());
            if (client.getEmail() != null && !client.getEmail().isBlank()) { btnSendEmail.setVisible(true); btnSendEmail.setManaged(true); }
            applyPaperFormat();
        }
    }

    @FXML private void handleSendEmail() { mailManager.sendInvoiceEmail(currentClient, currentSaleId, cfg, () -> {}, e -> AlertUtil.showError("Error", e.getMessage())); }
    @FXML private void handlePrint() {
        ModalService.showStandardModal("/view/receipt/print_preview.fxml", container.getBundle().getString("receipt.print_preview"), container, (PrintPreviewController ctrl) -> {
            if (currentClient != null) ctrl.setClientInfo(currentClient);
            ctrl.setReceiptData(currentItems, currentTotal, currentPaid, currentChange, currentPaymentMethod, currentSaleId, isGiftMode, rewardPromoCode, rewardAmount, rewardExpiryDate);
        });
    }

    @FXML private void handleBack() { ((Stage) receiptContent.getScene().getWindow()).close(); if (onBackAction != null) onBackAction.run(); }
    @FXML private void handleNewSale() { ((Stage) receiptContent.getScene().getWindow()).close(); if (onNewSaleAction != null) onNewSaleAction.run(); }
    @FXML private void handleGiftTicket() { this.isGiftMode = !this.isGiftMode; setReceiptData(currentItems, currentTotal, currentPaid, currentChange, currentPaymentMethod, currentSaleId, onNewSaleAction, onBackAction, null, rewardPromoCode, rewardAmount, rewardExpiryDate); }

    private void setLabel(Label lbl, String text) { if (lbl != null) { lbl.setText(text != null ? text : ""); lbl.setVisible(text != null && !text.isEmpty()); lbl.setManaged(lbl.isVisible()); } }
    private void applyPaperFormat() { receiptContent.setPrefWidth(currentClient != null ? 700 : 300); }

    // Fallback methods for older calls
    public void setReceiptData(List<CartItem> items, double total, double paid, double change, String paymentMethod, int saleId, Runnable onNewSale, Runnable onBack) { setReceiptData(items, total, paid, change, paymentMethod, saleId, onNewSale, onBack, null, null, 0, null); }
    public void setReceiptData(List<CartItem> items, double total, double paid, double change, String paymentMethod, int saleId, Runnable onNewSale, Runnable onBack, String observations) { setReceiptData(items, total, paid, change, paymentMethod, saleId, onNewSale, onBack, observations, null, 0, null); }
}



