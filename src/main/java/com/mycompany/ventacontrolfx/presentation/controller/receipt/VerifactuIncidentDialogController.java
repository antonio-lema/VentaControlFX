package com.mycompany.ventacontrolfx.presentation.controller.receipt;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import java.util.function.Consumer;

public class VerifactuIncidentDialogController {

    @FXML private ComboBox<String> cmbReason;
    @FXML private TextArea txtObservations;

    private Consumer<String> onSaveCallback;

    @FXML
    public void initialize() {
        cmbReason.getItems().addAll(
            "Fallo de conexión a Internet (ISP)",
            "Forte de suministro eléctrico",
            "Fallo de hardware / Servidor local",
            "Mantenimiento técnico programado",
            "Cierre por fuerza mayor",
            "Otro (especificar en observaciones)"
        );
    }

    public void setOnSave(Consumer<String> callback) {
        this.onSaveCallback = callback;
    }

    @FXML
    private void handleSave() {
        String reason = cmbReason.getValue();
        if (reason == null || reason.isEmpty()) {
            reason = "Incidencia técnica no especificada";
        }
        
        String observations = txtObservations.getText();
        String fullReason = reason + (observations != null && !observations.isEmpty() ? " - " + observations : "");
        
        if (onSaveCallback != null) {
            onSaveCallback.accept(fullReason);
        }
        close();
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        ((Stage) cmbReason.getScene().getWindow()).close();
    }
}

