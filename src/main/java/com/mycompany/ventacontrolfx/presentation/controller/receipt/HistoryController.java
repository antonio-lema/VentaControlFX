package com.mycompany.ventacontrolfx.presentation.controller.receipt;

import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.domain.model.HistoryStats;
import com.mycompany.ventacontrolfx.application.usecase.SaleUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import com.mycompany.ventacontrolfx.presentation.util.Searchable;
import com.mycompany.ventacontrolfx.presentation.util.RealTimeSearchBinder;
import com.mycompany.ventacontrolfx.shared.util.DateFilterUtils;
import com.mycompany.ventacontrolfx.presentation.controller.receipt.HistoryTableManager;
import com.mycompany.ventacontrolfx.presentation.controller.receipt.HistoryDetailsManager;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class HistoryController implements Injectable, Searchable {

    @FXML private DatePicker datePickerStart, datePickerEnd;
    @FXML private TextField txtSearchId;
    @FXML private TableView<Sale> salesTable;
    @FXML private TableColumn<Sale, Integer> colId;
    @FXML private TableColumn<Sale, String> colUser, colDate, colMethod, colFiscalStatus;
    @FXML private TableColumn<Sale, Double> colTotal;
    @FXML private Label lblTotalSalesCount, lblTotalAmount, lblTotalCash, lblTotalCard, lblCount;
    @FXML private ComboBox<Integer> cmbRowLimit;
    @FXML private VBox detailsPanel, detailsItemsContainer, skeletonTableContainer;
    @FXML private Label lblSaleId, lblSaleFullDate, lblPaymentMethod, lblTotalAmountDetail, lblReturnBadge;
    @FXML private Button btnReturn, btnPrint, btnCorrection, btnResendAeat;
    @FXML private HBox quickFilterContainer;

    private ServiceContainer container;
    private HistoryTableManager tableManager;
    private HistoryDetailsManager detailsManager;
    private HistoryActionManager actionManager;
    private HistoryDataManager dataManager;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        SaleUseCase saleUseCase = container.getSaleUseCase();

        // 1. Inicializar Managers
        this.tableManager = new HistoryTableManager(container, salesTable, colId, colUser, colDate, colMethod, colFiscalStatus, colTotal, cmbRowLimit, lblCount);
        this.tableManager.init();

        this.detailsManager = new HistoryDetailsManager(container, saleUseCase, detailsPanel, detailsItemsContainer, lblSaleId, lblSaleFullDate, lblPaymentMethod, lblTotalAmountDetail, lblReturnBadge, btnReturn, btnResendAeat);
        this.actionManager = new HistoryActionManager(container, saleUseCase);
        
        this.dataManager = new HistoryDataManager(container, skeletonTableContainer, salesTable, Arrays.asList(lblTotalSalesCount, lblTotalAmount, lblTotalCash, lblTotalCard));

        // 2. Eventos y Filtros
        DateFilterUtils.addQuickFilters(quickFilterContainer, datePickerStart, datePickerEnd, this::loadSalesDirect);
        if (txtSearchId != null) RealTimeSearchBinder.bind(txtSearchId, query -> handleSearch());
        salesTable.getSelectionModel().selectedItemProperty().addListener((obs, old, newVal) -> { if (newVal != null) detailsManager.showDetails(newVal); });

        setupPermissions();
        loadSalesDirect();
    }

    private void setupPermissions() {
        boolean canReturn = container.getUserSession().hasPermission("venta.devolucion");
        btnReturn.setVisible(canReturn); btnReturn.setManaged(canReturn);
        boolean canCorrect = container.getUserSession().hasPermission("venta.subsanar");
        btnCorrection.setVisible(canCorrect); btnCorrection.setManaged(canCorrect);
        boolean canResend = container.getUserSession().hasPermission("fiscal.reenviar");
        if (btnResendAeat != null) { btnResendAeat.setVisible(canResend); btnResendAeat.setManaged(canResend); }
    }

    @FXML private void loadSales() {
        if (txtSearchId != null && !txtSearchId.getText().trim().isEmpty()) handleSearch();
        else loadSalesDirect();
    }

    private void loadSalesDirect() {
        LocalDate start = datePickerStart.getValue();
        LocalDate end = datePickerEnd.getValue();

        // Si es el primer arranque y no hay nada seleccionado, forzamos HOY
        if (start == null && end == null && salesTable.getItems().isEmpty()) {
            start = LocalDate.now();
            end = LocalDate.now();
            datePickerStart.setValue(start);
            datePickerEnd.setValue(end);
            // También intentaremos seleccionar el chip de "Hoy" si existe
            if (quickFilterContainer != null && !quickFilterContainer.getChildren().isEmpty()) {
                for (javafx.scene.Node node : quickFilterContainer.getChildren()) {
                    if (node instanceof ToggleButton && ((ToggleButton)node).getText().equals("Hoy")) {
                        ((ToggleButton)node).setSelected(true);
                    }
                }
            }
        }

        // Si el usuario borra las fechas o pulsa "Todo", cargamos el rango completo
        LocalDate queryStart = (start == null) ? LocalDate.of(2000, 1, 1) : start;
        LocalDate queryEnd = (end == null) ? LocalDate.of(2100, 1, 1) : end;

        if (start != null && end != null && start.isAfter(end)) { 
            AlertUtil.showError(container.getBundle().getString("alert.error"), container.getBundle().getString("history.error.date_range")); 
            return; 
        }

        dataManager.loadSales(queryStart, queryEnd, result -> {
            updateUIWithResults((HistoryStats) result[0], (List<Sale>) result[1]);
        });
    }

    @FXML private void handleSearch() {
        String search = txtSearchId.getText().trim();
        if (search.isEmpty()) { loadSalesDirect(); return; }
        try {
            dataManager.searchById(Integer.parseInt(search), result -> {
                updateUIWithResults((HistoryStats) result[0], (List<Sale>) result[1]);
                if (result != null && !((List<?>)result[1]).isEmpty()) {
                    tableManager.select((Sale) ((List<?>)result[1]).get(0));
                }
            });
        } catch (NumberFormatException e) {
            // Ignorar para evitar alertas molestas mientras se escribe
        }
    }

    private void updateUIWithResults(HistoryStats stats, List<Sale> sales) {
        lblTotalSalesCount.setText("\ud83d\udcca " + stats.getCount());
        lblTotalAmount.setText("\ud83d\udcb0 " + String.format("%.2f \u20ac", stats.getTotalRevenue()));
        lblTotalCash.setText("\ud83d\udcb5 " + String.format("%.2f \u20ac", stats.getTotalCash()));
        lblTotalCard.setText("\ud83d\udcb3 " + String.format("%.2f \u20ac", stats.getTotalCard()));
        if (lblCount != null) lblCount.setText("\ud83d\udd0d " + stats.getCount() + " " + container.getBundle().getString("history.count_suffix"));
        
        tableManager.setData(sales);
        handleCloseDetails();
    }

    @FXML private void handleClearFilters() {
        txtSearchId.clear();
        datePickerStart.setValue(LocalDate.now());
        datePickerEnd.setValue(LocalDate.now());
        loadSalesDirect();
    }

    @FXML private void handleCloseDetails() { detailsManager.hide(); tableManager.clearSelection(); }
    @FXML private void handlePrintTicket() { actionManager.handlePrintTicket(tableManager.getSelection()); }
    @FXML private void handleRegisterReturn() { actionManager.handleRegisterReturn(tableManager.getSelection(), this::loadSalesDirect); }
    @FXML private void handleCorrection() { actionManager.handleCorrection(tableManager.getSelection(), this::loadSalesDirect); }
    
    @FXML private void handleResendAeat() {
        btnResendAeat.setDisable(true);
        actionManager.handleResendAeat(tableManager.getSelection(), () -> {
            btnResendAeat.setDisable(false);
            loadSalesDirect();
        });
    }

    @Override public void handleSearch(String text) { if (txtSearchId != null) { txtSearchId.setText(text); handleSearch(); } }
}
