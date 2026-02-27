package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.Client;
import com.mycompany.ventacontrolfx.application.usecase.ClientUseCase;
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
    private TextField txtName, txtTaxId, txtAddress, txtPostalCode, txtCity, txtProvince, txtCountry, txtEmail,
            txtPhone;

    private ClientUseCase clientUseCase;
    private Client currentClient;
    private boolean saveClicked = false;

    @Override
    public void inject(ServiceContainer container) {
        this.clientUseCase = container.getClientUseCase();
    }

    public void init(Client client) {
        this.currentClient = client;
        if (client != null) {
            lblTitle.setText("Editar Cliente");
            txtName.setText(client.getName());
            txtTaxId.setText(client.getTaxId());
            txtAddress.setText(client.getAddress());
            txtPostalCode.setText(client.getPostalCode());
            txtCity.setText(client.getCity());
            txtProvince.setText(client.getProvince());
            txtCountry.setText(client.getCountry());
            txtEmail.setText(client.getEmail());
            txtPhone.setText(client.getPhone());
        }
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    @FXML
    private void handleSave() {
        if (txtName.getText().isEmpty()) {
            AlertUtil.showWarning("Validación", "El nombre es obligatorio.");
            return;
        }

        if (currentClient == null)
            currentClient = new Client();
        currentClient.setName(txtName.getText());
        currentClient.setTaxId(txtTaxId.getText());
        currentClient.setAddress(txtAddress.getText());
        currentClient.setPostalCode(txtPostalCode.getText());
        currentClient.setCity(txtCity.getText());
        currentClient.setProvince(txtProvince.getText());
        currentClient.setCountry(txtCountry.getText());
        currentClient.setEmail(txtEmail.getText());
        currentClient.setPhone(txtPhone.getText());

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
