package com.mycompany.ventacontrolfx.presentation.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.application.Platform;

public class PaymentController {

    @FXML
    private Label lblTotalAmount, lblCashAmount;
    @FXML
    private TextField txtGivenAmount;
    @FXML
    private Button btnQuick1, btnQuick2, btnQuick3, btnQuick4;

    public interface PaymentCallback {
        void onSuccess(double paid, double change, String method);
    }

    private double totalAmount;
    private PaymentCallback callback;

    public void initialize() {
        Platform.runLater(() -> txtGivenAmount.requestFocus());
    }

    public void setTotalAmount(double amount, PaymentCallback callback) {
        this.totalAmount = amount;
        this.callback = callback;

        String formatted = String.format("%.2f €", amount);
        lblTotalAmount.setText(formatted);
        lblCashAmount.setText(formatted);
        txtGivenAmount.setText(String.format("%.2f", amount).replace(',', '.'));

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

        btnQuick1.setText(btn1Val + " €");
        btnQuick2.setText(btn2Val + " €");
        btnQuick3.setText(btn3Val + " €");
        btnQuick4.setText(btn4Val + " €");
    }

    @FXML
    private void handleQuickMoney(javafx.event.ActionEvent event) {
        Button clicked = (Button) event.getSource();
        String text = clicked.getText().replace(" €", "").replace(",", ".");
        txtGivenAmount.setText(text);
    }

    @FXML
    private void handleCardPayment() {
        handleClose();
        if (callback != null)
            callback.onSuccess(totalAmount, 0.0, "Tarjeta");
    }

    @FXML
    private void handleCashPayment() {
        try {
            String text = txtGivenAmount.getText().trim().replace(",", ".");
            double given = text.isEmpty() ? totalAmount : Double.parseDouble(text);

            if (given >= totalAmount) {
                double change = given - totalAmount;
                handleClose();
                if (callback != null)
                    callback.onSuccess(given, change, "Efectivo");
            } else {
                txtGivenAmount.setStyle("-fx-border-color: -color-danger; -fx-border-width: 2;");
            }
        } catch (NumberFormatException e) {
            txtGivenAmount.setStyle("-fx-border-color: -color-danger; -fx-border-width: 2;");
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) lblTotalAmount.getScene().getWindow();
        stage.close();
    }
}
