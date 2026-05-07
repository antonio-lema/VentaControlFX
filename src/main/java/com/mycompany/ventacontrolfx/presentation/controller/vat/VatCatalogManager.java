package com.mycompany.ventacontrolfx.presentation.controller.vat;

import com.mycompany.ventacontrolfx.application.usecase.TaxManagementUseCase;
import com.mycompany.ventacontrolfx.domain.model.TaxGroup;
import com.mycompany.ventacontrolfx.domain.model.TaxRate;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestiona la pestaña de Catálogo (Tasas y Grupos) del VatManagementController.
 */
public class VatCatalogManager {

    private final ServiceContainer container;
    private final TaxManagementUseCase taxManagementUseCase;
    
    // UI References
    private final TableView<TaxRate> taxRatesTable;
    private final TableView<TaxGroup> taxGroupsTable;

    public VatCatalogManager(
            ServiceContainer container,
            TaxManagementUseCase taxManagementUseCase,
            TableView<TaxRate> taxRatesTable,
            TableView<TaxGroup> taxGroupsTable) {
        this.container = container;
        this.taxManagementUseCase = taxManagementUseCase;
        this.taxRatesTable = taxRatesTable;
        this.taxGroupsTable = taxGroupsTable;
    }

    public void init() {
        setupTables();
        loadData();
    }

