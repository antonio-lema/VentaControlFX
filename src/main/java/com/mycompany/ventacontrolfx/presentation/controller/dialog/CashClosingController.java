package com.mycompany.ventacontrolfx.presentation.controller.dialog;

import com.mycompany.ventacontrolfx.application.usecase.CashClosureUseCase;
import com.mycompany.ventacontrolfx.domain.model.CashClosure;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.UserSession;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;

public class CashClosingController {

    @FXML
    private Label lblInitFund, lblSales, lblCashIn, lblReturns, lblCashOut, lblExpected, lblDifference, lblDiffReason;
    @FXML
    private TextField txtActualAmount;
    @FXML
    private TextArea txtNotes;

    private CashClosureUseCase closureUseCase;
    private UserSession userSession;
    private double expectedAmount = 0;
    private boolean confirmed = false;

    public void init(CashClosureUseCase useCase, UserSession session) {
        this.closureUseCase = useCase;
        this.userSession = session;

        try {
            this.expectedAmount = closureUseCase.getCurrentCashInDrawer();
            double initialFund = closureUseCase.getActiveFundAmount();
            java.util.Map<String, Double> totals = closureUseCase.getTodayTotals();

            boolean canSeeTotals = userSession.hasPermission("caja.ver_totales")
                    || userSession.hasPermission("USUARIOS");

            if (canSeeTotals) {
                lblInitFund.setText(String.format("%.2f €", initialFund));
                // Ventas brutas (sin restar devoluciones aún en la etiqueta de ventas)
                double returnsCash = totals.getOrDefault("returns_cash", 0.0);
                lblSales.setText(String.format("%.2f €", totals.getOrDefault("cash", 0.0) + returnsCash));
                lblCashIn.setText(String.format("%.2f €", totals.getOrDefault("manual_in", 0.0)));
                lblReturns.setText(String.format("%.2f €", returnsCash));
                lblCashOut.setText(String.format("%.2f €", totals.getOrDefault("manual_out", 0.0)));
                lblExpected.setText(String.format("%.2f €", expectedAmount));
            } else {
                lblInitFund.setText("**** €");
                lblSales.setText("**** €");
                lblCashIn.setText("**** €");
                lblReturns.setText("**** €");
                lblCashOut.setText("**** €");
                lblExpected.setText("**** €");
            }
            updateDifference();
        } catch (SQLException e) {
            lblExpected.setText("Error");
        }

        txtActualAmount.textProperty().addListener((obs, old, newVal) -> updateDifference());

        // Foco inicial
        javafx.application.Platform.runLater(txtActualAmount::requestFocus);
    }

    private void updateDifference() {
        if (!userSession.hasPermission("caja.ver_totales") && !userSession.hasPermission("USUARIOS")) {
            lblDifference.setText("**** €");
            lblDifference.setStyle("-fx-font-size: 24px; -fx-font-weight: 900; -fx-text-fill: #94a3b8;");
            lblDiffReason.setText("El descuadre se calculará al confirmar");
            lblDiffReason.setStyle("-fx-text-fill: #64748b;");
            return;
        }

        try {
            String text = txtActualAmount.getText().replace(",", ".").trim();
            double actual = text.isEmpty() ? 0 : Double.parseDouble(text);
            double diff = actual - expectedAmount;

            if (Math.abs(diff) < 0.001) {
                lblDifference.setText(String.format("%.2f €", diff)); // Sin signo si es cero real
                lblDifference.setStyle("-fx-font-size: 24px; -fx-font-weight: 900; -fx-text-fill: #10b981;");
                lblDiffReason.setText("✅ Caja cuadrada perfectamente");
                lblDiffReason.setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
            } else {
                lblDifference.setText(String.format("%+.2f €", diff)); // Forzar signo + o -
                lblDifference.setStyle("-fx-font-size: 24px; -fx-font-weight: 900; -fx-text-fill: #ef4444;");
                lblDiffReason.setText(diff > 0 ? "⚠️ Sobante de caja detectado" : "❌ Faltante de caja detectado");
                lblDiffReason.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
            }
        } catch (NumberFormatException e) {
            lblDifference.setText("-- €");
            lblDiffReason.setText("Importe no válido");
        }
    }

    @FXML
    private void handleClose() {
        try {
            String actualText = txtActualAmount.getText().replace(",", ".").trim();
            if (actualText.isEmpty()) {
                AlertUtil.showWarning("Campo Obligatorio", "Debe introducir el efectivo real contado.");
                return;
            }

            double actual = Double.parseDouble(actualText);
            double diff = actual - expectedAmount;
            String notes = txtNotes.getText().trim();

            if (Math.abs(diff) > 0.001 && notes.isEmpty()) {
                AlertUtil.showWarning("Justificación Necesaria",
                        "Es obligatorio introducir una nota explicando el descuadre.");
                txtNotes.requestFocus();
                txtNotes.setStyle("-fx-border-color: #ef4444; -fx-border-width: 2;");
                return;
            }

            CashClosure closure = new CashClosure();
            closure.setClosureDate(LocalDate.now());
            closure.setUserId(userSession.getCurrentUser() != null ? userSession.getCurrentUser().getUserId() : 1);
            closure.setTotalCash(expectedAmount);
            // Nota: totalCard y totalAll se enriquecen en el UseCase, pero mapeamos lo
            // básico aquí si fuera necesario
            // Sin embargo, el UseCase performClosure ya recalcula los totales de ventas.
            closure.setActualCash(actual);
            closure.setNotes(notes);

            closureUseCase.performClosure(closure);

            confirmed = true;
            close();

        } catch (NumberFormatException e) {
            AlertUtil.showError("Error de Formato", "El importe real no es válido.");
        } catch (SQLException e) {
            AlertUtil.showError("Error al Cerrar Caja", e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        ((Stage) txtActualAmount.getScene().getWindow()).close();
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
