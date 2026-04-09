package com.mycompany.ventacontrolfx.presentation.controller.dialog;

import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class PriceListCloneController implements Injectable {

    @FXML
    private TextField txtName;
    @FXML
    private TextField txtPercentage;

    private boolean confirmed = false;

    @Override
    public void inject(ServiceContainer container) {
        // No requiere servicios pesados, solo campos
    }

    public void initData(String defaultName) {
        if (txtName != null) {
            txtName.setText(defaultName);
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getName() {
        return txtName != null ? txtName.getText().trim() : "";
    }

    public double getPercentage() {
        if (txtPercentage == null || txtPercentage.getText().trim().isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(txtPercentage.getText().trim().replace(",", "."));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @FXML
    private void handleConfirm() {
        if (getName().isEmpty()) {
            AlertUtil.showWarning("Campo vacÃ­o", "El nombre de la tarifa no puede estar vacÃ­o.");
            return;
        }
        try {
            getPercentage(); // Solo para verificar que no cause crash, aunque devuelve 0 si falla
        } catch (Exception e) {
            AlertUtil.showWarning("Formato invÃ¡lido", "El porcentaje de ajuste debe ser un nÃºmero.");
            return;
        }
        confirmed = true;
        closeModal();
    }

    @FXML
    private void handleCancel() {
        confirmed = false;
        closeModal();
    }

    private void closeModal() {
        if (txtName != null && txtName.getScene() != null) {
            ((Stage) txtName.getScene().getWindow()).close();
        }
    }
}
