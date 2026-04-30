package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.function.BiConsumer;

public class CorrectionDialogController implements Injectable {

    @FXML
    private TextField txtName;
    @FXML
    private TextField txtNif;

    private ServiceContainer container;
    private Sale currentSale;
    private BiConsumer<String, String> onSuccess;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
    }

    public void init(Sale sale) {
        this.currentSale = sale;
        this.txtName.setText(sale.getCustomerNameSnapshot() != null ? sale.getCustomerNameSnapshot() : "");
        this.txtNif.setText(sale.getCustomerNifSnapshot() != null ? sale.getCustomerNifSnapshot() : "");
    }

    public void setOnSuccess(BiConsumer<String, String> onSuccess) {
        this.onSuccess = onSuccess;
    }

    @FXML
    private void handleSave() {
        String name = txtName.getText().trim();
        String nif = txtNif.getText().trim();

        if (name.isEmpty() || nif.isEmpty()) {
            AlertUtil.showWarning("Datos incompletos", "Debe indicar el nombre y el NIF para la subsanación.");
            return;
        }

        if (onSuccess != null) {
            onSuccess.accept(name, nif);
        }
        close();
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        Stage stage = (Stage) txtName.getScene().getWindow();
        stage.close();
    }
}
