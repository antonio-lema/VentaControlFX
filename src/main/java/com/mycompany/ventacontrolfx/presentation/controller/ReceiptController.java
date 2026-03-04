package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.CartItem;
import com.mycompany.ventacontrolfx.domain.model.Client;
import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
import com.mycompany.ventacontrolfx.application.usecase.ConfigUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.ModalService;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReceiptController implements Injectable {

    @FXML
    private VBox receiptContent, companyHeaderSection, ticketInfoSection, itemsContainer, totalsContainer,
            barcodeSection, paymentInfoContainer, clientInfoSection;
    @FXML
    private HBox itemsHeaderHBox;
    @FXML
    private Label lblTicketTitle, lblSuccessMessage, lblDate, lblSubtotal, lblVat, lblTotal, lblPaid, lblChange,
            lblPaymentMethod, lblTotalRight, lblChangeRight, lblGiftIndicator, lblPVPHeader, lblTotalHeader,
            lblClientName, lblClientTaxId, lblClientAddress, lblCompanyBrand, lblCompanyName, lblCompanyAddress,
            lblCompanyPhone, lblCompanyCif, lblFooterMessage, lblSuccessIcon, lblGiftIcon, lblCompanyIcon,
            lblAttendedBy, lblWebsiteUrl;
    @FXML
    private ImageView imgCompanyLogo, imgAppLogoRight;
    @FXML
    private Button btnGiftTicket;

    private ServiceContainer container;
    private ConfigUseCase configUseCase;
    private SaleConfig cfg;
    private List<CartItem> currentItems;
    private double currentTotal, currentPaid, currentChange;
    private String currentPaymentMethod;
    private int currentSaleId;
    private Client currentClient;
    private boolean isGiftMode = false;
    private Runnable onNewSaleAction;
    private Runnable onBackAction;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.configUseCase = container.getConfigUseCase();
        this.cfg = configUseCase.getConfig();
    }

    public void setReceiptData(List<CartItem> items, double total, double paid, double change, String paymentMethod,
            int saleId, Runnable onNewSale, Runnable onBack) {
        this.currentItems = items;
        this.currentTotal = total;
        this.currentPaid = paid;
        this.currentChange = change;
        this.currentPaymentMethod = paymentMethod;
        this.currentSaleId = saleId;
        this.onNewSaleAction = onNewSale;
        this.onBackAction = onBack;

        String sym = cfg.getCurrencySymbol();
        String fmt = "%." + cfg.getDecimalCount() + "f " + sym;

        lblDate.setText(
                "Fecha: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM, yyyy 'Hora:' HH:mm:ss")));
        lblTicketTitle.setText(
                (isGiftMode ? "Ticket regalo Nº: " : "Factura simplificada Nº: ") + String.format("%03d", saleId));

        if (lblSuccessMessage != null) {
            lblSuccessMessage.setText(isGiftMode ? "¡Ticket regalo creado!"
                    : (currentClient != null ? "¡Factura creada!" : "¡Ticket creado!"));
        }

        applyCompanyHeader();
        itemsContainer.getChildren().clear();
        for (CartItem item : items)
            addItemRow(item, sym);

        totalsContainer.setVisible(!isGiftMode);
        totalsContainer.setManaged(!isGiftMode);
        paymentInfoContainer.setVisible(!isGiftMode);
        paymentInfoContainer.setManaged(!isGiftMode);
        lblGiftIndicator.setVisible(isGiftMode);
        lblGiftIndicator.setManaged(isGiftMode);

        double totalVatAmount = 0.0;
        double totalSubtotal = 0.0;

        for (CartItem item : items) {
            double itemTotal = item.getTotal();
            double effectiveRate = item.getProduct().resolveEffectiveIva(cfg.getTaxRate());
            double itemSubtotal = itemTotal / (1.0 + (effectiveRate / 100.0));
            totalVatAmount += (itemTotal - itemSubtotal);
            totalSubtotal += itemSubtotal;
        }

        lblSubtotal.setText(String.format(fmt, totalSubtotal));
        lblVat.setText("IVA Incl. " + String.format(fmt, totalVatAmount));
        lblTotal.setText(String.format(fmt, total));
        lblPaid.setText(String.format(fmt, paid));
        lblChange.setText(String.format(fmt, change));
        lblPaymentMethod.setText(paymentMethod);

        if (lblTotalRight != null)
            lblTotalRight.setText(String.format(fmt, total));
        if (lblChangeRight != null)
            lblChangeRight.setText(String.format(fmt, change));

        applyPaperFormat();
    }

    private void applyCompanyHeader() {
        if (cfg == null)
            return;
        if (cfg.isShowLogo() && cfg.getLogoPath() != null) {
            File f = new File(cfg.getLogoPath());
            if (f.exists()) {
                Image img = new Image(f.toURI().toString());
                if (imgCompanyLogo != null) {
                    imgCompanyLogo.setImage(img);
                    imgCompanyLogo.setVisible(true);
                }
                if (lblCompanyIcon != null)
                    lblCompanyIcon.setVisible(false);
            }
        }
        if (lblCompanyBrand != null)
            lblCompanyBrand.setText(cfg.getCompanyName());
        setLabelText(lblCompanyName, cfg.getCompanyName());
        setLabelText(lblCompanyAddress, cfg.isShowAddress() ? cfg.getAddress() : "");
        setLabelText(lblCompanyPhone, cfg.isShowPhone() ? "Tel: " + cfg.getPhone() : "");
        setLabelText(lblCompanyCif, cfg.isShowCif() ? "CIF: " + cfg.getCif() : "");
        setLabelText(lblFooterMessage,
                cfg.getFooterMessage() != null ? cfg.getFooterMessage() : "GRACIAS POR SU VISITA");

        if (lblAttendedBy != null && container != null && container.getUserSession() != null
                && container.getUserSession().getCurrentUser() != null) {
            setLabelText(lblAttendedBy, "Le ha atendido: " + container.getUserSession().getCurrentUser().getUsername());
        } else {
            setLabelText(lblAttendedBy, "");
        }
        setLabelText(lblWebsiteUrl, "");
    }

    private void setLabelText(Label lbl, String text) {
        if (lbl == null)
            return;
        lbl.setText(text != null ? text : "");
        lbl.setVisible(text != null && !text.isEmpty());
        lbl.setManaged(lbl.isVisible());
    }

    public void setClientInfo(Client client) {
        this.currentClient = client;
        if (client != null) {
            if (clientInfoSection != null) {
                clientInfoSection.setVisible(true);
                clientInfoSection.setManaged(true);
            }
            if (lblClientName != null)
                lblClientName.setText(client.getName());
            if (lblClientTaxId != null)
                lblClientTaxId.setText("CIF: " + client.getTaxId());
            if (lblClientAddress != null)
                lblClientAddress.setText(client.getAddress());
            applyPaperFormat();
        }
    }

    @FXML
    private void handlePrint() {
        ModalService.showStandardModal("/view/print_preview.fxml", "Vista Previa", container,
                (PrintPreviewController ctrl) -> {
                    if (currentClient != null)
                        ctrl.setClientInfo(currentClient);
                    ctrl.setReceiptData(currentItems, currentTotal, currentPaid, currentChange, currentPaymentMethod,
                            currentSaleId,
                            isGiftMode);
                });
    }

    @FXML
    private void handleBack() {
        ((Stage) receiptContent.getScene().getWindow()).close();
        if (onBackAction != null) {
            onBackAction.run();
        }
    }

    @FXML
    private void handleNewSale() {
        ((Stage) receiptContent.getScene().getWindow()).close();
        if (onNewSaleAction != null) {
            onNewSaleAction.run();
        }
    }

    @FXML
    private void handleGiftTicket() {
        this.isGiftMode = !this.isGiftMode;
        setReceiptData(currentItems, currentTotal, currentPaid, currentChange, currentPaymentMethod, currentSaleId,
                onNewSaleAction, onBackAction);
    }

    private void addItemRow(CartItem item, String sym) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-border-color: transparent transparent #eee transparent; -fx-padding: 5 0;");

        Label desc = new Label(item.getProduct().getName());
        HBox.setHgrow(desc, Priority.ALWAYS);
        desc.setMaxWidth(Double.MAX_VALUE);

        Label qty = new Label(String.valueOf(item.getQuantity()));
        qty.setPrefWidth(30);
        qty.setAlignment(Pos.CENTER_RIGHT);

        row.getChildren().addAll(desc, qty);

        if (!isGiftMode) {
            Label price = new Label(String.format("%.2f %s", item.getTotal(), sym));
            price.setPrefWidth(70);
            price.setAlignment(Pos.CENTER_RIGHT);
            row.getChildren().add(price);
        }
        itemsContainer.getChildren().add(row);
    }

    private void applyPaperFormat() {
        // Simplified formatting logic for now
        if (currentClient != null)
            receiptContent.setPrefWidth(700);
        else
            receiptContent.setPrefWidth(300);
    }
}
