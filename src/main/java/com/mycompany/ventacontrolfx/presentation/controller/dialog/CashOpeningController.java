package com.mycompany.ventacontrolfx.presentation.controller.dialog;

import com.mycompany.ventacontrolfx.application.usecase.CashClosureUseCase;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

public class CashOpeningController {

    @FXML
    private Label lblLastClosure;
    @FXML
    private TextField txtInitialAmount;
    @FXML
    private TextArea txtNotes;

    private CashClosureUseCase closureUseCase;
    private UserSession userSession;
    private boolean confirmed = false;

    public void init(CashClosureUseCase useCase, UserSession session) {
        this.closureUseCase = useCase;
        this.userSession = session;

        try {
            double lastClosure = closureUseCase.getLastClosureAmount();
            if (userSession.hasPermission("caja.ver_totales") || userSession.hasPermission("USUARIOS")) {
                lblLastClosure.setText(String.format("%.2f €", lastClosure));
                txtInitialAmount.setText(String.format("%.2f", lastClosure).replace(".", ","));
            } else {
                lblLastClosure.setText("**** €");
                txtInitialAmount.setText(""); // Obligar a contar manualmente
            }
        } catch (SQLException e) {
            lblLastClosure.setText("No disponible");
        }

        // Foco inicial
        javafx.application.Platform.runLater(txtInitialAmount::requestFocus);
    }

    @FXML
    private void handleOpen() {
        try {
            // New check: Are there ANY pending transactions from before opening this fund?
            // If they didn't close yesterday, we should not allow starting a 'new' session
            // without clearing the old one.
            if (closureUseCase.getTodayTransactionCount() > 0) {
                AlertUtil.showWarning("Cierre Pendiente",
                        "Existen ventas o movimientos sin cerrar de una sesión anterior.\n" +
                                "Debe realizar el Arqueo y Cierre antes de abrir un nuevo turno.");
                // Optionally redirect or just inform
                return;
            }

            String amountText = txtInitialAmount.getText().replace(",", ".").trim();
            if (amountText.isEmpty()) {
                AlertUtil.showWarning("Campo Obligatorio", "Debe introducir un importe inicial.");
                return;
            }

            double amount = Double.parseDouble(amountText);
            if (amount < 0) {
                AlertUtil.showError("Importe Inválido", "El fondo inicial no puede ser negativo.");
                return;
            }

            String notes = txtNotes.getText();
            int userId = userSession.getCurrentUser() != null ? userSession.getCurrentUser().getUserId() : 1;
            closureUseCase.openCashFund(amount, notes, userId);

            confirmed = true;
            close();

        } catch (NumberFormatException e) {
            AlertUtil.showError("Error de Formato", "El importe introducido no es un número válido.");
        } catch (SQLException e) {
            AlertUtil.showError("Error al Abrir Caja", e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        ((Stage) txtInitialAmount.getScene().getWindow()).close();
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
