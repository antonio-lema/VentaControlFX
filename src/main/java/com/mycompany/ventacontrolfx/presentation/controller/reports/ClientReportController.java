package com.mycompany.ventacontrolfx.presentation.controller.reports;

import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.controller.receipt.TicketDetailController;
import com.mycompany.ventacontrolfx.presentation.util.RealTimeSearchBinder;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import com.mycompany.ventacontrolfx.presentation.navigation.ModalService;
import com.mycompany.ventacontrolfx.shared.util.PaginationHelper;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ClientReportController implements Injectable {

    @FXML private TextField txtSearch;
    @FXML private ComboBox<String> cmbDateRange;
    @FXML private Label lblKpiActive, lblKpiActiveVar, lblKpiTotal, lblKpiTotalVar, lblKpiAvgOrder, lblKpiAvgOrderVar, lblKpiLtv, lblKpiLtvVar, lblClientCount;
    @FXML private ComboBox<Integer> cmbRowLimit;
    @FXML private TableView<ClientReportDataManager.ClientRow> clientTable;
    @FXML private TableColumn<ClientReportDataManager.ClientRow, String> colClientName, colClientOrders, colClientTotal;
    @FXML private TableColumn<ClientReportDataManager.ClientRow, Void> colClientStatus;
    @FXML private VBox pnlDetailEmpty, pnlDetailContent, pnlTopProducts;
    @FXML private StackPane spDetailInitial, spAvatar;
    @FXML private Label lblDetailInitials, lblDetailName, lblDetailTier, lblDetailInfo, lblPrefPayment;
    @FXML private TableView<Sale> purchaseTable;
    @FXML private TableColumn<Sale, String> colDate, colItems, colMethod, colAmount;
    @FXML private LineChart<String, Number> chartEvolution;
    @FXML private FontAwesomeIconView iconPrefPayment;

    private ServiceContainer container;
    private ClientReportDataManager dataManager;
    private ClientReportStatsManager statsManager;
    private ClientReportExportManager exportManager;
    private ClientReportDetailManager detailManager;
    private PaginationHelper<ClientReportDataManager.ClientRow> paginationHelper;
    private List<ClientReportDataManager.ClientRow> allRows = new ArrayList<>();

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.dataManager = new ClientReportDataManager(container);
        this.statsManager = new ClientReportStatsManager(container);
        this.exportManager = new ClientReportExportManager();
        this.detailManager = new ClientReportDetailManager(container, spDetailInitial, pnlDetailContent, lblDetailInitials, lblDetailName, lblDetailTier, lblDetailInfo, spAvatar, purchaseTable);

        setupFilters();
        setupClientTable();
        setupPurchaseTable();

        paginationHelper = new PaginationHelper<>(clientTable, cmbRowLimit, lblClientCount, container.getBundle().getString("sidebar.item.directory").toLowerCase());
        loadDataRange(cmbDateRange.getValue());
    }

    private void setupFilters() {
        cmbDateRange.setItems(FXCollections.observableArrayList(
                container.getBundle().getString("report.client.range.today"), container.getBundle().getString("report.client.range.7d"),
                container.getBundle().getString("report.client.range.30d"), container.getBundle().getString("report.client.range.month"),
                container.getBundle().getString("report.client.range.year"), container.getBundle().getString("report.client.range.all")));
        cmbDateRange.setValue(container.getBundle().getString("report.client.range.year"));
        cmbDateRange.valueProperty().addListener((obs, o, n) -> loadDataRange(n));
        RealTimeSearchBinder.bind(txtSearch, this::filterClients);
    }

    private void setupClientTable() {
        colClientName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().clientName()));
        colClientOrders.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().count() + ""));
        colClientTotal.setCellValueFactory(c -> new SimpleStringProperty(String.format("\u20ac%.2f", c.getValue().total())));
        colClientStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) setGraphic(null);
                else {
                    ClientReportDataManager.ClientRow row = getTableRow().getItem();
                    Label status = new Label(row.isActive() ? container.getBundle().getString("status.active") : container.getBundle().getString("status.inactive"));
                    status.setStyle(row.isActive() ? "-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-padding: 2 8; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;" 
                                                   : "-fx-background-color: #f1f5f9; -fx-text-fill: #64748b; -fx-padding: 2 8; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
                    setGraphic(status); setAlignment(Pos.CENTER);
                }
            }
        });
        clientTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> { if (n != null) detailManager.show(n); });
    }

    private void setupPurchaseTable() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
        colDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSaleDateTime() != null ? c.getValue().getSaleDateTime().format(fmt) : "\u2014"));
        colItems.setCellValueFactory(c -> new SimpleStringProperty(String.format(container.getBundle().getString("report.client.units"), c.getValue().getTotalItems())));
        colMethod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPaymentMethod()));
        colAmount.setCellValueFactory(c -> new SimpleStringProperty(String.format("\u20ac%.2f", c.getValue().getTotal())));
        purchaseTable.setRowFactory(tv -> {
            TableRow<Sale> row = new TableRow<>();
            row.setOnMouseClicked(e -> { if (e.getClickCount() == 2 && !row.isEmpty()) handleViewTicket(row.getItem().getSaleId()); });
            return row;
        });
    }

    private void handleViewTicket(int saleId) {
        try {
            Sale sale = container.getSaleUseCase().getSaleDetails(saleId);
            if (sale != null) ModalService.showStandardModal("/view/receipt/view_ticket_modal.fxml", "Detalle de Ticket #" + saleId, container, (TicketDetailController ctrl) -> ctrl.setSale(sale));
        } catch (Exception e) { AlertUtil.showError(container.getBundle().getString("alert.error"), container.getBundle().getString("report.client.error.ticket")); }
    }

    private void loadDataRange(String rangeFilter) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusYears(20);
        if (rangeFilter != null) {
            if (rangeFilter.equals(container.getBundle().getString("report.client.range.today"))) start = end;
            else if (rangeFilter.equals(container.getBundle().getString("report.client.range.7d"))) start = end.minusDays(7);
            else if (rangeFilter.equals(container.getBundle().getString("report.client.range.30d"))) start = end.minusDays(30);
            else if (rangeFilter.equals(container.getBundle().getString("report.client.range.month"))) start = end.withDayOfMonth(1);
            else if (rangeFilter.equals(container.getBundle().getString("report.client.range.year"))) start = end.withDayOfYear(1);
        }
        dataManager.fetchData(start, end, rows -> {
            this.allRows = rows;
            filterClients(txtSearch.getText());
            statsManager.updateKpis(allRows, lblKpiActive, lblKpiTotal, lblKpiAvgOrder, lblKpiLtv, lblKpiActiveVar, lblKpiTotalVar, lblKpiAvgOrderVar, lblKpiLtvVar);
        }, e -> AlertUtil.showError(container.getBundle().getString("alert.error"), e.getMessage()));
    }

    private void filterClients(String query) {
        String q = query != null ? query.toLowerCase().trim() : "";
        List<ClientReportDataManager.ClientRow> filtered = allRows.stream()
                .filter(r -> q.isEmpty() || r.clientName().toLowerCase().contains(q) || r.info().toLowerCase().contains(q))
                .toList();
        paginationHelper.setData(filtered);
    }

    @FXML private void handleExport() { exportManager.exportToCsv(allRows); }
}



