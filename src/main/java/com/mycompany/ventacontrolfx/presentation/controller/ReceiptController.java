package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.CartItem;
import com.mycompany.ventacontrolfx.domain.model.Client;
import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
import com.mycompany.ventacontrolfx.domain.repository.IEmailSender;
import com.mycompany.ventacontrolfx.application.ports.IFiscalPdfService;
import com.mycompany.ventacontrolfx.application.usecase.ConfigUseCase;
import com.mycompany.ventacontrolfx.application.usecase.QueryFiscalDocumentUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.ModalService;
import com.mycompany.ventacontrolfx.util.AlertUtil;
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
            barcodeSection, paymentInfoContainer, clientInfoSection, vatBreakdownContainer, hboxSavings,
            observationSection;
    @FXML
    private HBox itemsHeaderHBox, hboxVatTotal, barcodeContainer;
    @FXML
    private Label lblTicketTitle, lblSuccessMessage, lblDate, lblSubtotal, lblVat, lblTotal, lblPaid, lblChange,
            lblPaymentMethod, lblTotalRight, lblChangeRight, lblGiftIndicator, lblPVPHeader, lblTotalHeader,
            lblClientName, lblClientTaxId, lblClientAddress, lblCompanyBrand, lblCompanyName, lblCompanyAddress,
            lblCompanyPhone, lblCompanyCif, lblFooterMessage, lblSuccessIcon, lblGiftIcon, lblCompanyIcon,
            lblAttendedBy, lblWebsiteUrl, lblSavings, lblObservations, lblModalTitle, lblBarcodeValue;
    @FXML
    private ImageView imgCompanyLogo, imgAppLogoRight;
    @FXML
    private Button btnGiftTicket, btnSendEmail;

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
        setReceiptData(items, total, paid, change, paymentMethod, saleId, onNewSale, onBack, null);
    }

    public void setReceiptData(List<CartItem> items, double total, double paid, double change, String paymentMethod,
            int saleId, Runnable onNewSale, Runnable onBack, String observations) {
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
                container.getBundle().getString("receipt.info.date") + ": "
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM, yyyy '"
                                + container.getBundle().getString("receipt.info.time") + ":' HH:mm:ss")));
        lblTicketTitle.setText(
                (isGiftMode ? container.getBundle().getString("receipt.title.gift") + " N\u00ba: "
                        : container.getBundle().getString("receipt.title.invoice") + " N\u00ba: ")
                        + String.format("%03d", saleId));

        if (lblSuccessMessage != null) {
            lblSuccessMessage.setText(isGiftMode ? container.getBundle().getString("receipt.success.gift_title")
                    : container.getBundle().getString("receipt.success.title"));
        }

        applyCompanyHeader();
        itemsContainer.getChildren().clear();
        
        System.out.println("[Receipt] Setting data for " + (items != null ? items.size() : "null") + " items");
        
        if (items == null || items.isEmpty()) {
            Label placeholder = new Label(container.getBundle().getString("receipt.items.empty"));
            placeholder.setStyle("-fx-text-fill: grey; -fx-font-style: italic;");
            itemsContainer.getChildren().add(placeholder);
        }

        double totalSavings = 0;
        if (items != null) {
            for (CartItem item : items) {
                addItemRow(item, sym);
                double disc = item.getDiscountAmount();
                if (!cfg.isPricesIncludeTax()) {
                    double rate = item.getProduct().resolveEffectiveIva(cfg.getTaxRate());
                    disc *= (1 + (rate / 100.0));
                }
                totalSavings += disc;
            }
        }

        if (lblSavings != null) {
            lblSavings.setText(String.format("- " + fmt, totalSavings));
            if (hboxSavings != null) {
                hboxSavings.setVisible(totalSavings > 0);
                hboxSavings.setManaged(totalSavings > 0);
            }
        }

        totalsContainer.setVisible(!isGiftMode);
        totalsContainer.setManaged(!isGiftMode);
        paymentInfoContainer.setVisible(!isGiftMode);
        paymentInfoContainer.setManaged(!isGiftMode);
        lblGiftIndicator.setVisible(isGiftMode);
        lblGiftIndicator.setManaged(isGiftMode);

        double totalVatAmount = 0.0;
        double totalSubtotal = 0.0;
        boolean isInclusive = cfg.isPricesIncludeTax();

        java.util.Map<Double, Double[]> vatBreakdown = new java.util.TreeMap<>();

        for (CartItem item : items) {
            double lineTotal = item.getTotal();
            double effectiveRate = item.getProduct().resolveEffectiveIva(cfg.getTaxRate());

            double itemSubtotal;
            double itemVat;
            if (isInclusive) {
                itemSubtotal = lineTotal / (1.0 + (effectiveRate / 100.0));
                itemVat = lineTotal - itemSubtotal;
            } else {
                itemSubtotal = lineTotal;
                itemVat = lineTotal * (effectiveRate / 100.0);
            }
            totalVatAmount += itemVat;
            totalSubtotal += itemSubtotal;

            Double[] vals = vatBreakdown.getOrDefault(effectiveRate, new Double[] { 0.0, 0.0 });
            vals[0] += itemSubtotal;
            vals[1] += itemVat;
            vatBreakdown.put(effectiveRate, vals);
        }

        // Render breakdown
        if (vatBreakdownContainer != null) {
            vatBreakdownContainer.getChildren().clear();
            for (java.util.Map.Entry<Double, Double[]> entry : vatBreakdown.entrySet()) {
                HBox row = new HBox();
                Label lblRate = new Label(container.getBundle().getString("receipt.vat_label")
                        + String.format(" %.0f%%", entry.getKey()));
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
        lblVat.setText(
                container.getBundle().getString("receipt.vat_label") + " Incl. " + String.format(fmt, totalVatAmount));
        lblTotal.setText(String.format(fmt, currentTotal));
        lblPaid.setText(String.format(fmt, paid));
        lblChange.setText(String.format(fmt, change));
        lblPaymentMethod.setText(paymentMethod);

        if (lblTotalRight != null)
            lblTotalRight.setText(String.format(fmt, total));
        if (lblChangeRight != null)
            lblChangeRight.setText(String.format(fmt, change));

        // Display general observation
        if (observationSection != null && lblObservations != null) {
            boolean hasObs = observations != null && !observations.trim().isEmpty();
            observationSection.setVisible(hasObs);
            observationSection.setManaged(hasObs);
            if (hasObs) {
                lblObservations.setText(observations);
            }
        }

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
                cfg.getFooterMessage() != null ? cfg.getFooterMessage()
                        : container.getBundle().getString("receipt.footer.thanks"));

        if (lblAttendedBy != null && container != null && container.getUserSession() != null
                && container.getUserSession().getCurrentUser() != null) {
            setLabelText(lblAttendedBy, container.getBundle().getString("receipt.attended_by") + ": "
                    + container.getUserSession().getCurrentUser().getUsername());
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

            if (client.getEmail() != null && !client.getEmail().trim().isEmpty()) {
                if (btnSendEmail != null) {
                    btnSendEmail.setVisible(true);
                    btnSendEmail.setManaged(true);
                }
            }
            applyPaperFormat();
        }
    }

    @FXML
    private void handleSendEmail() {
        if (currentClient == null || currentClient.getEmail() == null || currentClient.getEmail().trim().isEmpty()) {
            AlertUtil.showWarning(container.getBundle().getString("alert.warning"),
                    container.getBundle().getString("receipt.error.no_email"));
            return;
        }

        container.getAsyncManager().runAsyncTask(() -> {
            // 1. Obtener datos fiscales para el PDF
            QueryFiscalDocumentUseCase queryUseCase = container.getQueryFiscalDocumentUseCase();
            QueryFiscalDocumentUseCase.PrintData data = queryUseCase.getDataForReprint(currentSaleId);

            // 2. Adjuntar logo si est\u00e1 disponible
            if (cfg != null && cfg.getLogoPath() != null && !cfg.getLogoPath().trim().isEmpty()) {
                data.logoPath = cfg.getLogoPath();
            }

            // 3. Generar PDF en memoria
            IFiscalPdfService pdfService = container.getPdfService();
            byte[] pdfBytes = pdfService.generateInvoicePdfBytes(data);

            // 4. Preparar y enviar Email
            IEmailSender emailSender = container.getEmailSender();
            String subject = "Su factura de " + (cfg != null ? cfg.getCompanyName() : "GestionTPV");
            String body = "Estimado/a " + currentClient.getName() + ",\n\n"
                    + "Adjuntamos la factura correspondiente a su compra con ID #" + currentSaleId + ".\n\n"
                    + "Gracias por su confianza.\n\n"
                    + "Atentamente,\n" + (cfg != null ? cfg.getCompanyName() : "El equipo de Ventas");
            String fileName = "Factura_" + currentSaleId + ".pdf";

            emailSender.sendWithAttachment(
                    currentClient.getEmail(),
                    subject,
                    body,
                    pdfBytes,
                    fileName);
            return true;
        }, result -> {
            AlertUtil.showToast(
                    container.getBundle().getString("receipt.success.email_sent") + ": " + currentClient.getEmail());
        }, error -> {
            error.printStackTrace();
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("receipt.error.email_failed") + ": " + error.getMessage());
        });
    }

    @FXML
    private void handlePrint() {
        ModalService.showStandardModal("/view/print_preview.fxml",
                container.getBundle().getString("receipt.print_preview"), container,
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

        Label desc = new Label(translateDynamic(item.getProduct().getName()));
        HBox.setHgrow(desc, Priority.ALWAYS);
        desc.setMaxWidth(Double.MAX_VALUE);

        Label qty = new Label(String.valueOf(item.getQuantity()));
        qty.setPrefWidth(30);
        qty.setAlignment(Pos.CENTER_RIGHT);

        row.getChildren().addAll(desc, qty);

        if (!isGiftMode) {
            double originalUnitPrice = item.getUnitPrice();
            double originalLineTotal = originalUnitPrice * item.getQuantity();

            if (cfg != null && !cfg.isPricesIncludeTax()) {
                double rate = item.getProduct().resolveEffectiveIva(cfg.getTaxRate());
                originalLineTotal = originalLineTotal * (1 + (rate / 100.0));
            }

            Label price = new Label(String.format("%.2f %s", originalLineTotal, sym));
            price.setPrefWidth(70);
            price.setAlignment(Pos.CENTER_RIGHT);
            row.getChildren().add(price);
        }
        itemsContainer.getChildren().add(row);

        // Si hay descuento, a\u00f1adir una l\u00ednea extra negativa justo debajo
        if (!isGiftMode && item.getDiscountAmount() > 0) {
            HBox discountRow = new HBox(10);
            discountRow.setAlignment(Pos.CENTER_LEFT);
            discountRow.setStyle("-fx-padding: 2 0 5 15;"); // Indentaci\u00f3n
            Label discDesc = new Label("[DESC] " + container.getBundle().getString("receipt.savings_label"));
            discDesc.setStyle("-fx-font-size: 10; -fx-text-fill: #666; -fx-font-style: italic;");
            HBox.setHgrow(discDesc, Priority.ALWAYS);
            discDesc.setMaxWidth(Double.MAX_VALUE);

            double discountAmount = item.getDiscountAmount();
            if (cfg != null && !cfg.isPricesIncludeTax()) {
                double rate = item.getProduct().resolveEffectiveIva(cfg.getTaxRate());
                discountAmount *= (1 + (rate / 100.0));
            }

            Label discPrice = new Label(String.format("-%.2f %s", discountAmount, sym));
            discPrice.setStyle("-fx-font-size: 10; -fx-text-fill: #666; -fx-font-style: italic;");
            discPrice.setPrefWidth(70);
            discPrice.setAlignment(Pos.CENTER_RIGHT);

            discountRow.getChildren().addAll(discDesc, discPrice);
            itemsContainer.getChildren().add(discountRow);
        }
    }

    private void applyPaperFormat() {
        // Simplified formatting logic for now
        if (currentClient != null)
            receiptContent.setPrefWidth(700);
        else
            receiptContent.setPrefWidth(300);
    }
    private String translateDynamic(String text) {
        if (text == null || text.isBlank()) return text;
        if (container != null && container.getBundle() != null && container.getBundle().containsKey(text)) {
            return container.getBundle().getString(text);
        }
        return text;
    }
}
