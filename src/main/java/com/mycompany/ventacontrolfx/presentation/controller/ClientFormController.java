package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.Client;
import com.mycompany.ventacontrolfx.application.usecase.ClientUseCase;
import com.mycompany.ventacontrolfx.application.usecase.PriceListUseCase;
import com.mycompany.ventacontrolfx.domain.model.PriceList;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

public class ClientFormController implements Injectable {

    @FXML
    private Label lblTitle;
    @FXML
    private javafx.scene.control.ComboBox<String> cmbType;
    @FXML
    private javafx.scene.control.ComboBox<PriceList> cmbPriceList;
    @FXML
    private TextField txtName, txtTaxId, txtAddress, txtPostalCode, txtCity, txtProvince, txtCountry, txtEmail,
            txtPhone;

    private ClientUseCase clientUseCase;
    private PriceListUseCase priceListUseCase;
    private Client currentClient;
    private boolean saveClicked = false;

    @Override
    public void inject(ServiceContainer container) {
        this.clientUseCase = container.getClientUseCase();
        this.priceListUseCase = container.getPriceListUseCase();
    }

    public void init(Client client) {
        this.currentClient = client;

        cmbType.setItems(javafx.collections.FXCollections.observableArrayList("Particular", "Empresa"));
        cmbType.valueProperty().addListener((obs, oldVal, newVal) -> {
            if ("Empresa".equals(newVal)) {
                lblTitle.setText(currentClient == null ? "Nueva Empresa" : "Editar Empresa");
                txtName.setPromptText("Nombre de la empresa");
                txtTaxId.setPromptText("CIF");
            } else {
                lblTitle.setText(currentClient == null ? "Nuevo Cliente" : "Editar Cliente");
                txtName.setPromptText("Nombre completo");
                txtTaxId.setPromptText("DNI / NIE");
            }
        });

        if (client != null) {
            cmbType.setValue(client.isIsCompany() ? "Empresa" : "Particular");
            txtName.setText(client.getName());
            txtTaxId.setText(client.getTaxId());
            txtAddress.setText(client.getAddress());
            txtPostalCode.setText(client.getPostalCode());
            txtCity.setText(client.getCity());
            txtProvince.setText(client.getProvince());
            txtCountry.setText(client.getCountry());
            txtEmail.setText(client.getEmail());
            txtPhone.setText(client.getPhone());
        } else {
            cmbType.setValue("Particular");
        }

        setupPriceLists();
    }

    private void setupPriceLists() {
        try {
            cmbPriceList.setItems(javafx.collections.FXCollections.observableArrayList(priceListUseCase.getAll()));
            cmbPriceList.setConverter(new javafx.util.StringConverter<PriceList>() {
                @Override
                public String toString(PriceList object) {
                    return object == null ? "Tarifa del sistema" : object.getName();
                }

                @Override
                public PriceList fromString(String string) {
                    return null;
                }
            });

            if (currentClient != null && currentClient.getPriceListId() > 0) {
                for (PriceList pl : cmbPriceList.getItems()) {
                    if (pl.getId() == currentClient.getPriceListId()) {
                        cmbPriceList.setValue(pl);
                        break;
                    }
                }
            } else {
                // If no specific price list, we could set a "Default" placeholder or leave
                // empty
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    @FXML
    private void handleSave() {
        if (txtName.getText().trim().isEmpty()) {
            AlertUtil.showWarning("Validación", "El nombre es obligatorio.");
            return;
        }

        if ("Empresa".equals(cmbType.getValue()) && txtTaxId.getText().trim().isEmpty()) {
            AlertUtil.showWarning("Validación", "El CIF es obligatorio para configurar una Empresa.");
            return;
        }

        String email = txtEmail.getText().trim();
        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            AlertUtil.showWarning("Validación", "El formato del correo electrónico es inválido.");
            return;
        }

        if (currentClient == null)
            currentClient = new Client();

        currentClient.setIsCompany("Empresa".equals(cmbType.getValue()));
        currentClient.setName(txtName.getText());
        currentClient.setTaxId(txtTaxId.getText());
        currentClient.setAddress(txtAddress.getText());
        currentClient.setPostalCode(txtPostalCode.getText());
        currentClient.setCity(txtCity.getText());
        currentClient.setProvince(txtProvince.getText());
        currentClient.setCountry(txtCountry.getText());
        currentClient.setEmail(txtEmail.getText());
        currentClient.setPhone(txtPhone.getText());

        PriceList selectedPriceList = cmbPriceList.getValue();
        if (selectedPriceList != null) {
            currentClient.setPriceListId(selectedPriceList.getId());
        } else {
            currentClient.setPriceListId(-1);
        }

        try {
            if (currentClient.getId() == 0)
                clientUseCase.addClient(currentClient);
            else
                clientUseCase.updateClient(currentClient);
            saveClicked = true;
            close();
        } catch (SQLException e) {
            AlertUtil.showError("Error", "Error al guardar: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        ((Stage) txtName.getScene().getWindow()).close();
    }
}
