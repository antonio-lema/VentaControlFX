package com.mycompany.ventacontrolfx.presentation.controller.dialog;

import com.mycompany.ventacontrolfx.application.usecase.SaleUseCase;
import com.mycompany.ventacontrolfx.domain.model.Return;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import com.mycompany.ventacontrolfx.shared.util.DateFilterUtils;
import com.mycompany.ventacontrolfx.presentation.controller.receipt.ReturnTableManager;
import com.mycompany.ventacontrolfx.presentation.controller.receipt.ReturnDetailsManager;
import com.mycompany.ventacontrolfx.presentation.controller.receipt.ReturnActionManager;
import com.mycompany.ventacontrolfx.presentation.util.RealTimeSearchBinder;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReturnListController implements Injectable {

    @FXML
    private DatePicker datePickerStart, datePickerEnd;
    @FXML
    private HBox quickFilterContainer;
    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<String> cmbPaymentMethod;
    @FXML
    private ComboBox<Integer> cmbRowLimit;
    @FXML
    private TableView<Return> returnsTable;
    @FXML
    private TableColumn<Return, Integer> colSaleId, colClosure;
    @FXML
    private TableColumn<Return, String> colId, colUser, colDate, colReason, colActions;
    @FXML
    private TableColumn<Return, Double> colAmount;
    @FXML
    private TableColumn<Return, String> colFiscalStatus;
    @FXML
    private Label lblCount, lblTotalRefunded, lblKpiTotal, lblKpiCount, lblKpiAverage, lblKpiLast;

    @FXML
    private VBox detailsPanel, detailsItemsContainer;
    @FXML
    private Label lblDetailReturnId, lblDetailSaleId, lblDetailDate, lblDetailReason, lblDetailUser, lblDetailTotal;

    private SaleUseCase saleUseCase;
    private ServiceContainer container;
    private ObservableList<Return> masterData = FXCollections.observableArrayList();
    private final DateTimeFormatter kpiFormatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

    private ReturnTableManager tableManager;
    private ReturnDetailsManager detailsManager;
    private ReturnActionManager actionManager;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.saleUseCase = container.getSaleUseCase();

        // 1. Inicializar Managers
        this.tableManager = new ReturnTableManager(
            container, returnsTable, colId, colSaleId, colClosure, colUser, colDate, colReason, colActions, colAmount, colFiscalStatus, cmbRowLimit, lblCount
        );
        this.tableManager.init(
            this::handleReprint, 
            saleId -> actionManager.handleViewOriginalTicket(saleId), 
            item -> detailsManager.show(item)
        );

        this.detailsManager = new ReturnDetailsManager(
            container, saleUseCase, detailsPanel, detailsItemsContainer, lblDetailReturnId, 
            lblDetailSaleId, lblDetailDate, lblDetailReason, lblDetailUser, lblDetailTotal
        );

        this.actionManager = new ReturnActionManager(container, saleUseCase, container.getGetSaleTicketUseCase());

        // 2. Setup Filtros UI
        if (cmbPaymentMethod != null) {
            cmbPaymentMethod.setItems(FXCollections.observableArrayList(
                container.getBundle().getString("returns.filter.all"),
                container.getBundle().getString("returns.filter.cash"),
                container.getBundle().getString("returns.filter.card")));
            cmbPaymentMethod.setValue(container.getBundle().getString("returns.filter.all"));
        }

        DateFilterUtils.addQuickFilters(quickFilterContainer, datePickerStart, datePickerEnd, container.getBundle(), this::loadReturns);

        // 3. Listeners
        if (txtSearch != null) RealTimeSearchBinder.bind(txtSearch, query -> applyFilters());

        if (datePickerStart != null) datePickerStart.valueProperty().addListener((obs, old, nv) -> loadReturns());
        if (datePickerEnd != null) datePickerEnd.valueProperty().addListener((obs, old, nv) -> loadReturns());
        if (cmbPaymentMethod != null) cmbPaymentMethod.valueProperty().addListener((obs, old, nv) -> applyFilters());

        loadReturns();
    }

    @FXML
    public void loadReturns() {
        try {
            LocalDate start = datePickerStart.getValue();
            LocalDate end = datePickerEnd.getValue();
            if (start == null || end == null) {
                start = LocalDate.of(2000, 1, 1);
                end = LocalDate.of(2100, 1, 1);
            }
            masterData.setAll(saleUseCase.getReturnsHistory(start, end));
            applyFilters();
        } catch (SQLException e) {
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("returns.error.load") + ": " + e.getMessage());
        }
    }

    @FXML
    private void applyFilters() {
        String filterText = txtSearch != null ? txtSearch.getText().toLowerCase().trim() : "";
        String paymentFilter = cmbPaymentMethod != null ? cmbPaymentMethod.getValue() : container.getBundle().getString("returns.filter.all");

        FilteredList<Return> filteredData = new FilteredList<>(masterData, r -> {
            boolean matchesText = filterText.isEmpty() || 
                (r.getReason() != null && r.getReason().toLowerCase().contains(filterText)) ||
                (r.getUserName() != null && r.getUserName().toLowerCase().contains(filterText)) ||
                String.valueOf(r.getSaleId()).contains(filterText);

            boolean matchesPayment = true;
            if (paymentFilter != null && !container.getBundle().getString("returns.filter.all").equals(paymentFilter)) {
                if (container.getBundle().getString("returns.filter.cash").equalsIgnoreCase(paymentFilter)) {
                    matchesPayment = "EFECTIVO".equalsIgnoreCase(r.getPaymentMethod()) || "CASH".equalsIgnoreCase(r.getPaymentMethod());
                } else if (container.getBundle().getString("returns.filter.card").equalsIgnoreCase(paymentFilter)) {
                    matchesPayment = "TARJETA".equalsIgnoreCase(r.getPaymentMethod()) || "CARD".equalsIgnoreCase(r.getPaymentMethod());
                }
            }
            return matchesText && matchesPayment;
        });

        tableManager.setData(filteredData);
        updateSummary(filteredData);
    }

    private void updateSummary(List<Return> list) {
        int count = list.size();
        double total = list.stream().mapToDouble(Return::getTotalRefunded).sum();
        double average = count > 0 ? (total / count) : 0;
        String lastReturnDate = "-";
        if (count > 0) {
            lastReturnDate = list.stream()
                .filter(r -> r.getReturnDatetime() != null)
                .max((r1, r2) -> r1.getReturnDatetime().compareTo(r2.getReturnDatetime()))
                .map(r -> r.getReturnDatetime().format(kpiFormatter)).orElse("-");
        }

        if (lblCount != null) lblCount.setVisible(true);
        if (lblTotalRefunded != null) lblTotalRefunded.setText(String.format("%.2f \u20ac", total));
        if (lblKpiTotal != null) lblKpiTotal.setText(String.format("%.2f \u20ac", total));
        if (lblKpiCount != null) lblKpiCount.setText(String.valueOf(count));
        if (lblKpiAverage != null) lblKpiAverage.setText(String.format("%.2f \u20ac", average));
        if (lblKpiLast != null) lblKpiLast.setText(lastReturnDate);
    }

    @FXML
    private void handleClearFilters() {
        if (txtSearch != null) txtSearch.clear();
        if (cmbPaymentMethod != null) cmbPaymentMethod.setValue(container.getBundle().getString("returns.filter.all"));
        datePickerStart.setValue(LocalDate.now());
        datePickerEnd.setValue(LocalDate.now());
        loadReturns();
    }

    @FXML
    private void handleCloseDetails() {
        detailsManager.hide();
        returnsTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handleReprint() {
        actionManager.handleReprint(tableManager.getSelection());
    }

    @FXML
    private void handleViewOriginalTicket() {
        Return selected = tableManager.getSelection();
        if (selected != null) actionManager.handleViewOriginalTicket(selected.getSaleId());
    }
}


