package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.Client;
import com.mycompany.ventacontrolfx.service.ClientService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import java.sql.SQLException;

public class ClientFormController implements com.mycompany.ventacontrolfx.util.Injectable {

    @FXML
    private Label lblTitle;
    @FXML
    private TextField txtName, txtTaxId, txtAddress, txtPostalCode, txtCity, txtProvince, txtCountry, txtEmail,
            txtPhone;

    @FXML
    private javafx.scene.layout.StackPane rootStackPane;

    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    private void handleMousePressed(javafx.scene.input.MouseEvent event) {
        xOffset = event.getSceneX();
        yOffset = event.getSceneY();
    }

    @FXML
    private void handleMouseDragged(javafx.scene.input.MouseEvent event) {
        Stage stage = (Stage) rootStackPane.getScene().getWindow();
        stage.setX(event.getScreenX() - xOffset);
        stage.setY(event.getScreenY() - yOffset);
    }

    private ClientService clientService;
    private Client currentClient;
    private boolean saveClicked = false;
    private com.mycompany.ventacontrolfx.service.ServiceContainer container;

    @Override
    public void inject(com.mycompany.ventacontrolfx.service.ServiceContainer container) {
        this.container = container;
        this.clientService = container.getClientService();
    }

    public void init(Client client) {
        this.currentClient = client;

        if (client != null) {
            lblTitle.setText("Editar Empresa");
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
            lblTitle.setText("Nueva Empresa");
        }
    }

    public boolean isSaveClicked() {
        return saveClicked;
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    @FXML
    private void handleSave() {
        if (txtName.getText().isEmpty()) {
            showAlert("Validación", "El nombre es obligatorio", Alert.AlertType.WARNING);
            return;
        }

        if (currentClient == null) {
            currentClient = new Client();
            currentClient.setIsCompany(true);
        }

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
            clientService.saveClient(currentClient);
            saveClicked = true;
            closeWindow();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "No se pudo guardar la empresa:\n" + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void closeWindow() {
        if (txtName.getScene() != null && txtName.getScene().getWindow() != null) {
            ((Stage) txtName.getScene().getWindow()).close();
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        switch (type) {
            case ERROR:
                AlertUtil.showError(title, content);
                break;
            case WARNING:
                AlertUtil.showWarning(title, content);
                break;
            case CONFIRMATION:
                AlertUtil.showConfirmation(title, content, "");
                break;
            default:
                AlertUtil.showInfo(title, content);
                break;
        }
    }
}
