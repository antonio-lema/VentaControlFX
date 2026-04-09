package com.mycompany.ventacontrolfx.presentation.controller.dialog;

import com.mycompany.ventacontrolfx.application.usecase.TaxManagementUseCase;
import com.mycompany.ventacontrolfx.domain.model.TaxRate;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;

public class TaxRateDialogController implements Injectable {

    @FXML private TextField txtName;
    @FXML private TextField txtRate;
    @FXML private TextField txtCountry;
    @FXML private DatePicker dpStartDate;
    @FXML private CheckBox chkActive;

    private TaxManagementUseCase taxManagementUseCase;
    private TaxRate currentRate;
    private boolean saveClicked = false;

    @Override
    public void inject(ServiceContainer container) {
        this.taxManagementUseCase = container.getTaxManagementUseCase();
    }

    public void init(TaxRate rate) {
        this.currentRate = rate;

        if (rate != null) {
            txtName.setText(rate.getName() != null ? rate.getName() : "");
            txtRate.setText(String.valueOf(rate.getRate()));
            txtCountry.setText(rate.getCountry() != null ? rate.getCountry() : "ES");
            chkActive.setSelected(rate.isActive());

            if (rate.getValidFrom() != null) {
                dpStartDate.setValue(rate.getValidFrom().toLocalDate());
            } else {
                dpStartDate.setValue(LocalDate.now());
            }
        } else {
            dpStartDate.setValue(LocalDate.now());
            chkActive.setSelected(true);
        }
    }

    @FXML
    private void handleSave() {
        if (txtName.getText().trim().isEmpty()) {
            AlertUtil.showWarning("Validaci\u00f3n", "El nombre es obligatorio.");
            return;
        }

        if (txtRate.getText().trim().isEmpty()) {
            AlertUtil.showWarning("Validaci\u00f3n", "El porcentaje es obligatorio.");
            return;
        }

        if (currentRate == null) {
            currentRate = new TaxRate();
        }

        try {
            currentRate.setName(txtName.getText().trim());
            currentRate.setRate(Double.parseDouble(txtRate.getText().trim().replace(",", ".")));
            currentRate.setCountry(txtCountry.getText().trim());
            currentRate.setActive(chkActive.isSelected());

            if (dpStartDate.getValue() != null) {
                currentRate.setValidFrom(dpStartDate.getValue().atStartOfDay());
            }

            if (currentRate.getTaxRateId() == 0) {
                taxManagementUseCase.saveTaxRate(currentRate);
            } else {
                taxManagementUseCase.updateTaxRate(currentRate);
            }

            saveClicked = true;
            close();
        } catch (NumberFormatException e) {
            AlertUtil.showWarning("Validaci\u00f3n", "Por favor, introduce un porcentaje v\u00e1lido.");
        } catch (SQLException e) {
            AlertUtil.showError("Error", "No se pudo guardar la tasa: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        ((Stage) txtName.getScene().getWindow()).close();
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }
}
