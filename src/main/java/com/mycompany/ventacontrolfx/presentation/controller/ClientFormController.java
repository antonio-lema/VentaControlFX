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
    private javafx.scene.control.ComboBox<String> cmbIdType;
    @FXML
    private javafx.scene.control.ComboBox<PriceList> cmbPriceList;
    @FXML
    private TextField txtName, txtTaxId, txtAddress, txtPostalCode, txtCity, txtProvince, txtCountry, txtEmail,
            txtPhone;

    private ServiceContainer container;
    private ClientUseCase clientUseCase;
    private PriceListUseCase priceListUseCase;
    private Client currentClient;
    private boolean saveClicked = false;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.clientUseCase = container.getClientUseCase();
        this.priceListUseCase = container.getPriceListUseCase();
    }

    public void init(Client client) {
        this.currentClient = client;

        cmbType.setItems(javafx.collections.FXCollections.observableArrayList(
                container.getBundle().getString("client.type.person"),
                container.getBundle().getString("client.type.company")));
        cmbIdType.setItems(javafx.collections.FXCollections.observableArrayList("DNI", "NIE", "CIF", "Pasaporte"));

        cmbType.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isCompany = container.getBundle().getString("client.type.company").equals(newVal);
            if (isCompany) {
                lblTitle.setText(currentClient == null ? container.getBundle().getString("client.form.new_company")
                        : container.getBundle().getString("client.form.edit_company"));
                txtName.setPromptText(container.getBundle().getString("client.form.name.prompt_company"));
                cmbIdType.setValue("CIF");
            } else {
                lblTitle.setText(currentClient == null ? container.getBundle().getString("client.form.new_client")
                        : container.getBundle().getString("client.form.edit_client"));
                txtName.setPromptText(container.getBundle().getString("client.form.name.prompt_client"));
                cmbIdType.setValue("DNI");
            }
        });

        cmbIdType.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null)
                return;
            switch (newVal) {
                case "DNI":
                    txtTaxId.setPromptText("12345678A");
                    break;
                case "NIE":
                    txtTaxId.setPromptText("X1234567L");
                    break;
                case "CIF":
                    txtTaxId.setPromptText("A12345678");
                    break;
                case "Pasaporte":
                    txtTaxId.setPromptText("N\u00famero de pasaporte");
                    break;
            }
        });

        if (client != null) {
            cmbType.setValue(client.isIsCompany() ? container.getBundle().getString("client.type.company")
                    : container.getBundle().getString("client.type.person"));
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
            cmbType.setValue(container.getBundle().getString("client.type.person"));
        }

        setupPriceLists();
    }

    private void setupPriceLists() {
        try {
            cmbPriceList.setItems(javafx.collections.FXCollections.observableArrayList(priceListUseCase.getAll()));
            cmbPriceList.setConverter(new javafx.util.StringConverter<PriceList>() {
                @Override
                public String toString(PriceList object) {
                    return object == null ? container.getBundle().getString("client.form.price_list.default")
                            : object.getName();
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
            AlertUtil.showWarning(container.getBundle().getString("alert.validation"),
                    container.getBundle().getString("client.error.name_required"));
            return;
        }

        String idType = cmbIdType.getValue();
        String taxId = txtTaxId.getText().trim();

        if (taxId.isEmpty()) {
            AlertUtil.showWarning(container.getBundle().getString("alert.validation"),
                    container.getBundle().getString("client.error.id_required"));
            return;
        }

        if (!validateIdentification(idType, taxId)) {
            AlertUtil.showWarning(container.getBundle().getString("alert.validation"),
                    container.getBundle().getString("client.error.invalid_format") + " " + idType);
            return;
        }
        String email = txtEmail.getText().trim();
        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            AlertUtil.showWarning(container.getBundle().getString("alert.validation"),
                    container.getBundle().getString("client.error.invalid_email"));
            return;
        }

        if (currentClient == null)
            currentClient = new Client();

        currentClient.setIsCompany(container.getBundle().getString("client.type.company").equals(cmbType.getValue()));
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
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("error.save") + ": " + e.getMessage());
        }
    }

    @FXML
    private void handleCancel() {
        close();
    }

    private void close() {
        ((Stage) txtName.getScene().getWindow()).close();
    }

    private boolean validateIdentification(String type, String id) {
        if (id == null || id.isEmpty())
            return false;
        id = id.toUpperCase().trim();

        switch (type) {
            case "DNI":
                return validateDNI(id);
            case "NIE":
                return validateNIE(id);
            case "CIF":
                return validateCIF(id);
            case "Pasaporte":
                return id.length() >= 5; // Validaci\u00f3n b\u00e1sica
            default:
                return true;
        }
    }

    private boolean validateDNI(String dni) {
        if (!dni.matches("^[0-9]{8}[TRWAGMYFPDXBNJZSQVHLCKE]$"))
            return false;
        String numberPart = dni.substring(0, 8);
        char letter = dni.charAt(8);
        return calculateDNILetter(Integer.parseInt(numberPart)) == letter;
    }

    private boolean validateNIE(String nie) {
        if (!nie.matches("^[XYZ][0-9]{7}[TRWAGMYFPDXBNJZSQVHLCKE]$"))
            return false;
        String nieFormatted = nie.replace('X', '0').replace('Y', '1').replace('Z', '2');
        String numberPart = nieFormatted.substring(0, 8);
        char letter = nie.charAt(8);
        return calculateDNILetter(Integer.parseInt(numberPart)) == letter;
    }

    private char calculateDNILetter(int number) {
        String letters = "TRWAGMYFPDXBNJZSQVHLCKE";
        return letters.charAt(number % 23);
    }

    private boolean validateCIF(String cif) {
        if (!cif.matches("^[ABCDEFGHJNPQRSUVW][0-9]{7}[A-J0-9]$"))
            return false;

        String control = cif.substring(8);
        String digits = cif.substring(1, 8);
        int sumEven = 0;
        int sumOdd = 0;

        for (int i = 0; i < digits.length(); i++) {
            int digit = Character.getNumericValue(digits.charAt(i));
            if (i % 2 == 0) { // Posiciones impares de la cadena (1, 3, 5, 7) -> \u00edndices pares 0, 2, 4, 6
                int doubleDigit = digit * 2;
                sumOdd += (doubleDigit / 10) + (doubleDigit % 10);
            } else {
                sumEven += digit;
            }
        }

        int totalSum = sumEven + sumOdd;
        int lastDigit = totalSum % 10;
        int controlDigit = (lastDigit == 0) ? 0 : (10 - lastDigit);
        char controlLetter = "JABCDEFGHI".charAt(controlDigit);

        if (Character.isDigit(control.charAt(0))) {
            return Integer.parseInt(control) == controlDigit;
        } else {
            return control.charAt(0) == controlLetter;
        }
    }
}
