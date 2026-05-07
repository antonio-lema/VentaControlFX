package com.mycompany.ventacontrolfx.presentation.controller.vat;

import com.mycompany.ventacontrolfx.application.usecase.TaxManagementUseCase;
import com.mycompany.ventacontrolfx.domain.model.PriceUpdateLog;
import com.mycompany.ventacontrolfx.domain.model.TaxRevision;
import com.mycompany.ventacontrolfx.domain.repository.IPriceUpdateLogRepository;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.shared.util.DateFilterUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestiona la pestaña de Historial de IVA y Precios del VatManagementController.
 */
public class VatHistoryManager {

    private final ServiceContainer container;
    private final TaxManagementUseCase taxManagementUseCase;
    private final IPriceUpdateLogRepository priceLogRepository;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // UI References
    private final TableView<TaxRevision> historyTable;
    private final TableView<PriceUpdateLog> priceLogTable;
    private final ComboBox<String> cmbHistoryScope;
    private final HBox quickFilterContainer;
    
    private Integer histFilterDays = null;

    public VatHistoryManager(
            ServiceContainer container,
            TaxManagementUseCase taxManagementUseCase,
            IPriceUpdateLogRepository priceLogRepository,
            TableView<TaxRevision> historyTable,
            TableView<PriceUpdateLog> priceLogTable,
            ComboBox<String> cmbHistoryScope,
            HBox quickFilterContainer) {
        this.container = container;
        this.taxManagementUseCase = taxManagementUseCase;
        this.priceLogRepository = priceLogRepository;
        this.historyTable = historyTable;
        this.priceLogTable = priceLogTable;
        this.cmbHistoryScope = cmbHistoryScope;
        this.quickFilterContainer = quickFilterContainer;
    }

    public void init() {
        setupTaxHistoryTable();
        setupPriceLogTable();
        setupHistoryFilter();
    }

