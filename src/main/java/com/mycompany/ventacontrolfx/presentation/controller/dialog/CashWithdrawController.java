package com.mycompany.ventacontrolfx.presentation.controller.dialog;

import com.mycompany.ventacontrolfx.application.usecase.CashClosureUseCase;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

public class CashWithdrawController {

    @FXML
    private Label lblAvailable;
    @FXML
    private TextField txtAmount;
    @FXML
    private Label lblWarning;
    @FXML
    private TextField txtReason;
    @FXML
    private Button btnConfirm;

    private CashClosureUseCase closureUseCase;
    private UserSession userSession;
    private double availableAmount = 0;
    private boolean confirmed = false;

    public void init(CashClosureUseCase useCase, UserSession session) {
        this.closureUseCase = useCase;
        this.userSession = session;

        try {
            this.availableAmount = closureUseCase.getCurrentCashInDrawer();
            lblAvailable.setText(String.format("%.2f €", availableAmount));
        } catch (SQLException e) {
            lblAvailable.setText("Error al cargar");
        }

        // Listener para validación visual de saldo en tiempo real
        txtAmount.textProperty().addListener((obs, old, newVal) -> validateAmountRealtime(newVal));

        javafx.application.Platform.runLater(txtAmount::requestFocus);
    }

    private void validateAmountRealtime(String text) {
        try {
            double amount = Double.parseDouble(text.replace(",", ".").trim());
            if (amount > availableAmount) {
                lblWarning.setText(String.format(
                        "⚠️ Saldo insuficiente. Disponible: %.2f €", availableAmount));
                lblWarning.setVisible(true);
                lblWarning.setManaged(true);
                btnConfirm.setDisable(true);
            } else {
                lblWarning.setVisible(false);
                lblWarning.setManaged(false);
                btnConfirm.setDisable(false);
            }
        } catch (NumberFormatException e) {
            lblWarning.setVisible(false);
            lblWarning.setManaged(false);
            btnConfirm.setDisable(false);
        }
    }

    @FXML
    private void handleConfirm() {
        // — Validar importe
        String amountText = txtAmount.getText().replace(",", ".").trim();
        if (amountText.isEmpty()) {
            AlertUtil.showWarning("Campo Obligatorio", "Debes introducir el importe a retirar.");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            AlertUtil.showError("Formato Inválido", "El importe introducido no es un número válido.");
            return;
        }
        if (amount <= 0) {
            AlertUtil.showError("Importe Inválido", "El importe debe ser mayor que cero.");
            return;
        }

        // — Validar motivo
        String reason = txtReason.getText().trim();
        if (reason.isEmpty()) {
            AlertUtil.showWarning("Campo Obligatorio", "Debes especificar el motivo de la retirada.");
            return;
        }

        try {
            int userId = userSession.getCurrentUser() != null ? userSession.getCurrentUser().getUserId() : 1;
            closureUseCase.withdrawCash(amount, reason, userId);
            confirmed = true;
            close();

        } catch (SQLException e) {
            AlertUtil.showError("❌ Sin Efectivo Suficiente", e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        ((Stage) txtAmount.getScene().getWindow()).close();
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
