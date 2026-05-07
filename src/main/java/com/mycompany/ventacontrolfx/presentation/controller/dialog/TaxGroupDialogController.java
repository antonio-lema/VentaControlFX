package com.mycompany.ventacontrolfx.presentation.controller.dialog;

import com.mycompany.ventacontrolfx.application.usecase.TaxManagementUseCase;
import com.mycompany.ventacontrolfx.domain.model.TaxGroup;
import com.mycompany.ventacontrolfx.domain.model.TaxRate;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TaxGroupDialogController implements Injectable {

    @FXML private TextField txtName;
    @FXML private CheckBox chkDefault;
    @FXML private VBox ratesListContainer;

    private TaxManagementUseCase taxManagementUseCase;
    private TaxGroup currentGroup;
    private boolean saveClicked = false;

    @Override
    public void inject(ServiceContainer container) {
        this.taxManagementUseCase = container.getTaxManagementUseCase();
    }

    public void init(TaxGroup group) {
        this.currentGroup = group;

        if (group != null && group.getTaxGroupId() > 0) {
            txtName.setText(group.getName());
            chkDefault.setSelected(group.isDefault());
        }

        loadRates();
    }

    private void loadRates() {
        try {
            List<TaxRate> activeRates = taxManagementUseCase.getAllTaxRates();
            ratesListContainer.getChildren().clear();

            for (TaxRate rate : activeRates) {
                // Crear un CheckBox moderno que reaccione al clic intuitivo
                CheckBox cb = new CheckBox(rate.getName() + " (" + String.format("%.2f%%", rate.getRate()) + ")");
                cb.getStyleClass().add("permission-checkbox");
                cb.setUserData(rate);
                cb.setMaxWidth(Double.MAX_VALUE);
                cb.setCursor(javafx.scene.Cursor.HAND);

                // Preseleccionar si ya est\u00e1 en el grupo
                if (currentGroup != null && currentGroup.getRates() != null) {
                    boolean isIncluded = currentGroup.getRates().stream()
                            .anyMatch(r -> r.getTaxRateId() == rate.getTaxRateId());
                    cb.setSelected(isIncluded);
                }

                ratesListContainer.getChildren().add(cb);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Error", "No se pudieron cargar las tasas impositivas.");
        }
    }

    @FXML
    private void handleSave() {
        if (txtName.getText().trim().isEmpty()) {
            AlertUtil.showWarning("Validaci\u00f3n", "El nombre es obligatorio.");
            return;
        }

        if (currentGroup == null) {
            currentGroup = new TaxGroup();
            currentGroup.setRates(new ArrayList<>());
        }

        currentGroup.setName(txtName.getText().trim());
        currentGroup.setDefault(chkDefault.isSelected());

        // Extraer tasas seleccionadas
        List<TaxRate> selectedRates = new ArrayList<>();
        ratesListContainer.getChildren().forEach(node -> {
            if (node instanceof CheckBox && ((CheckBox) node).isSelected()) {
                selectedRates.add((TaxRate) node.getUserData());
            }
        });
        currentGroup.setRates(selectedRates);

        try {
            if (currentGroup.getTaxGroupId() == 0) {
                taxManagementUseCase.saveTaxGroup(currentGroup);
            } else {
                taxManagementUseCase.updateTaxGroup(currentGroup);
            }
            saveClicked = true;
            close();
        } catch (SQLException e) {
            AlertUtil.showError("Error", "No se pudo guardar el grupo: " + e.getMessage());
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

