package com.mycompany.ventacontrolfx.presentation.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.application.Platform;

import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;

public class PaymentController implements Injectable {

    private ServiceContainer container;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
    }

    @FXML
    private Label lblTotalAmount, lblCashAmount, lblCashWarning;
    @FXML
    private TextField txtGivenAmount;
    @FXML
    private Button btnQuick1, btnQuick2, btnQuick3, btnQuick4;
    @FXML
    private javafx.scene.layout.VBox cashSection, vboxCashWarning;

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

        String formatted = String.format("%.2f â‚¬", amount);
        lblTotalAmount.setText(formatted);
        lblCashAmount.setText(formatted);
        txtGivenAmount.setText(String.format("%.2f", amount).replace(',', '.'));

        if (amount > 1000) {
            cashSection.setDisable(true);
            vboxCashWarning.setVisible(true);
            vboxCashWarning.setManaged(true);
        } else {
            cashSection.setDisable(false);
            vboxCashWarning.setVisible(false);
            vboxCashWarning.setManaged(false);
        }

        setupQuickButtons(amount);
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

        btnQuick1.setText(btn1Val + " â‚¬");
        btnQuick2.setText(btn2Val + " â‚¬");
        btnQuick3.setText(btn3Val + " â‚¬");
        btnQuick4.setText(btn4Val + " â‚¬");
    }

    @FXML
    private void handleQuickMoney(javafx.event.ActionEvent event) {
        Button clicked = (Button) event.getSource();
        String text = clicked.getText().replace(" â‚¬", "").replace(",", ".");
        txtGivenAmount.setText(text);
    }

    @FXML
    private void handleCardPayment() {
        handleClose();
        if (callback != null) {
            String method = container.getBundle().getString("payment.method.card");
            Platform.runLater(() -> callback.onSuccess(totalAmount, 0.0, method, 0.0, totalAmount));
        }
    }

    @FXML
    private void handleCashPayment() {
        processCashPayment(false);
    }

    @FXML
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
                // Pago Mixto: Registrar parte en efectivo y el resto por tarjeta
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
