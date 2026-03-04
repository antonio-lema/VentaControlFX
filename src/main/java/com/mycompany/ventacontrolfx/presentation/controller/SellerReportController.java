package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.application.usecase.SaleUseCase;
import com.mycompany.ventacontrolfx.application.usecase.UserUseCase;
import com.mycompany.ventacontrolfx.domain.dto.SellerAnalytics;
import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.domain.model.User;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class SellerReportController implements Injectable {

    @FXML
    private DatePicker dpFrom, dpTo;
    @FXML
    private ComboBox<String> cbPaymentMethod;
    @FXML
    private CheckBox chkIncludeReturns;

    @FXML
    private Label lblKpiTotal, lblKpiCount, lblKpiBest, lblKpiAvg;
    @FXML
    private Label lblTrendTotal, lblTrendCount, lblBestPerformance;

    @FXML
    private PieChart pieChartDistribution;

    @FXML
    private TableView<SellerAnalytics> sellerTable;
    @FXML
    private TableColumn<SellerAnalytics, String> colRank, colSeller, colTotal, colAvgTicket, colParticip;

    @FXML
    private Label lblCashTotal, lblCardTotal;
    @FXML
    private ProgressBar progressCash, progressCard;

    @FXML
    private Label lblDetailTitle;
    @FXML
    private TextField txtDetailSearch;
    @FXML
    private TableView<Sale> detailTable;
    @FXML
    private TableColumn<Sale, String> colDate, colMethod, colClient, colAmount;

    private SaleUseCase saleUseCase;
    private UserUseCase userUseCase;
    private ServiceContainer container;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private ObservableList<Sale> currentDetailSales = FXCollections.observableArrayList();

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.saleUseCase = container.getSaleUseCase();
        this.userUseCase = container.getUserUseCase();

        setupTables();
        setupFilters();

        // Rango por defecto: mes actual
        dpFrom.setValue(LocalDate.now().withDayOfMonth(1));
        dpTo.setValue(LocalDate.now());

        loadData();
    }

    private void setupFilters() {
        cbPaymentMethod.setItems(FXCollections.observableArrayList("Todos", "Efectivo", "Tarjeta"));
        cbPaymentMethod.setValue("Todos");

        txtDetailSearch.textProperty().addListener((obs, oldVal, newVal) -> {
            filterDetailTable(newVal);
        });
    }

    private void setupTables() {
        // Seller Table
        colRank.setCellValueFactory(
                c -> new SimpleStringProperty(String.valueOf(sellerTable.getItems().indexOf(c.getValue()) + 1)));
        colSeller.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSellerName()));
        colTotal.setCellValueFactory(
                c -> new SimpleStringProperty(String.format("%.2f €", c.getValue().getTotalSales())));
        colAvgTicket.setCellValueFactory(
                c -> new SimpleStringProperty(String.format("%.2f €", c.getValue().getAverageTicket())));
        colParticip.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("%.1f%%", c.getValue().getParticipationPercentage() * 100)));

        sellerTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null)
                showSellerDetail(newVal);
        });

        // Detail Table
        colDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getSaleDateTime() != null ? c.getValue().getSaleDateTime().format(FMT) : "—"));
        colMethod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPaymentMethod()));
        colClient.setCellValueFactory(c -> {
            Integer cid = c.getValue().getClientId();
            if (cid == null)
                return new SimpleStringProperty("Venta General");
            try {
                var client = container.getClientUseCase().getById(cid);
                return new SimpleStringProperty(client != null ? client.getName() : "Cliente #" + cid);
            } catch (Exception e) {
                return new SimpleStringProperty("Cliente #" + cid);
            }
        });
        colAmount.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f €", c.getValue().getTotal())));
    }

    @FXML
    private void handleFilter() {
        loadData();
    }

    private void loadData() {
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();
        String methodFilter = cbPaymentMethod.getValue();
        boolean includeReturns = chkIncludeReturns.isSelected();

        try {
            // 1. Cargar Ventas del periodo actual
            List<Sale> currentSales = saleUseCase.getSalesByRange(from, to).stream()
                    .filter(s -> includeReturns || !s.isReturn())
                    .filter(s -> methodFilter.equals("Todos") || s.getPaymentMethod().equalsIgnoreCase(methodFilter))
                    .collect(Collectors.toList());

            // 2. Cargar Ventas del periodo anterior para comparativa (Trend)
            long days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
            LocalDate prevFrom = from.minusDays(days);
            LocalDate prevTo = from.minusDays(1);
            List<Sale> prevSales = saleUseCase.getSalesByRange(prevFrom, prevTo).stream()
                    .filter(s -> includeReturns || !s.isReturn())
                    .collect(Collectors.toList());

            processAnalytics(currentSales, prevSales);

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error de BD", "No se pudieron cargar los datos: " + e.getMessage());
        }
    }

    private void processAnalytics(List<Sale> currentSales, List<Sale> prevSales) throws SQLException {
        double totalCurrent = currentSales.stream().mapToDouble(Sale::getTotal).sum();
        double totalPrev = prevSales.stream().mapToDouble(Sale::getTotal).sum();

        // KPIs
        lblKpiTotal.setText(String.format("%.2f €", totalCurrent));
        lblKpiCount.setText(String.valueOf(currentSales.size()));
        lblKpiAvg.setText(
                currentSales.isEmpty() ? "0.00 €" : String.format("%.2f €", totalCurrent / currentSales.size()));

        // Trends
        updateTrendLabel(lblTrendTotal, totalCurrent, totalPrev, true);
        updateTrendLabel(lblTrendCount, (double) currentSales.size(), (double) prevSales.size(), false);

        // Agrupar por Vendedor
        Map<Integer, List<Sale>> byUser = currentSales.stream().collect(Collectors.groupingBy(Sale::getUserId));
        List<User> allUsers = userUseCase.getAllUsers();
        Map<Integer, User> userMap = allUsers.stream().collect(Collectors.toMap(User::getUserId, u -> u));

        List<SellerAnalytics> analyticsList = new ArrayList<>();
        for (Map.Entry<Integer, List<Sale>> entry : byUser.entrySet()) {
            User user = userMap.get(entry.getKey());
            String name = (user != null) ? user.getFullName() : "Usuario #" + entry.getKey();

            SellerAnalytics sa = new SellerAnalytics(entry.getKey(), name);
            List<Sale> sellerSales = entry.getValue();
            double sellerTotal = sellerSales.stream().mapToDouble(Sale::getTotal).sum();

            sa.setTotalSales(sellerTotal);
            sa.setTransactionCount(sellerSales.size());
            sa.setAverageTicket(sellerSales.isEmpty() ? 0 : sellerTotal / sellerSales.size());
            sa.setParticipationPercentage(totalCurrent > 0 ? sellerTotal / totalCurrent : 0);
            sa.setSales(sellerSales);

            // Breakdown pagos
            sa.setCashTotal(sellerSales.stream().filter(s -> "Efectivo".equalsIgnoreCase(s.getPaymentMethod()))
                    .mapToDouble(Sale::getTotal).sum());
            sa.setCardTotal(sellerSales.stream().filter(s -> "Tarjeta".equalsIgnoreCase(s.getPaymentMethod()))
                    .mapToDouble(Sale::getTotal).sum());

            analyticsList.add(sa);
        }

        analyticsList.sort((a, b) -> Double.compare(b.getTotalSales(), a.getTotalSales()));
        sellerTable.setItems(FXCollections.observableArrayList(analyticsList));

        if (!analyticsList.isEmpty()) {
            SellerAnalytics best = analyticsList.get(0);
            lblKpiBest.setText(best.getSellerName());
            lblBestPerformance.setText(String.format("%.2fe vendidos", best.getTotalSales()));
        } else {
            lblKpiBest.setText("—");
            lblBestPerformance.setText("0.00 € vendidos");
        }

        // Gráfico Pie
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (SellerAnalytics sa : analyticsList) {
            pieData.add(new PieChart.Data(sa.getSellerName(), sa.getTotalSales()));
        }
        pieChartDistribution.setData(pieData);

        // Resumen Global de Pagos
        double globalCash = currentSales.stream().filter(s -> "Efectivo".equalsIgnoreCase(s.getPaymentMethod()))
                .mapToDouble(Sale::getTotal).sum();
        double globalCard = currentSales.stream().filter(s -> "Tarjeta".equalsIgnoreCase(s.getPaymentMethod()))
                .mapToDouble(Sale::getTotal).sum();
        lblCashTotal.setText(String.format("%.2f €", globalCash));
        lblCardTotal.setText(String.format("%.2f €", globalCard));

        double totalPay = globalCash + globalCard;
        progressCash.setProgress(totalPay > 0 ? globalCash / totalPay : 0);
        progressCard.setProgress(totalPay > 0 ? globalCard / totalPay : 0);
    }

    private void updateTrendLabel(Label lbl, double current, double prev, boolean isCurrency) {
        if (prev == 0) {
            lbl.setText(isCurrency ? "+100%" : "+" + (int) current);
            lbl.getStyleClass().removeAll("trend-up", "trend-down", "trend-neutral");
            lbl.getStyleClass().add("trend-up");
            return;
        }
        double diff = current - prev;
        double pct = (diff / prev) * 100;

        if (isCurrency) {
            lbl.setText(String.format("%s%.1f%%", pct >= 0 ? "+" : "", pct));
        } else {
            lbl.setText(String.format("%s%d", diff >= 0 ? "+" : "", (int) diff));
        }

        lbl.getStyleClass().removeAll("trend-up", "trend-down", "trend-neutral");
        if (pct > 0.1)
            lbl.getStyleClass().add("trend-up");
        else if (pct < -0.1)
            lbl.getStyleClass().add("trend-down");
        else
            lbl.getStyleClass().add("trend-neutral");
    }

    private void showSellerDetail(SellerAnalytics sa) {
        lblDetailTitle.setText("📋 Ventas de " + sa.getSellerName());
        currentDetailSales.setAll(sa.getSales());
        filterDetailTable(txtDetailSearch.getText());
    }

    private void filterDetailTable(String query) {
        if (query == null || query.isEmpty()) {
            detailTable.setItems(currentDetailSales);
            return;
        }
        String lower = query.toLowerCase();
        List<Sale> filtered = currentDetailSales.stream()
                .filter(s -> s.getPaymentMethod().toLowerCase().contains(lower) ||
                        String.valueOf(s.getTotal()).contains(lower))
                .collect(Collectors.toList());
        detailTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    private void handleExportExcel() {
        showInfo("Exportación", "Módulo Premium: Generando Excel...");
    }

    @FXML
    private void handleExportPDF() {
        showInfo("Exportación", "Módulo Premium: Generando PDF...");
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
