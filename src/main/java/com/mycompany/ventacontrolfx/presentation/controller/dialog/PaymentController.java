package com.mycompany.ventacontrolfx.presentation.controller.dialog;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;
import javafx.application.Platform;

import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;

public class PaymentController implements Injectable {

    private ServiceContainer container;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        
        // Handle payment method changes
        tgPaymentMethod.selectedToggleProperty().addListener((obs, old, nv) -> {
            updateUIForMethod();
        });

        // Handle amount input changes for real-time calculations
        txtGivenAmount.textProperty().addListener((obs, old, nv) -> {
            updateCalculations();
        });
    }

    @FXML
    private Label lblTotalAmount, lblAmountTitle, lblAmountValue, lblCashWarning;
    @FXML
    private TextField txtGivenAmount;
    @FXML
    private Button btnQuick1, btnQuick2, btnQuick3, btnQuick4, btnConfirmPay;
    @FXML
    private javafx.scene.layout.VBox amountSection, vboxCashWarning, cardInstructionView;
    @FXML
    private ToggleButton btnPayCash, btnPayCard, btnPayMixed;
    @FXML
    private ToggleGroup tgPaymentMethod;
    @FXML
    private javafx.scene.layout.GridPane gridQuickButtons;

    public interface PaymentCallback {
        void onSuccess(double paid, double change, String method, double cashAmount, double cardAmount);
    }

    private double totalAmount;
    private PaymentCallback callback;

    public void initialize() {
        Platform.runLater(() -> txtGivenAmount.requestFocus());
    }

    public void setTotalAmount(double amount, PaymentCallback callback) {
        this.totalAmount = amount;
        this.callback = callback;

        String formatted = String.format("%.2f \u20ac", amount);
        lblTotalAmount.setText(formatted);
        lblAmountValue.setText(formatted);
        txtGivenAmount.setText(String.format("%.2f", amount).replace(',', '.'));

        if (amount > 1000) {
            btnPayCash.setDisable(true);
            btnPayMixed.setDisable(true);
            tgPaymentMethod.selectToggle(btnPayCard);
            vboxCashWarning.setVisible(true);
            vboxCashWarning.setManaged(true);
        } else {
            btnPayCash.setDisable(false);
            btnPayMixed.setDisable(false);
            vboxCashWarning.setVisible(false);
            vboxCashWarning.setManaged(false);
        }

        setupQuickButtons(amount);
        updateUIForMethod();
    }

    private void updateUIForMethod() {
        boolean isCard = tgPaymentMethod.getSelectedToggle() == btnPayCard;
        boolean isMixed = tgPaymentMethod.getSelectedToggle() == btnPayMixed;
        
        amountSection.setVisible(!isCard);
        amountSection.setManaged(!isCard);
        
        cardInstructionView.setVisible(isCard);
        cardInstructionView.setManaged(isCard);
        
        if (isMixed) {
            lblAmountTitle.setText(container.getBundle().getString("payment.method.mixed"));
            gridQuickButtons.setVisible(false);
            gridQuickButtons.setManaged(false);
        } else {
            lblAmountTitle.setText(container.getBundle().getString("payment.amount_given"));
            gridQuickButtons.setVisible(true);
            gridQuickButtons.setManaged(true);
        }
        updateCalculations();
    }

    private void updateCalculations() {
        boolean isCard = tgPaymentMethod.getSelectedToggle() == btnPayCard;
        boolean isMixed = tgPaymentMethod.getSelectedToggle() == btnPayMixed;
        
        if (isCard) return;

        try {
            String text = txtGivenAmount.getText().trim().replace(",", ".");
            if (text.isEmpty()) {
                lblAmountValue.setText(String.format("%.2f \u20ac", totalAmount));
                return;
            }

            double given = Double.parseDouble(text);
            if (isMixed) {
                double remaining = totalAmount - given;
                if (remaining > 0) {
                    lblAmountValue.setText(String.format("%.2f \u20ac (Tarj)", remaining));
                    lblAmountValue.setStyle("-fx-text-fill: -fx-text-primary;");
                } else {
                    lblAmountValue.setText("0.00 \u20ac");
                    lblAmountValue.setStyle("-fx-text-fill: #10b981;"); // Green
                }
            } else {
                double change = given - totalAmount;
                if (change >= 0) {
                    lblAmountValue.setText(String.format("%.2f \u20ac (Cambio)", change));
                    lblAmountValue.setStyle("-fx-text-fill: #10b981;"); // Green
                } else {
                    lblAmountValue.setText(String.format("%.2f \u20ac", totalAmount));
                    lblAmountValue.setStyle("-fx-text-fill: -fx-text-primary;");
                }
            }
            txtGivenAmount.setStyle(""); // Reset error style
        } catch (NumberFormatException e) {
            // Ignore during typing
        }
    }

    private void setupQuickButtons(double amount) {
        int ceil = (int) Math.ceil(amount);
        int btn1Val = ceil;
        int btn2Val = btn1Val + 1;
        int btn3Val = (int) (Math.ceil(amount / 5.0) * 5);
        if (btn3Val <= btn2Val)
            btn3Val += 5;
        int btn4Val = (int) (Math.ceil(amount / 10.0) * 10);
        if (btn4Val <= btn3Val)
            btn4Val += 10;

        btnQuick1.setText(btn1Val + " \u20ac");
        btnQuick2.setText(btn2Val + " \u20ac");
        btnQuick3.setText(btn3Val + " \u20ac");
        btnQuick4.setText(btn4Val + " \u20ac");
    }

    @FXML
    private void handleQuickMoney(javafx.event.ActionEvent event) {
        Button clicked = (Button) event.getSource();
        String text = clicked.getText().replace(" \u20ac", "").replace(",", ".");
        txtGivenAmount.setText(text);
    }

    @FXML
    private void handleConfirmPayment() {
        // Fiscal validation first (if Invoice mode is set in cart)
        if (container.getCartUseCase().isInvoiceMode() && container.getCartUseCase().getSelectedClient() == null) {
            com.mycompany.ventacontrolfx.presentation.util.AlertUtil.showError(container.getBundle().getString("payment.error.id_required.title"),
                    container.getBundle().getString("cart.doc_type.error_no_client"));
            return;
        }

        if (tgPaymentMethod.getSelectedToggle() == btnPayCard) {
            handleCardPayment();
        } else if (tgPaymentMethod.getSelectedToggle() == btnPayMixed) {
            handleMixedPayment();
        } else {
            handleCashPayment();
        }
    }

    private void handleCardPayment() {
        handleClose();
        if (callback != null) {
            String method = container.getBundle().getString("payment.method.card");
            Platform.runLater(() -> callback.onSuccess(totalAmount, 0.0, method, 0.0, totalAmount));
        }
    }

    private void handleCashPayment() {
        processCashPayment(false);
    }

    private void handleMixedPayment() {
        processCashPayment(true);
    }

    private void processCashPayment(boolean isMixed) {
        try {
            String text = txtGivenAmount.getText().trim().replace(",", ".");
            double given = text.isEmpty() ? totalAmount : Double.parseDouble(text);

            double roundedTotal = Math.round(totalAmount * 100.0) / 100.0;
            double roundedGiven = Math.round(given * 100.0) / 100.0;

            if (roundedGiven >= roundedTotal) {
                double change = roundedGiven - roundedTotal;
                handleClose();
                if (callback != null) {
                    String method = container.getBundle().getString("payment.method.cash");
                    Platform.runLater(() -> callback.onSuccess(given, change, method, totalAmount, 0.0));
                }
            } else if (isMixed && roundedGiven > 0) {
                double remaining = roundedTotal - roundedGiven;
                handleClose();
                if (callback != null) {
                    String method = String.format(container.getBundle().getString("payment.method.mixed_format"),
                            roundedGiven, remaining);
                    Platform.runLater(() -> callback.onSuccess(roundedTotal, 0.0, method, roundedGiven, remaining));
                }
            } else {
                txtGivenAmount.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2;");
            }
        } catch (NumberFormatException e) {
            txtGivenAmount.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2;");
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) lblTotalAmount.getScene().getWindow();
        stage.close();
    }
}
