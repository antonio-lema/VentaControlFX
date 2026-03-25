package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.application.usecase.SaleUseCase;
import com.mycompany.ventacontrolfx.application.usecase.UserUseCase;
import com.mycompany.ventacontrolfx.domain.dto.SellerAnalytics;
import com.mycompany.ventacontrolfx.domain.model.Return;
import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.domain.model.User;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;
import com.mycompany.ventacontrolfx.util.DateFilterUtils;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class SellerReportController implements Injectable {

    @FXML
    private DatePicker dpFrom, dpTo;
    @FXML
    private HBox quickFilterContainer;
    @FXML
    private ComboBox<String> cbPaymentMethod;

    @FXML
    private Label lblKpiTotal, lblKpiCount, lblKpiBest, lblKpiAvg, lblKpiMargin, lblGoalPct, lblGoalText,
            lblReturnsTotal;
    @FXML
    private Label lblTrendTotal, lblTrendCount, lblTrendAvg, lblTrendMargin, lblBestPerformance;

    @FXML
    private PieChart pieChartDistribution;
    @FXML
    private BarChart<String, Number> salesBarChart;
    @FXML
    private LineChart<String, Number> trendLineChart;

    @FXML
    private TableView<SellerAnalytics> sellerTable;
    @FXML
    private TableColumn<SellerAnalytics, String> colRank, colAvatar, colSeller, colTotal, colAvgTicket, colParticip,
            colGoal, colReturns;

    @FXML
    private Label lblCashTotal, lblCardTotal;
    @FXML
    private ProgressBar progressGoal, progressCash, progressCard;

    @FXML
    private Label lblDetailTitle;
    @FXML
    private TextField txtDetailSearch;
    @FXML
    private TableView<Sale> detailTable;
    @FXML
    private TableColumn<Sale, String> colDate, colRecordType, colMethod, colClient, colAmount;

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

        DateFilterUtils.addQuickFilters(quickFilterContainer, dpFrom, dpTo, this::loadData);

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
        colAvatar.setCellValueFactory(c -> new SimpleStringProperty("🧑‍💼"));
        colSeller.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getSellerName()));
        colTotal.setCellValueFactory(
                c -> new SimpleStringProperty(String.format("%.2f €", c.getValue().getTotalSales())));
        colAvgTicket.setCellValueFactory(
                c -> new SimpleStringProperty(String.format("%.2f €", c.getValue().getAverageTicket())));
        colParticip.setCellValueFactory(c -> new SimpleStringProperty(
                String.format("%.1f%%", c.getValue().getParticipationPercentage() * 100)));
        colGoal.setCellValueFactory(
                c -> new SimpleStringProperty(
                        String.format("%.1f%% (Obj 3000€)", c.getValue().getGoalReachedPercentage() * 100)));
        colReturns.setCellValueFactory(
                c -> new SimpleStringProperty(String.format("%.2f €", c.getValue().getReturnsTotal())));

        sellerTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null)
                showSellerDetail(newVal);
        });

        // Detail Table
        colDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getSaleDateTime() != null ? c.getValue().getSaleDateTime().format(FMT) : "—"));
        colRecordType
                .setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isReturn() ? "Devolución" : "Venta"));
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

        try {
            LocalDate searchFrom = (from == null) ? LocalDate.of(2000, 1, 1) : from;
            LocalDate searchTo = (to == null) ? LocalDate.of(2100, 1, 1) : to;

            // 1. Cargar Ventas del periodo actual
            List<Sale> currentSales = saleUseCase.getSalesByRange(searchFrom, searchTo).stream()
                    .filter(s -> methodFilter.equals("Todos") || s.getPaymentMethod().equalsIgnoreCase(methodFilter))
                    .collect(Collectors.toList());

            // 1b. Cargar Devoluciones del periodo actual
            List<Return> currentReturnsList = saleUseCase.getReturnsHistory(searchFrom, searchTo).stream()
                    .filter(r -> methodFilter.equals("Todos") || r.getPaymentMethod().equalsIgnoreCase(methodFilter))
                    .collect(Collectors.toList());

            // 2. Trend (Periodo anterior) - Solo si hay fechas seleccionadas
            List<Sale> prevSales = new ArrayList<>();
            List<Return> prevReturns = new ArrayList<>();
            if (from != null && to != null) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
                LocalDate prevFrom = from.minusDays(days);
                LocalDate prevTo = from.minusDays(1);
                prevSales = saleUseCase.getSalesByRange(prevFrom, prevTo);
                prevReturns = saleUseCase.getReturnsHistory(prevFrom, prevTo);
            }

            processAnalytics(currentSales, currentReturnsList, prevSales, prevReturns);

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error de BD", "No se pudieron cargar los datos: " + e.getMessage());
        }
    }

    private void processAnalytics(List<Sale> currentSales, List<Return> currentReturnsList, List<Sale> prevSales,
            List<Return> prevReturns)
            throws SQLException {
        double totalSalesGross = currentSales.stream().mapToDouble(Sale::getTotal).sum();
        double totalReturnsAmount = currentReturnsList.stream().mapToDouble(Return::getTotalRefunded).sum();
        double totalCurrentNet = totalSalesGross - totalReturnsAmount;

        double totalPrevGross = prevSales.stream().mapToDouble(Sale::getTotal).sum();
        double totalPrevReturnsAmount = prevReturns.stream().mapToDouble(Return::getTotalRefunded).sum();
        double totalPrev = totalPrevGross - totalPrevReturnsAmount;

        // Asume margen del 35%
        double marginCurrent = totalCurrentNet * 0.35;
        double marginPrev = totalPrev * 0.35;

        // KPIs
        lblKpiTotal.setText(String.format("%.2f €", totalCurrentNet));
        lblKpiMargin.setText(String.format("%.2f €", marginCurrent));
        lblKpiCount.setText(String.valueOf(currentSales.size()));
        lblKpiAvg.setText(
                currentSales.isEmpty() ? "0.00 €" : String.format("%.2f €", totalCurrentNet / currentSales.size()));

        // Devoluciones Label
        lblReturnsTotal.setText(String.format("-%.2f €", totalReturnsAmount));

        // Goal Tracking (Supongamos un objetivo global de 15,000 para todo el equipo)
        double teamGoal = 15000.0;
        double goalPct = totalCurrentNet / teamGoal;
        lblGoalPct.setText(String.format("%.1f%%", goalPct * 100));
        progressGoal.setProgress(Math.min(goalPct, 1.0));
        lblGoalText.setText(String.format("%.0f / %.0f €", totalCurrentNet, teamGoal));

        // Trends
        updateTrendLabel(lblTrendTotal, totalCurrentNet, totalPrev, true);
        updateTrendLabel(lblTrendMargin, marginCurrent, marginPrev, true);
        double avgCurrent = currentSales.isEmpty() ? 0 : totalCurrentNet / currentSales.size();
        double avgPrev = prevSales.isEmpty() ? 0 : totalPrev / prevSales.size();
        updateTrendLabel(lblTrendAvg, avgCurrent, avgPrev, true);
        updateTrendLabel(lblTrendCount, (double) currentSales.size(), (double) prevSales.size(), false);

        // Agrupar por Vendedor
        Map<Integer, List<Sale>> byUser = currentSales.stream().collect(Collectors.groupingBy(Sale::getUserId));
        Map<Integer, List<Return>> returnsByUser = currentReturnsList.stream()
                .collect(Collectors.groupingBy(Return::getUserId));

        List<User> allUsers = userUseCase.getAllUsers();
        Map<Integer, User> userMap = allUsers.stream().collect(Collectors.toMap(User::getUserId, u -> u));

        List<SellerAnalytics> analyticsList = new ArrayList<>();
        XYChart.Series<String, Number> seriesBar = new XYChart.Series<>(); // Para de gráfico de barras
        XYChart.Series<String, Number> seriesLine = new XYChart.Series<>(); // Evolución temporal

        Set<Integer> allUserIdsInPeriod = new HashSet<>(byUser.keySet());
        allUserIdsInPeriod.addAll(returnsByUser.keySet());

        for (Integer userId : allUserIdsInPeriod) {
            User user = userMap.get(userId);
            String name = (user != null) ? user.getFullName() : "Usuario #" + userId;

            SellerAnalytics sa = new SellerAnalytics(userId, name);
            List<Sale> sellerSales = byUser.getOrDefault(userId, new ArrayList<>());
            List<Return> sellerReturns = returnsByUser.getOrDefault(userId, new ArrayList<>());

            double sellerGross = sellerSales.stream().mapToDouble(Sale::getTotal).sum();
            double sellerRetSum = sellerReturns.stream().mapToDouble(Return::getTotalRefunded).sum();
            double sellerNet = sellerGross - sellerRetSum;

            sa.setTotalSales(sellerNet);
            sa.setReturnsTotal(sellerRetSum);
            sa.setTransactionCount(sellerSales.size());
            sa.setAverageTicket(sellerSales.isEmpty() ? 0 : sellerNet / sellerSales.size());
            sa.setParticipationPercentage(totalCurrentNet > 0 ? sellerNet / totalCurrentNet : 0);
            sa.setSales(sellerSales);

            // Asume un objetivo de 3000 por vendedor
            sa.setGoalReachedPercentage(sellerNet / 3000.0);

            // Breakdown pagos (Neto por vendedor)
            double sCash = sellerSales.stream().filter(s -> "Efectivo".equalsIgnoreCase(s.getPaymentMethod()))
                    .mapToDouble(Sale::getTotal).sum() -
                    sellerReturns.stream().filter(r -> "Efectivo".equalsIgnoreCase(r.getPaymentMethod()))
                            .mapToDouble(Return::getTotalRefunded).sum();

            double sCard = sellerSales.stream().filter(s -> "Tarjeta".equalsIgnoreCase(s.getPaymentMethod()))
                    .mapToDouble(Sale::getTotal).sum() -
                    sellerReturns.stream().filter(r -> "Tarjeta".equalsIgnoreCase(r.getPaymentMethod()))
                            .mapToDouble(Return::getTotalRefunded).sum();

            sa.setCashTotal(sCash);
            sa.setCardTotal(sCard);

            analyticsList.add(sa);

            // Agregar al gráfico de barras (Ventas Netas)
            seriesBar.getData().add(new XYChart.Data<>(name, sellerNet));
        }

        analyticsList.sort((a, b) -> Double.compare(b.getTotalSales(), a.getTotalSales()));
        sellerTable.setItems(FXCollections.observableArrayList(analyticsList));

        // Evolución LineChart por día (Ventas Netas)
        Map<LocalDate, Double> dailySalesNet = currentSales.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getSaleDateTime().toLocalDate(),
                        TreeMap::new,
                        Collectors.summingDouble(Sale::getTotal)));

        // Restar devoluciones por día
        currentReturnsList.forEach(r -> {
            LocalDate date = r.getReturnDatetime().toLocalDate();
            dailySalesNet.put(date, dailySalesNet.getOrDefault(date, 0.0) - r.getTotalRefunded());
        });

        double acum = 0;
        for (Map.Entry<LocalDate, Double> entry : dailySalesNet.entrySet()) {
            acum += entry.getValue();
            seriesLine.getData()
                    .add(new XYChart.Data<>(entry.getKey().format(DateTimeFormatter.ofPattern("dd/MM")), acum));
        }

        salesBarChart.getData().setAll(seriesBar);
        trendLineChart.getData().setAll(seriesLine);

        if (!analyticsList.isEmpty()) {
            SellerAnalytics best = analyticsList.get(0);
            lblKpiBest.setText(best.getSellerName());
            lblBestPerformance.setText(String.format("%.2f € / Mes", best.getTotalSales()));
        } else {
            lblKpiBest.setText("—");
            lblBestPerformance.setText("0.00 € / Mes");
        }

        // Gráfico Pie: Distribución por Pagos (Global)
        pieChartDistribution.setLabelsVisible(false);
        pieChartDistribution.setLegendVisible(false);
        double globalCash = currentSales.stream().filter(s -> "Efectivo".equalsIgnoreCase(s.getPaymentMethod()))
                .mapToDouble(Sale::getTotal).sum() -
                currentReturnsList.stream().filter(r -> "Efectivo".equalsIgnoreCase(r.getPaymentMethod()))
                        .mapToDouble(Return::getTotalRefunded).sum();

        double globalCard = currentSales.stream().filter(s -> "Tarjeta".equalsIgnoreCase(s.getPaymentMethod()))
                .mapToDouble(Sale::getTotal).sum() -
                currentReturnsList.stream().filter(r -> "Tarjeta".equalsIgnoreCase(r.getPaymentMethod()))
                        .mapToDouble(Return::getTotalRefunded).sum();

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList(
                new PieChart.Data("Efectivo", Math.max(0, globalCash)),
                new PieChart.Data("Tarjeta", Math.max(0, globalCard)));
        pieChartDistribution.setData(pieData);

        // Resumen Global de Pagos Labels
        lblCashTotal.setText(String.format("%.2f €", globalCash));
        lblCardTotal.setText(String.format("%.2f €", globalCard));

        double totalPay = globalCash + globalCard;
        progressCash.setProgress(totalPay > 0 ? globalCash / totalPay : 0);
        progressCard.setProgress(totalPay > 0 ? globalCard / totalPay : 0);
    }

    private void updateTrendLabel(Label lbl, double current, double prev, boolean isCurrency) {
        lbl.getStyleClass().removeAll("trend-badge-up", "trend-badge-down", "trend-badge-neutral", "trend-badge-blue");

        if (prev == 0) {
            lbl.setText(isCurrency ? "↑ 100%" : "↑ " + (int) current);
            lbl.getStyleClass().add("trend-badge-up");
            return;
        }

        double diff = current - prev;
        double pct = (diff / prev) * 100;

        String arrow = pct > 0.1 ? "↑ " : (pct < -0.1 ? "↓ " : "• ");

        if (isCurrency) {
            lbl.setText(String.format("%s%.1f%%", arrow, Math.abs(pct)));
        } else {
            lbl.setText(String.format("%s%d", arrow, (int) Math.abs(diff)));
        }

        if (pct > 0.1)
            lbl.getStyleClass().add("trend-badge-up");
        else if (pct < -0.1)
            lbl.getStyleClass().add("trend-badge-down");
        else
            lbl.getStyleClass().add("trend-badge-neutral");
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
