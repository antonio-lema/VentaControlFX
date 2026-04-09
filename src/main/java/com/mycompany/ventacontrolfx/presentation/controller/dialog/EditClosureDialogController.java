package com.mycompany.ventacontrolfx.presentation.controller.dialog;

import com.mycompany.ventacontrolfx.application.usecase.CashClosureUseCase;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

public class EditClosureDialogController {

    @FXML
    private Label lblPreviousCash;
    @FXML
    private TextField txtAmount;
    @FXML
    private TextField txtReason;

    private CashClosureUseCase closureUseCase;
    private UserSession userSession;
    private int closureId;
    private double previousCash;
    private boolean confirmed = false;

    public void init(CashClosureUseCase useCase, UserSession session, int closureId, double previousCash) {
        this.closureUseCase = useCase;
        this.userSession = session;
        this.closureId = closureId;
        this.previousCash = previousCash;

        lblPreviousCash.setText(String.format("%.2f \u20ac", previousCash));
        txtAmount.setText(String.valueOf(previousCash));

        javafx.application.Platform.runLater(txtAmount::requestFocus);
    }

    @FXML
    private void handleConfirm() {
        String amountText = txtAmount.getText().replace(",", ".").trim();
        if (amountText.isEmpty()) {
            AlertUtil.showWarning("Campo Obligatorio", "Debes introducir el nuevo importe.");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            AlertUtil.showError("Formato Inv\u00e1lido", "El importe introducido no es un n\u00famero v\u00e1lido.");
            return;
        }

        String reason = txtReason.getText().trim();
        if (reason.isEmpty()) {
            AlertUtil.showWarning("Campo Obligatorio", "Debes especificar el motivo de la modificaci\u00f3n.");
            return;
        }

        try {
            int userId = userSession.getCurrentUser() != null ? userSession.getCurrentUser().getUserId() : 1;
            closureUseCase.updateClosure(closureId, amount, reason, userId, previousCash);
            confirmed = true;
            close();

        } catch (SQLException e) {
            AlertUtil.showError("Error", "No se pudo modificar el arqueo.");
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