    @SuppressWarnings("unchecked")
    private void setupTables() {
        // --- Tax Rates Table ---
        TableColumn<TaxRate, String> colRateName = (TableColumn<TaxRate, String>) taxRatesTable.getColumns().get(0);
        TableColumn<TaxRate, String> colRateValue = (TableColumn<TaxRate, String>) taxRatesTable.getColumns().get(1);
        TableColumn<TaxRate, String> colRateCountry = (TableColumn<TaxRate, String>) taxRatesTable.getColumns().get(2);
        TableColumn<TaxRate, String> colRateStatus = (TableColumn<TaxRate, String>) taxRatesTable.getColumns().get(3);
        TableColumn<TaxRate, Void> colRateActions = (TableColumn<TaxRate, Void>) taxRatesTable.getColumns().get(4);

        colRateName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colRateValue.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%.2f%%", cell.getValue().getRate())));
        colRateCountry.setCellValueFactory(new PropertyValueFactory<>("country"));
        colRateStatus.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().isActive()
                ? container.getBundle().getString("status.active")
                : container.getBundle().getString("status.inactive")));

        setupTaxRateActionsColumn(colRateActions);

        // --- Tax Groups Table ---
        TableColumn<TaxGroup, String> colGroupName = (TableColumn<TaxGroup, String>) taxGroupsTable.getColumns().get(0);
        TableColumn<TaxGroup, String> colGroupDefault = (TableColumn<TaxGroup, String>) taxGroupsTable.getColumns().get(1);
        TableColumn<TaxGroup, String> colGroupRates = (TableColumn<TaxGroup, String>) taxGroupsTable.getColumns().get(2);
        TableColumn<TaxGroup, Void> colGroupActions = (TableColumn<TaxGroup, Void>) taxGroupsTable.getColumns().get(3);

        colGroupName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colGroupDefault.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().isDefault() ? "SÍ" : "—"));
        colGroupRates.setCellValueFactory(cell -> {
            List<TaxRate> rates = cell.getValue().getRates();
            if (rates == null || rates.isEmpty()) return new SimpleStringProperty(container.getBundle().getString("vat.group.none"));
            return new SimpleStringProperty(rates.stream()
                    .map(r -> r.getName() + " (" + r.getRate() + "%)")
                    .collect(Collectors.joining(", ")));
        });

        setupTaxGroupActionsColumn(colGroupActions);
    }

    public void loadData() {
        try {
            if (taxManagementUseCase != null) {
                taxRatesTable.setItems(FXCollections.observableArrayList(taxManagementUseCase.getAllTaxRates()));
                taxGroupsTable.setItems(FXCollections.observableArrayList(taxManagementUseCase.getAllTaxGroups()));
            }
        } catch (SQLException e) {
            AlertUtil.showError(container.getBundle().getString("alert.error"), e.getMessage());
        }
    }

    private void setupTaxRateActionsColumn(TableColumn<TaxRate, Void> column) {
        column.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button();
            private final Button deleteBtn = new Button();
            private final HBox btnContainer = new HBox(8, editBtn, deleteBtn);
            {
                editBtn.getStyleClass().addAll("btn-sm", "btn-secondary");
                editBtn.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.EDIT));
                editBtn.setOnAction(e -> handleEditTaxRate(getTableView().getItems().get(getIndex())));

                deleteBtn.getStyleClass().addAll("btn-sm", "btn-danger");
                deleteBtn.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.TRASH));
                deleteBtn.setOnAction(e -> handleDeleteTaxRate(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnContainer);
            }
        });
    }

    private void setupTaxGroupActionsColumn(TableColumn<TaxGroup, Void> column) {
        column.setCellFactory(param -> new TableCell<>() {
            private final Button editBtn = new Button();
            private final Button deleteBtn = new Button();
            private final HBox btnContainer = new HBox(8, editBtn, deleteBtn);
            {
                editBtn.getStyleClass().addAll("btn-sm", "btn-secondary");
                editBtn.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.EDIT));
                editBtn.setOnAction(e -> handleEditTaxGroup(getTableView().getItems().get(getIndex())));

                deleteBtn.getStyleClass().addAll("btn-sm", "btn-danger");
                deleteBtn.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.TRASH));
                deleteBtn.setOnAction(e -> handleDeleteTaxGroup(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnContainer);
            }
        });
    }

    public void handleAddTaxRate() {
        TaxRate newRate = new TaxRate();
        newRate.setActive(true);
        newRate.setCountry("ES");
        com.mycompany.ventacontrolfx.presentation.navigation.ModalService.showTransparentModal(
                "/view/dialog/tax_rate_dialog.fxml",
                container.getBundle().getString("vat.dialog.tax_rate.title_new"),
                container,
                (com.mycompany.ventacontrolfx.presentation.controller.dialog.TaxRateDialogController ctrl) -> ctrl.init(newRate)
        );
        loadData();
    }

    public void handleEditTaxRate(TaxRate rate) {
        com.mycompany.ventacontrolfx.presentation.navigation.ModalService.showTransparentModal(
                "/view/dialog/tax_rate_dialog.fxml",
                container.getBundle().getString("vat.dialog.tax_rate.title_edit"),
                container,
                (com.mycompany.ventacontrolfx.presentation.controller.dialog.TaxRateDialogController ctrl) -> ctrl.init(rate)
        );
        loadData();
    }

    public void handleDeleteTaxRate(TaxRate rate) {
        boolean confirmed = AlertUtil.showConfirmation(container.getBundle().getString("vat.confirm.delete_rate.title"),
                container.getBundle().getString("alert.confirm"),
                String.format(container.getBundle().getString("vat.confirm.delete_rate.msg"), rate.getName()));
        if (confirmed) {
            try {
                taxManagementUseCase.deleteTaxRate(rate.getTaxRateId());
                loadData();
                AlertUtil.showInfo(container.getBundle().getString("alert.success"), 
                        container.getBundle().getString("vat.dialog.tax_rate.success_delete"));
            } catch (SQLException e) {
                AlertUtil.showError(container.getBundle().getString("alert.error"), 
                        container.getBundle().getString("vat.dialog.tax_rate.error_delete"));
            }
        }
    }

    public void handleAddTaxGroup(Runnable onGroupsChanged) {
        com.mycompany.ventacontrolfx.presentation.navigation.ModalService.showTransparentModal(
                "/view/dialog/tax_group_dialog.fxml",
                container.getBundle().getString("vat.dialog.tax_group.title_new"),
                container,
                (com.mycompany.ventacontrolfx.presentation.controller.dialog.TaxGroupDialogController ctrl) -> ctrl.init(new TaxGroup())
        );
        loadData();
        if (onGroupsChanged != null) onGroupsChanged.run();
    }

    public void handleEditTaxGroup(TaxGroup group) {
        com.mycompany.ventacontrolfx.presentation.navigation.ModalService.showTransparentModal(
                "/view/dialog/tax_group_dialog.fxml",
                container.getBundle().getString("vat.dialog.tax_group.title_edit"),
                container,
                (com.mycompany.ventacontrolfx.presentation.controller.dialog.TaxGroupDialogController ctrl) -> ctrl.init(group)
        );
        loadData();
    }

    public void handleDeleteTaxGroup(TaxGroup group) {
        boolean confirmed = AlertUtil.showConfirmation(
                container.getBundle().getString("vat.confirm.delete_group.title"),
                container.getBundle().getString("alert.confirm"),
                String.format(container.getBundle().getString("vat.confirm.delete_group.msg"), group.getName()));
        if (confirmed) {
            try {
                taxManagementUseCase.deleteTaxGroup(group.getTaxGroupId());
                loadData();
                AlertUtil.showInfo(container.getBundle().getString("alert.success"), 
                        container.getBundle().getString("vat.dialog.tax_group.success_delete"));
            } catch (SQLException e) {
                AlertUtil.showError(container.getBundle().getString("alert.error"), 
                        container.getBundle().getString("vat.dialog.tax_group.error_delete"));
            }
        }
    }
}


