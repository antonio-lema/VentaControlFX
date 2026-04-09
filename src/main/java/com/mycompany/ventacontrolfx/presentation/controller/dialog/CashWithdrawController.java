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
    private javafx.scene.layout.HBox bannerAvailable;
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

        boolean canSeeTotals = userSession.hasPermission("caja.ver_totales") || userSession.hasPermission("USUARIOS");
        if (bannerAvailable != null) {
            bannerAvailable.setVisible(canSeeTotals);
            bannerAvailable.setManaged(canSeeTotals);
        }

        try {
            this.availableAmount = closureUseCase.getCurrentCashInDrawer();
            lblAvailable.setText(String.format("%.2f \u20AC", availableAmount));
        } catch (SQLException e) {
            lblAvailable.setText("Error al cargar");
        }

        // Listener para validaci\u00c3\u00b3n visual de saldo en tiempo real
        txtAmount.textProperty().addListener((obs, old, newVal) -> validateAmountRealtime(newVal));

        javafx.application.Platform.runLater(txtAmount::requestFocus);
    }

    private void validateAmountRealtime(String text) {
        try {
            double amount = Double.parseDouble(text.replace(",", ".").trim());
            if (amount > availableAmount) {
                boolean canSeeTotals = userSession.hasPermission("caja.ver_totales")
                        || userSession.hasPermission("USUARIOS");
                if (canSeeTotals) {
                    lblWarning.setText(String.format(
                            "\u00e2\u0161\u00a0\u00ef\u00b8\u008f Saldo insuficiente. Disponible: %.2f \u20AC", availableAmount));
                } else {
                    lblWarning.setText("\u00e2\u0161\u00a0\u00ef\u00b8\u008f Saldo insuficiente.");
                }
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
        // \u00e2\u20ac\u201d Validar importe
        String amountText = txtAmount.getText().replace(",", ".").trim();
        if (amountText.isEmpty()) {
            AlertUtil.showWarning("Campo Obligatorio", "Debes introducir el importe a retirar.");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            AlertUtil.showError("Formato Inv\u00c3\u00a1lido", "El importe introducido no es un n\u00c3\u00bamero v\u00c3\u00a1lido.");
            return;
        }
        if (amount <= 0) {
            AlertUtil.showError("Importe Inv\u00c3\u00a1lido", "El importe debe ser mayor que cero.");
            return;
        }

        // \u00e2\u20ac\u201d Validar motivo
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
            AlertUtil.showError("\u00e2\u009d\u0152 Sin Efectivo Suficiente", e.getMessage());
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
