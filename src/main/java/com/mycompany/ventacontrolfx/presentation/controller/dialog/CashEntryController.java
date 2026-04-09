package com.mycompany.ventacontrolfx.presentation.controller.dialog;

import com.mycompany.ventacontrolfx.application.usecase.CashClosureUseCase;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.UserSession;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

public class CashEntryController {

    @FXML
    private Label lblCurrentCash;
    @FXML
    private javafx.scene.layout.HBox bannerCurrentCash;
    @FXML
    private TextField txtAmount;
    @FXML
    private ComboBox<String> comboType;
    @FXML
    private TextField txtReason;

    private CashClosureUseCase closureUseCase;
    private UserSession userSession;
    private boolean confirmed = false;

    public void init(CashClosureUseCase useCase, UserSession session) {
        this.closureUseCase = useCase;
        this.userSession = session;

        boolean canSeeTotals = userSession.hasPermission("caja.ver_totales") || userSession.hasPermission("USUARIOS");
        if (bannerCurrentCash != null) {
            bannerCurrentCash.setVisible(canSeeTotals);
            bannerCurrentCash.setManaged(canSeeTotals);
        }

        comboType.setItems(FXCollections.observableArrayList(
                "Cambio banco", "Fondo de reserva", "Ajuste manual",
                "Ingreso extraordinario", "Donaci\u00f3n", "Otro"));
        comboType.setValue("Cambio banco");

        try {
            double current = closureUseCase.getCurrentCashInDrawer();
            lblCurrentCash.setText(String.format("%.2f \u20ac", current));
        } catch (SQLException e) {
            lblCurrentCash.setText("No disponible");
        }

        javafx.application.Platform.runLater(txtAmount::requestFocus);
    }

    @FXML
    private void handleConfirm() {
        // \u00e2\u20ac\u201d Validar importe
        String amountText = txtAmount.getText().replace(",", ".").trim();
        if (amountText.isEmpty()) {
            AlertUtil.showWarning("Campo Obligatorio", "Debes introducir el importe a ingresar.");
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            AlertUtil.showError("Formato Inv\u00e1lido", "El importe introducido no es un n\u00famero v\u00e1lido.");
            return;
        }
        if (amount <= 0) {
            AlertUtil.showError("Importe Inv\u00e1lido", "El importe debe ser mayor que cero.");
            return;
        }

        // \u00e2\u20ac\u201d Validar motivo
        String reason = txtReason.getText().trim();
        if (reason.isEmpty()) {
            AlertUtil.showWarning("Campo Obligatorio", "Es obligatorio introducir una justificaci\u00f3n para el ingreso.");
            return;
        }

        try {
            int userId = userSession.getCurrentUser() != null ? userSession.getCurrentUser().getUserId() : 1;
            String type = comboType.getValue() != null ? comboType.getValue() : "Manual";
            String fullReason = "[" + type + "] " + reason;

            closureUseCase.registerCashEntry(amount, fullReason, userId);
            confirmed = true;
            close();

        } catch (SQLException e) {
            AlertUtil.showError("Error al Registrar", "No se pudo registrar el ingreso: " + e.getMessage());
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
