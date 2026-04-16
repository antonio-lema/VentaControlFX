package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.application.usecase.ClientUseCase;
import com.mycompany.ventacontrolfx.application.usecase.SaleUseCase;
import com.mycompany.ventacontrolfx.domain.model.Client;
import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import com.mycompany.ventacontrolfx.util.ModalService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import com.mycompany.ventacontrolfx.presentation.util.RealTimeSearchBinder;
import com.mycompany.ventacontrolfx.util.PaginationHelper;

public class ClientReportController implements Injectable {

    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<String> cmbDateRange;
    @FXML
    private Label lblKpiActive, lblKpiActiveVar;
    @FXML
    private Label lblKpiTotal, lblKpiTotalVar;
    @FXML
    private Label lblKpiAvgOrder, lblKpiAvgOrderVar;
    @FXML
    private Label lblKpiLtv, lblKpiLtvVar;
    @FXML
    private Label lblClientCount;
    @FXML
    private ComboBox<Integer> cmbRowLimit;

    @FXML
    private TableView<ClientRow> clientTable;
    @FXML
    private TableColumn<ClientRow, String> colClientName;
    @FXML
    private TableColumn<ClientRow, Void> colClientStatus;
    @FXML
    private TableColumn<ClientRow, String> colClientOrders;
    @FXML
    private TableColumn<ClientRow, String> colClientTotal;

    @FXML
    private VBox pnlDetailEmpty, pnlDetailContent, pnlTopProducts;
    @FXML
    private StackPane spDetailInitial;
    @FXML
    private Label lblDetailInitials, lblDetailName, lblDetailTier, lblDetailInfo;

    @FXML
    private TableView<Sale> purchaseTable;
    @FXML
    private TableColumn<Sale, String> colDate, colItems, colMethod, colAmount;

    @FXML
    private LineChart<String, Number> chartEvolution;
    @FXML
    private Label lblPrefPayment;
    @FXML
    private FontAwesomeIconView iconPrefPayment;

    private SaleUseCase saleUseCase;
    private ClientUseCase clientUseCase;
    private ServiceContainer container;
    private PaginationHelper<ClientRow> paginationHelper;
    private List<ClientRow> allRows = new ArrayList<>();
    private static final DateTimeFormatter FMT_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.saleUseCase = container.getSaleUseCase();
        this.clientUseCase = container.getClientUseCase();

        setupFilters();
        setupClientTable();
        setupPurchaseTable();

        paginationHelper = new PaginationHelper<>(clientTable, cmbRowLimit, lblClientCount,
                container.getBundle().getString("sidebar.item.directory").toLowerCase());

        // SYNC LOAD: Bypasses all async/threading to isolate root cause
        try {
            java.util.List<com.mycompany.ventacontrolfx.domain.model.ClientSaleSummary> summaries = saleUseCase
                    .getClientSalesSummary(LocalDate.now().withDayOfYear(1), LocalDate.now());
            java.util.List<com.mycompany.ventacontrolfx.domain.model.Client> clients = clientUseCase.getAllClients();
            java.util.Map<Integer, com.mycompany.ventacontrolfx.domain.model.Client> clientMap = clients.stream()
                    .collect(java.util.stream.Collectors.toMap(
                            com.mycompany.ventacontrolfx.domain.model.Client::getId, c -> c, (a, b) -> a));
            allRows.clear();
            for (com.mycompany.ventacontrolfx.domain.model.ClientSaleSummary s : summaries) {
                com.mycompany.ventacontrolfx.domain.model.Client c = clientMap.get(s.getClientId());
                String name = c != null ? c.getName() : "Cliente #" + s.getClientId();
                String info = (c != null && c.getTaxId() != null) ? "CIF: " + c.getTaxId() : "Particular";
                boolean active = s.getLastPurchase() != null &&
                        s.getLastPurchase().toLocalDate().isAfter(LocalDate.now().minusDays(30));
                allRows.add(new ClientRow(s.getClientId(), name, info,
                        s.getTotalSpent(), s.getTotalOrders(), active, "Test", "#1e88e5", s.getLastPurchase()));
            }
            paginationHelper.setData(allRows);
            updateKpis(allRows);
            System.out.println("[SYNC] Loaded " + allRows.size() + " clients.");
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("[SYNC ERROR] " + ex);
        }