    @SuppressWarnings("unchecked")
    private void setupTaxHistoryTable() {
        if (historyTable == null) return;
        
        TableColumn<TaxRevision, String> colDate = (TableColumn<TaxRevision, String>) historyTable.getColumns().get(0);
        TableColumn<TaxRevision, String> colEndDate = (TableColumn<TaxRevision, String>) historyTable.getColumns().get(1);
        TableColumn<TaxRevision, String> colScope = (TableColumn<TaxRevision, String>) historyTable.getColumns().get(2);
        TableColumn<TaxRevision, String> colTarget = (TableColumn<TaxRevision, String>) historyTable.getColumns().get(3);
        TableColumn<TaxRevision, String> colRate = (TableColumn<TaxRevision, String>) historyTable.getColumns().get(4);
        TableColumn<TaxRevision, String> colReason = (TableColumn<TaxRevision, String>) historyTable.getColumns().get(5);

        colDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStartDate().format(formatter)));
        colEndDate.setCellValueFactory(cell -> cell.getValue().getEndDate() == null
                ? new SimpleStringProperty("Vigente")
                : new SimpleStringProperty(cell.getValue().getEndDate().format(formatter)));
        colScope.setCellValueFactory(new PropertyValueFactory<>("scope"));
        colTarget.setCellValueFactory(cell -> {
            TaxRevision r = cell.getValue();
            return new SimpleStringProperty(switch (r.getScope()) {
                case GLOBAL -> "Global";
                case CATEGORY -> "Cat. ID: " + r.getCategoryId();
                case PRODUCT -> "Prod. ID: " + r.getProductId();
            });
        });
        colRate.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%.2f%%", cell.getValue().getRate())));
        colReason.setCellValueFactory(new PropertyValueFactory<>("reason"));
    }

    @SuppressWarnings("unchecked")
    private void setupPriceLogTable() {
        if (priceLogTable == null) return;

        TableColumn<PriceUpdateLog, String> colLogDate = (TableColumn<PriceUpdateLog, String>) priceLogTable.getColumns().get(0);
        TableColumn<PriceUpdateLog, String> colLogType = (TableColumn<PriceUpdateLog, String>) priceLogTable.getColumns().get(1);
        TableColumn<PriceUpdateLog, String> colLogScope = (TableColumn<PriceUpdateLog, String>) priceLogTable.getColumns().get(2);
        TableColumn<PriceUpdateLog, String> colLogCategory = (TableColumn<PriceUpdateLog, String>) priceLogTable.getColumns().get(3);
        TableColumn<PriceUpdateLog, String> colLogValue = (TableColumn<PriceUpdateLog, String>) priceLogTable.getColumns().get(4);
        TableColumn<PriceUpdateLog, String> colLogProducts = (TableColumn<PriceUpdateLog, String>) priceLogTable.getColumns().get(5);
        TableColumn<PriceUpdateLog, String> colLogReason = (TableColumn<PriceUpdateLog, String>) priceLogTable.getColumns().get(6);

        colLogDate.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getAppliedAt().format(formatter)));
        colLogType.setCellValueFactory(cell -> {
            String type = cell.getValue().getUpdateType();
            String display = switch (type) {
                case "percentage" -> container.getBundle().getString("vat.update.type.percentage");
                case "fixed" -> container.getBundle().getString("vat.update.type.fixed");
                case "rounding" -> container.getBundle().getString("vat.update.type.rounding");
                default -> type;
            };
            return new SimpleStringProperty(display);
        });
        colLogScope.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getScope()));
        colLogCategory.setCellValueFactory(cell -> {
            String catName = cell.getValue().getCategoryName();
            return new SimpleStringProperty(catName != null ? catName : "—");
        });
        colLogValue.setCellValueFactory(cell -> {
            String type = cell.getValue().getUpdateType();
            double val = cell.getValue().getValue();
            String display = switch (type) {
                case "percentage" -> String.format("%.2f%%", val);
                case "fixed" -> String.format("%.2f €", val);
                case "rounding" -> String.format("x.%02.0f", val * 100);
                default -> String.valueOf(val);
            };
            return new SimpleStringProperty(display);
        });
        colLogProducts.setCellValueFactory(cell -> new SimpleStringProperty(String.valueOf(cell.getValue().getProductsUpdated())));
        colLogReason.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getReason()));
    }

    private void setupHistoryFilter() {
        if (cmbHistoryScope == null) return;
        cmbHistoryScope.setItems(FXCollections.observableArrayList("Global", "Categoría (Todo)", "Producto (Todo)"));
        cmbHistoryScope.getSelectionModel().selectFirst();
        cmbHistoryScope.getSelectionModel().selectedItemProperty().addListener((obs, old, nv) -> refreshHistory());
        refreshHistory();
    }

    public void setFilterDays(Integer days) {
        this.histFilterDays = days;
        refreshHistory();
    }

    public void refreshAll() {
        this.histFilterDays = null;
        if (quickFilterContainer != null) {
            DateFilterUtils.addQuickFilters(quickFilterContainer, (label) -> {
                if (label.equals(container.getBundle().getString("filter.date.today"))) histFilterDays = 1;
                else if (label.equals(container.getBundle().getString("filter.date.7d"))) histFilterDays = 7;
                else if (label.equals(container.getBundle().getString("filter.date.this_month"))) histFilterDays = 30;
                else histFilterDays = null;
            }, container.getBundle(), this::refreshHistory);
        }
        refreshHistory();
        refreshPriceLog();
    }

    public void refreshHistory() {
        try {
            if (taxManagementUseCase != null) {
                TaxRevision.Scope scope = null;
                String scopeStr = cmbHistoryScope != null ? cmbHistoryScope.getSelectionModel().getSelectedItem() : null;
                
                if (container.getBundle().getString("vat.history.scope.global").equals(scopeStr))
                    scope = TaxRevision.Scope.GLOBAL;
                else if (container.getBundle().getString("vat.history.scope.category").equals(scopeStr))
                    scope = TaxRevision.Scope.CATEGORY;
                else if (container.getBundle().getString("vat.history.scope.product").equals(scopeStr))
                    scope = TaxRevision.Scope.PRODUCT;

                List<TaxRevision> history = taxManagementUseCase.getTaxHistory(scope);

                if (histFilterDays != null) {
                    LocalDateTime cutoff = LocalDateTime.now().minus(histFilterDays, ChronoUnit.DAYS);
                    history = history.stream().filter(r -> r.getStartDate().isAfter(cutoff)).collect(Collectors.toList());
                }

                historyTable.setItems(FXCollections.observableArrayList(history));
            }
        } catch (Exception ignored) {}
        refreshPriceLog();
    }

    public void refreshPriceLog() {
        if (priceLogTable == null) return;
        try {
            List<PriceUpdateLog> logs = priceLogRepository.getAll();
            if (histFilterDays != null) {
                LocalDateTime cutoff = LocalDateTime.now().minus(histFilterDays, ChronoUnit.DAYS);
                logs = logs.stream().filter(l -> l.getAppliedAt().isAfter(cutoff)).collect(Collectors.toList());
            }
            priceLogTable.setItems(FXCollections.observableArrayList(logs));
        } catch (Exception ignored) {}
    }

}


