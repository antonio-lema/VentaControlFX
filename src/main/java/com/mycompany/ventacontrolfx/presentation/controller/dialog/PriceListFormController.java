package com.mycompany.ventacontrolfx.presentation.controller.dialog;

import com.mycompany.ventacontrolfx.application.usecase.PriceListUseCase;
import com.mycompany.ventacontrolfx.domain.model.PriceList;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

public class PriceListFormController implements Injectable {

    @FXML
    private Label lblTitle;
    @FXML
    private TextField txtName;
    @FXML
    private TextArea txtDescription;
    @FXML
    private CheckBox chkIsActive;
    @FXML
    private CheckBox chkIsDefault;

    private ServiceContainer container;
    private PriceListUseCase priceListUseCase;
    private PriceList currentPriceList;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.priceListUseCase = container.getPriceListUseCase();
    }

    public void initData(PriceList priceList) {
        this.currentPriceList = priceList;
        if (priceList != null) {
            lblTitle.setText("Editar Tarifa");
            txtName.setText(priceList.getName());
            txtDescription.setText(priceList.getDescription());
            chkIsActive.setSelected(priceList.isActive());
            chkIsDefault.setSelected(priceList.isDefault());

            // Si es la tarifa por defecto, no dejamos desmarcarla desde aqu\u00ed (para evitar
            // quedarnos sin ninguna)
            if (priceList.isDefault() || priceList.getId() == 1) {
                chkIsDefault.setDisable(true);
                chkIsActive.setDisable(true); // No se puede desactivar la principal/default
            }
        } else {
            lblTitle.setText("Nueva Tarifa");
            chkIsActive.setSelected(true);
        }
    }

    @FXML
    private void handleSave() {
        if (txtName.getText() == null || txtName.getText().trim().isEmpty()) {
            AlertUtil.showWarning("Campos vac\u00edos", "El nombre de la tarifa no puede estar vac\u00edo.");
            return;
        }

        try {
            if (currentPriceList == null) {
                PriceList newPL = new PriceList(0,
                        txtName.getText().trim(),
                        txtDescription.getText() != null ? txtDescription.getText().trim() : "",
                        chkIsDefault.isSelected(),
                        chkIsActive.isSelected(),
                        currentPriceList != null ? currentPriceList.getPriority() : 0);
                priceListUseCase.save(newPL);
                AlertUtil.showInfo("\u00c3\u2030xito", "Tarifa creada correctamente.");
            } else {
                currentPriceList.setName(txtName.getText().trim());
                currentPriceList
                        .setDescription(txtDescription.getText() != null ? txtDescription.getText().trim() : "");
                currentPriceList.setDefault(chkIsDefault.isSelected());
                currentPriceList.setActive(chkIsActive.isSelected());
                priceListUseCase.update(currentPriceList);
                AlertUtil.showInfo("\u00c3\u2030xito", "Tarifa actualizada correctamente.");
            }
            closeModal();
        } catch (SQLException ex) {
            AlertUtil.showError("Error", "Ocurri\u00f3 un error al guardar la tarifa: " + ex.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        closeModal();
    }

    private void closeModal() {
        ((Stage) txtName.getScene().getWindow()).close();
    }
}