        // Also fire normal async load for subsequent refreshes
        loadDataRange(container.getBundle().getString("report.client.range.year"));
    }

    private void setupFilters() {
        if (cmbDateRange != null) {
            cmbDateRange.setItems(FXCollections.observableArrayList(
                    container.getBundle().getString("report.client.range.today"),
                    container.getBundle().getString("report.client.range.7d"),
                    container.getBundle().getString("report.client.range.30d"),
                    container.getBundle().getString("report.client.range.month"),
                    container.getBundle().getString("report.client.range.year"),
                    container.getBundle().getString("report.client.range.all")));
            cmbDateRange.setValue(container.getBundle().getString("report.client.range.year"));
            cmbDateRange.valueProperty().addListener((obs, o, n) -> loadDataRange(n));
        }

        if (txtSearch != null) {
            RealTimeSearchBinder.bind(txtSearch, query -> filterClients(query));
        }
    }

    private void setupClientTable() {
        colClientName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().clientName));
        colClientOrders.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().count + ""));
        colClientTotal
                .setCellValueFactory(c -> new SimpleStringProperty(String.format("\u20ac%.2f", c.getValue().total)));

        colClientStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    ClientRow row = getTableRow().getItem();
                    Label status = new Label(row.isActive ? container.getBundle().getString("status.active")
                            : container.getBundle().getString("status.inactive"));
                    if (row.isActive) {
                        status.setStyle(
                                "-fx-background-color: #dcfce7; -fx-text-fill: #15803d; -fx-padding: 2 8; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
                    } else {
                        status.setStyle(
                                "-fx-background-color: #f1f5f9; -fx-text-fill: #64748b; -fx-padding: 2 8; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
                    }
                    setGraphic(status);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        clientTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null)
                showClientDetail(n);
        });
    }

    private void setupPurchaseTable() {
        colDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getSaleDateTime() != null ? c.getValue().getSaleDateTime().format(FMT_DATE) : "\u2014"));

        colItems.setCellValueFactory(c -> {
            int items = c.getValue().getTotalItems();
            return new SimpleStringProperty(
                    String.format(container.getBundle().getString("report.client.units"), items));
        });

        colMethod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPaymentMethod()));
        colAmount.setCellValueFactory(
                c -> new SimpleStringProperty(String.format("\u20ac%.2f", c.getValue().getTotal())));

        purchaseTable.setRowFactory(tv -> {
            TableRow<Sale> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    Sale sale = row.getItem();
                    handleViewTicket(sale.getSaleId());
                }
            });
            return row;
        });
    }

    private void handleViewTicket(int saleId) {
        try {
            Sale sale = saleUseCase.getSaleDetails(saleId);
            if (sale == null)
                return;

            ModalService.showStandardModal("/view/view_ticket_modal.fxml", "Detalle de Ticket #" + saleId, container,
                    (TicketDetailController controller) -> {
                        controller.setSale(sale);
                    });
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("report.client.error.ticket"));
        }
    }

    private void loadDataRange(String rangeFilter) {
        LocalDate end = LocalDate.now();
        LocalDate start = null;

        if (rangeFilter != null) {
            String trimmed = rangeFilter.trim();
            if (trimmed.equals(container.getBundle().getString("report.client.range.today"))) {
                start = end;
            } else if (trimmed.equals(container.getBundle().getString("report.client.range.7d"))) {
                start = end.minusDays(7);
            } else if (trimmed.equals(container.getBundle().getString("report.client.range.30d"))) {
                start = end.minusDays(30);
            } else if (trimmed.equals(container.getBundle().getString("report.client.range.month"))) {
                start = end.withDayOfMonth(1);
            } else if (trimmed.equals(container.getBundle().getString("report.client.range.year"))) {
                start = end.withDayOfYear(1);
            } else if (trimmed.equals(container.getBundle().getString("report.client.range.all"))) {
                start = end.minusYears(20);
            }
        }

        if (start == null) {
            start = end.minusYears(20); // Fallback to avoid empty results on string mismatch
        }

        final LocalDate finalStart = start;
        final LocalDate finalEnd = end;

        container.getAsyncManager().runAsyncTask(() -> {
            try {
                List<com.mycompany.ventacontrolfx.domain.model.ClientSaleSummary> summaries = saleUseCase
                        .getClientSalesSummary(finalStart, finalEnd);

                List<Client> clients = clientUseCase.getAllClients();
                // Safe mapping using merge function to avoid IllegalStateException on
                // duplicates
                Map<Integer, Client> clientMap = clients.stream()
                        .collect(Collectors.toMap(Client::getId, c -> c, (existing, replacement) -> existing));

                return new Object[] { summaries, clientMap };
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, result -> {
            Object[] data = (Object[]) result;
            @SuppressWarnings("unchecked")
            List<com.mycompany.ventacontrolfx.domain.model.ClientSaleSummary> summaries = (List<com.mycompany.ventacontrolfx.domain.model.ClientSaleSummary>) data[0];
            @SuppressWarnings("unchecked")
            Map<Integer, Client> clientMap = (Map<Integer, Client>) data[1];

            allRows.clear();
            for (com.mycompany.ventacontrolfx.domain.model.ClientSaleSummary summary : summaries) {
                Client c = clientMap.get(summary.getClientId());
                String name = c != null ? c.getName()
                        : container.getBundle().getString("sidebar.item.directory") + " #" + summary.getClientId();
                String info = (c != null && c.getTaxId() != null && !c.getTaxId().isEmpty()) ? "CIF: " + c.getTaxId()
                        : container.getBundle().getString("report.client.info.particular");

                boolean isActive = summary.getLastPurchase() != null
                        && summary.getLastPurchase().toLocalDate().isAfter(LocalDate.now().minusDays(30));

                String tier = container.getBundle().getString("report.client.tier.bronze");
                String color = "#d1d5db";
                if (summary.getTotalSpent() > 1000) {
                    tier = container.getBundle().getString("report.client.tier.diamond");
                    color = "#e2e8f0";
                } else if (summary.getTotalSpent() > 500) {
                    tier = container.getBundle().getString("report.client.tier.gold");
                    color = "#fbbf24";
                } else if (summary.getTotalSpent() > 200) {
                    tier = container.getBundle().getString("report.client.tier.silver");
                    color = "#60a5fa";
                }

                allRows.add(new ClientRow(
                        summary.getClientId(), name, info,
                        summary.getTotalSpent(), summary.getTotalOrders(),
                        isActive, tier, color, summary.getLastPurchase()));
            }

            allRows.sort((a, b) -> Double.compare(b.total, a.total));
            if (txtSearch != null && !txtSearch.getText().isEmpty()) {
                filterClients(txtSearch.getText());
            } else {
                paginationHelper.setData(allRows);
            }
            updateKpis(allRows);
        }, e -> {
            e.printStackTrace();
            String errorMsg = e.getMessage() != null ? e.getMessage() : e.toString();
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("history.error.load") + "\n\nDetalle técnico: " + errorMsg);
        });
    }

    private void filterClients(String query) {
        String q = query != null ? query.toLowerCase().trim() : "";
        List<ClientRow> filtered = allRows.stream()
                .filter(r -> q.isEmpty() || r.clientName.toLowerCase().contains(q) || r.info.toLowerCase().contains(q))
                .collect(Collectors.toList());

        paginationHelper.setData(filtered);
    }

    private void updateKpis(List<ClientRow> rows) {
        if (rows.isEmpty()) {
            resetKpis();
            return;
        }

        long activeCount = rows.stream().filter(r -> r.isActive).count();
        double totalRev = rows.stream().mapToDouble(r -> r.total).sum();
        int totalOrders = rows.stream().mapToInt(r -> r.count).sum();
        double avgOrder = totalOrders > 0 ? totalRev / totalOrders : 0;
        double ltv = totalRev / rows.size();

        lblKpiActive.setText(String.valueOf(activeCount));
        lblKpiTotal.setText(String.format("\u20ac%.2f", totalRev));
        lblKpiAvgOrder.setText(String.format("\u20ac%.2f", avgOrder));
        lblKpiLtv.setText(String.format("\u20ac%.2f", ltv));

        setVariation(lblKpiActiveVar, 5.2, true);
        setVariation(lblKpiTotalVar, 12.4, true);
        setVariation(lblKpiAvgOrderVar, -1.2, false);
        setVariation(lblKpiLtvVar, 3.8, true);
    }

    private void resetKpis() {
        lblKpiActive.setText("0");
        lblKpiTotal.setText("\u20ac0.00");
        lblKpiAvgOrder.setText("\u20ac0.00");
        lblKpiLtv.setText("\u20ac0.00");
        lblKpiActiveVar.setText("-");
        lblKpiTotalVar.setText("-");
        lblKpiAvgOrderVar.setText("-");
        lblKpiLtvVar.setText("-");
    }

    private void setVariation(Label lbl, double amount, boolean positive) {
        if (lbl == null)
            return;
        lbl.setText((positive ? "\u2191 " : "\u2193 ") + Math.abs(amount) + "%");
        if (positive) {
            lbl.setStyle(
                    "-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; -fx-padding: 2 6; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;");
        } else {
            lbl.setStyle(
                    "-fx-background-color: #fee2e2; -fx-text-fill: #dc2626; -fx-padding: 2 6; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;");
        }
    }

    private void showClientDetail(ClientRow row) {
        pnlDetailEmpty.setVisible(false);
        pnlDetailEmpty.setManaged(false);
        pnlDetailContent.setVisible(true);
        pnlDetailContent.setManaged(true);
        pnlDetailContent.setStyle("-fx-opacity: 1;");

        lblDetailName.setText(row.clientName);
        String initials = row.clientName.substring(0, Math.min(row.clientName.length(), 2)).toUpperCase();
        lblDetailInitials.setText(initials);

        String[] colors = { "#e0e7ff", "#dcfce7", "#fef3c7", "#f3e8ff", "#fee2e2" };
        int colorIdx = Math.abs(row.clientName.hashCode()) % colors.length;
        spDetailInitial.setStyle("-fx-background-color: " + colors[colorIdx]
                + "; -fx-background-radius: 50; -fx-min-width: 64; -fx-min-height: 64; -fx-max-width: 64; -fx-max-height: 64;");

        lblDetailTier.setText(row.tier);
        if (row.tier.equals(container.getBundle().getString("report.client.tier.gold")))
            lblDetailTier.setStyle(
                    "-fx-background-color: #fef08a; -fx-text-fill: #854d0e; -fx-padding: 2 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
        else if (row.tier.equals(container.getBundle().getString("report.client.tier.silver")))
            lblDetailTier.setStyle(
                    "-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-padding: 2 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
        else
            lblDetailTier.setStyle(
                    "-fx-background-color: #fed7aa; -fx-text-fill: #9a3412; -fx-padding: 2 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");

        String lastDateStr = row.lastPurchase == null ? container.getBundle().getString("report.client.never")
                : row.lastPurchase.format(FMT_DATE);
        lblDetailInfo.setText(row.info + " | "
                + String.format(container.getBundle().getString("report.client.last_purchase"), lastDateStr));

        // ASYNC LOAD: Launch in background thread to avoid freezing UI
        purchaseTable.setItems(FXCollections.observableArrayList()); // clear first
        purchaseTable.setPlaceholder(new Label("Cargando ventas..."));

        final int clientId = row.clientId;
        new Thread(() -> {
            try {
                // Limit to 200 most recent sales to avoid loading tens of thousands of rows
                List<Sale> clientSales = saleUseCase.getSalesByClient(clientId);
                List<Sale> limited = clientSales.size() > 200
                        ? clientSales.subList(0, 200)
                        : clientSales;

                javafx.application.Platform.runLater(() -> {
                    purchaseTable.setItems(FXCollections.observableArrayList(limited));
                    purchaseTable.setPlaceholder(new Label("No hay ventas registradas"));
                    populateEvolutionChart(limited);
                    populateFavoriteMethod(limited);
                    populateTopProducts(limited);
                });
            } catch (SQLException e) {
                e.printStackTrace();
                javafx.application.Platform
                        .runLater(() -> purchaseTable.setPlaceholder(new Label("Error al cargar ventas")));
            }
        }, "ClientDetail-Loader").start();
    }

    private void populateEvolutionChart(List<Sale> sales) {
        chartEvolution.getData().clear();
        CategoryAxis xAxis = (CategoryAxis) chartEvolution.getXAxis();
        if (xAxis != null) {
            xAxis.getCategories().clear();
        }

        if (sales.isEmpty())
            return;

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        String filter = cmbDateRange != null ? cmbDateRange.getValue() : "Este a\u00f1o";
        boolean groupByDay = filter.contains("d\u00eda") || filter.contains("Hoy") || filter.contains("mes");

        if (groupByDay) {
            Map<LocalDate, Double> byDay = new TreeMap<>();
            for (Sale s : sales) {
                if (s.getSaleDateTime() != null) {
                    LocalDate date = s.getSaleDateTime().toLocalDate();
                    byDay.put(date, byDay.getOrDefault(date, 0.0) + s.getTotal());
                }
            }

            LocalDate end = LocalDate.now();
            LocalDate start = filter.contains("7") ? end.minusDays(7) : end.minusDays(30);

            Optional<LocalDate> firstSale = byDay.keySet().stream().findFirst();
            if (firstSale.isPresent() && firstSale.get().isBefore(start)) {
                start = firstSale.get();
            }

            for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
                if (!byDay.containsKey(d))
                    byDay.put(d, 0.0);
            }

            DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("dd MMM");
            List<String> categories = new ArrayList<>();
            for (Map.Entry<LocalDate, Double> entry : byDay.entrySet()) {
                String label = entry.getKey().format(dayFmt);
                categories.add(label);
                series.getData().add(new XYChart.Data<>(label, entry.getValue()));
            }

            if (xAxis != null) {
                xAxis.setCategories(FXCollections.observableArrayList(categories));
            }

        } else {
            Map<YearMonth, Double> byMonth = new TreeMap<>();
            for (Sale s : sales) {
                if (s.getSaleDateTime() != null) {
                    YearMonth ym = YearMonth.from(s.getSaleDateTime());
                    byMonth.put(ym, byMonth.getOrDefault(ym, 0.0) + s.getTotal());
                }
            }

            YearMonth current = YearMonth.now().minusMonths(5);
            for (int i = 0; i < 6; i++) {
                if (!byMonth.containsKey(current))
                    byMonth.put(current, 0.0);
                current = current.plusMonths(1);
            }

            DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMM yy");
            List<String> categories = new ArrayList<>();
            for (Map.Entry<YearMonth, Double> entry : byMonth.entrySet()) {
                String label = entry.getKey().format(monthFmt);
                categories.add(label);
                series.getData().add(new XYChart.Data<>(label, entry.getValue()));
            }

            if (xAxis != null) {
                xAxis.setCategories(FXCollections.observableArrayList(categories));
            }
        }

        chartEvolution.getData().add(series);
    }

    private void populateFavoriteMethod(List<Sale> sales) {
        if (sales.isEmpty())
            return;
        Map<String, Long> methodCount = sales.stream().map(Sale::getPaymentMethod).filter(Objects::nonNull)
                .collect(Collectors.groupingBy(m -> m, Collectors.counting()));

        String best = "Efectivo";
        long max = 0;
        for (Map.Entry<String, Long> entry : methodCount.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                best = entry.getKey();
            }
        }

        lblPrefPayment.setText(best);
        if (best.equalsIgnoreCase("Efectivo") || best.equalsIgnoreCase("Cash")) {
            iconPrefPayment.setIcon(FontAwesomeIcon.MONEY);
            iconPrefPayment.setFill(javafx.scene.paint.Color.web("#16a34a"));
        } else {
            iconPrefPayment.setIcon(FontAwesomeIcon.CREDIT_CARD);
            iconPrefPayment.setFill(javafx.scene.paint.Color.web("#3b82f6"));
        }
    }

    private void populateTopProducts(List<Sale> sales) {
        pnlTopProducts.getChildren().clear();
        if (sales.isEmpty()) {
            pnlTopProducts.getChildren().add(new Label(container.getBundle().getString("report.client.no_data")));
            return;
        }

        try {
            int clientId = sales.get(0).getClientId();
            List<com.mycompany.ventacontrolfx.domain.model.ProductSummary> top = saleUseCase.getTopProductsByClient(
                    clientId, 5);

            if (top.isEmpty()) {
                pnlTopProducts.getChildren().add(new Label(container.getBundle().getString("report.client.no_data")));
                return;
            }

            for (com.mycompany.ventacontrolfx.domain.model.ProductSummary entry : top) {
                HBox box = new HBox(10);
                box.setAlignment(Pos.CENTER_LEFT);
                Label name = new Label(entry.getName());
                name.setStyle("-fx-text-fill: #334155; -fx-font-size: 12px;");
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                Label qty = new Label(entry.getQuantity() + "x");
                qty.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-font-size: 12px;");
                box.getChildren().addAll(name, spacer, qty);
                pnlTopProducts.getChildren().add(box);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleExport() {
        if (allRows.isEmpty()) {
            AlertUtil.showWarning(container.getBundle().getString("alert.warning"),
                    container.getBundle().getString("report.client.no_data"));
            return;
        }

        StringBuilder csv = new StringBuilder();
        csv.append("ID;Nombre;Informaci\u00f3n;Total Facturado;Pedidos;Estado;Nivel\n");

        for (ClientRow row : allRows) {
            String totalStr = String.format(Locale.getDefault(), "%.2f", row.total);
            csv.append(row.clientId).append(";")
                    .append("\"").append(row.clientName.replace("\"", "\"\"")).append("\";")
                    .append("\"").append(row.info.replace("\"", "\"\"")).append("\";")
                    .append("\"").append(totalStr).append("\";")
                    .append(row.count).append(";")
                    .append(row.isActive ? "Activo" : "Inactivo")
                    .append(";")
                    .append(row.tier).append("\n");
        }

        try {
            String fileName = "Reporte_Clientes_" + LocalDate.now() + ".csv";
            java.io.File file = new java.io.File(System.getProperty("user.home") + "/Desktop/" + fileName);
            byte[] bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
            byte[] content = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            fos.write(bom);
            fos.write(content);
            fos.close();
            AlertUtil.showInfo("Exportaci\u00f3n exitosa",
                    "El archivo " + fileName + " se guard\u00f3 en el escritorio.");
        } catch (Exception e) {
            AlertUtil.showError("Error", "No se pudo exportar: " + e.getMessage());
        }
    }

    public static class ClientRow {
        public final int clientId;
        public final String clientName;
        public final String info;
        public final double total;
        public final int count;
        public final boolean isActive;
        public final String tier;
        public final String color;
        public final java.time.LocalDateTime lastPurchase;

        public ClientRow(int clientId, String clientName, String info, double total, int count, boolean isActive,
                String tier, String color, java.time.LocalDateTime lastPurchase) {
            this.clientId = clientId;
            this.clientName = clientName;
            this.info = info;
            this.total = total;
            this.count = count;
            this.isActive = isActive;
            this.tier = tier;
            this.color = color;
            this.lastPurchase = lastPurchase;
        }

        public int getClientId() {
            return clientId;
        }

        public String getClientName() {
            return clientName;
        }

        public String getInfo() {
            return info;
        }

        public double getTotal() {
            return total;
        }

        public int getCount() {
            return count;
        }

        public boolean isIsActive() {
            return isActive;
        }

        public String getTier() {
            return tier;
        }

        public String getColor() {
            return color;
        }

        public java.time.LocalDateTime getLastPurchase() {
            return lastPurchase;
        }
    }
}
