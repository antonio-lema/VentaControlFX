package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.application.usecase.ClientUseCase;
import com.mycompany.ventacontrolfx.application.usecase.SaleUseCase;
import com.mycompany.ventacontrolfx.domain.model.Client;
import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.domain.model.SaleDetail;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.event.EventHandler;
import javafx.scene.input.MouseEvent;
import com.mycompany.ventacontrolfx.util.ModalService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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

        paginationHelper = new PaginationHelper<>(clientTable, cmbRowLimit, lblClientCount, "clientes");
        loadDataRange("Este año");
    }

    private void setupFilters() {
        if (cmbDateRange != null) {
            cmbDateRange.setItems(FXCollections.observableArrayList(
                    "Hoy", "7 días", "30 días", "Este mes", "Este año", "Histórico"));
            cmbDateRange.setValue("Este año");
            cmbDateRange.valueProperty().addListener((obs, o, n) -> loadDataRange(n));
        }

        if (txtSearch != null) {
            RealTimeSearchBinder.bind(txtSearch, query -> filterClients(query));
        }
    }

    private void setupClientTable() {
        colClientName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().clientName));
        colClientOrders.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().count + ""));
        colClientTotal.setCellValueFactory(c -> new SimpleStringProperty(String.format("€%.2f", c.getValue().total)));

        colClientStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    ClientRow row = getTableRow().getItem();
                    Label status = new Label(row.isActive ? "Activo" : "Inactivo");
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
                c.getValue().getSaleDateTime() != null ? c.getValue().getSaleDateTime().format(FMT_DATE) : "—"));

        colItems.setCellValueFactory(c -> {
            int items = 0;
            try {
                com.mycompany.ventacontrolfx.domain.model.Sale sale = saleUseCase
                        .getSaleDetails(c.getValue().getSaleId());
                if (sale != null && sale.getDetails() != null) {
                    items = sale.getDetails().stream()
                            .mapToInt(com.mycompany.ventacontrolfx.domain.model.SaleDetail::getQuantity).sum();
                }
            } catch (Exception e) {
            }
            return new SimpleStringProperty(items + " unds");
        });

        colMethod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPaymentMethod()));
        colAmount.setCellValueFactory(c -> new SimpleStringProperty(String.format("€%.2f", c.getValue().getTotal())));

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
            AlertUtil.showError("Error", "No se pudo cargar el detalle del ticket.");
        }
    }

    private void loadDataRange(String rangeFilter) {
        LocalDate end = LocalDate.now();
        LocalDate start = end;

        switch (rangeFilter) {
            case "Hoy":
                start = end;
                break;
            case "7 días":
                start = end.minusDays(7);
                break;
            case "30 días":
                start = end.minusDays(30);
                break;
            case "Este mes":
                start = end.withDayOfMonth(1);
                break;
            case "Este año":
                start = end.withDayOfYear(1);
                break;
            case "Histórico":
                start = end.minusYears(10);
                break;
            default:
                start = end.withDayOfYear(1);
                break;
        }

        try {
            List<Sale> allSales = saleUseCase.getSalesByRange(start, end).stream()
                    .filter(s -> !s.isReturn() && s.getClientId() != null)
                    .collect(Collectors.toList());

            List<Client> clients = clientUseCase.getAllClients();
            Map<Integer, Client> clientMap = clients.stream().collect(Collectors.toMap(Client::getId, c -> c));
            Map<Integer, List<Sale>> byClient = allSales.stream().collect(Collectors.groupingBy(Sale::getClientId));

            allRows.clear();
            for (Map.Entry<Integer, List<Sale>> entry : byClient.entrySet()) {
                Client c = clientMap.get(entry.getKey());
                String name = c != null ? c.getName() : "Cliente #" + entry.getKey();
                String info = (c != null && c.getTaxId() != null && !c.getTaxId().isEmpty()) ? "CIF: " + c.getTaxId()
                        : "Particular";

                double total = entry.getValue().stream().mapToDouble(Sale::getTotal).sum();
                int count = entry.getValue().size();

                // Active if bought in last 30 days
                boolean isActive = entry.getValue().stream().anyMatch(s -> s.getSaleDateTime() != null
                        && s.getSaleDateTime().toLocalDate().isAfter(LocalDate.now().minusDays(30)));

                // Tier logic
                String tier = "Bronce";
                String color = "#d1d5db";
                if (total > 1000) {
                    tier = "Oro";
                    color = "#fef08a";
                } else if (total > 300) {
                    tier = "Plata";
                    color = "#e2e8f0";
                }

                allRows.add(new ClientRow(entry.getKey(), name, info, total, count, isActive, tier, color,
                        entry.getValue()));
            }

            allRows.sort((a, b) -> Double.compare(b.total, a.total));
            filterClients(txtSearch != null ? txtSearch.getText() : "");

            updateKpis(allRows);

        } catch (SQLException e) {
            e.printStackTrace();
        }
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

        // Pseudo LTV: Average revenue per client
        double ltv = totalRev / rows.size();

        lblKpiActive.setText(String.valueOf(activeCount));
        lblKpiTotal.setText(String.format("€%.2f", totalRev));
        lblKpiAvgOrder.setText(String.format("€%.2f", avgOrder));
        lblKpiLtv.setText(String.format("€%.2f", ltv));

        // Mock variations
        setVariation(lblKpiActiveVar, 5.2, true);
        setVariation(lblKpiTotalVar, 12.4, true);
        setVariation(lblKpiAvgOrderVar, -1.2, false);
        setVariation(lblKpiLtvVar, 3.8, true);
    }

    private void resetKpis() {
        lblKpiActive.setText("0");
        lblKpiTotal.setText("€0.00");
        lblKpiAvgOrder.setText("€0.00");
        lblKpiLtv.setText("€0.00");
        lblKpiActiveVar.setText("-");
        lblKpiTotalVar.setText("-");
        lblKpiAvgOrderVar.setText("-");
        lblKpiLtvVar.setText("-");
    }

    private void setVariation(Label lbl, double amount, boolean positive) {
        if (lbl == null)
            return;
        lbl.setText((positive ? "↑ " : "↓ ") + Math.abs(amount) + "%");
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

        // Random background for initials
        String[] colors = { "#e0e7ff", "#dcfce7", "#fef3c7", "#f3e8ff", "#fee2e2" };
        int colorIdx = Math.abs(row.clientName.hashCode()) % colors.length;
        spDetailInitial.setStyle("-fx-background-color: " + colors[colorIdx]
                + "; -fx-background-radius: 50; -fx-min-width: 64; -fx-min-height: 64; -fx-max-width: 64; -fx-max-height: 64;");

        lblDetailTier.setText(row.tier);
        if (row.tier.equals("Oro"))
            lblDetailTier.setStyle(
                    "-fx-background-color: #fef08a; -fx-text-fill: #854d0e; -fx-padding: 2 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
        else if (row.tier.equals("Plata"))
            lblDetailTier.setStyle(
                    "-fx-background-color: #f1f5f9; -fx-text-fill: #334155; -fx-padding: 2 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
        else
            lblDetailTier.setStyle(
                    "-fx-background-color: #fed7aa; -fx-text-fill: #9a3412; -fx-padding: 2 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");

        String lastDate = row.sales.isEmpty() ? "Nunca" : row.sales.get(0).getSaleDateTime().format(FMT_DATE);
        lblDetailInfo.setText(row.info + " | Última compra: " + lastDate);

        // Populate Sales Table
        List<Sale> sortedSales = new ArrayList<>(row.sales);
        sortedSales.sort((a, b) -> b.getSaleDateTime().compareTo(a.getSaleDateTime()));
        purchaseTable.setItems(FXCollections.observableArrayList(sortedSales));

        // Graph Evo
        populateEvolutionChart(sortedSales);

        // Fav Method
        populateFavoriteMethod(sortedSales);

        // Top Products
        populateTopProducts(sortedSales);
    }

    private void populateEvolutionChart(List<Sale> sales) {
        chartEvolution.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        // Group by Month
        Map<YearMonth, Double> byMonth = new TreeMap<>();
        for (Sale s : sales) {
            if (s.getSaleDateTime() != null) {
                YearMonth ym = YearMonth.from(s.getSaleDateTime());
                byMonth.put(ym, byMonth.getOrDefault(ym, 0.0) + s.getTotal());
            }
        }

        // Ensure at least 6 months back filler
        YearMonth current = YearMonth.now().minusMonths(5);
        for (int i = 0; i < 6; i++) {
            if (!byMonth.containsKey(current))
                byMonth.put(current, 0.0);
            current = current.plusMonths(1);
        }

        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMM");
        for (Map.Entry<YearMonth, Double> entry : byMonth.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey().format(monthFmt), entry.getValue()));
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
        if (best.equalsIgnoreCase("Efectivo")) {
            iconPrefPayment.setGlyphName("MONEY");
            iconPrefPayment.setFill(javafx.scene.paint.Color.web("#16a34a"));
        } else {
            iconPrefPayment.setGlyphName("CREDIT_CARD");
            iconPrefPayment.setFill(javafx.scene.paint.Color.web("#3b82f6"));
        }
    }

    private void populateTopProducts(List<Sale> sales) {
        pnlTopProducts.getChildren().clear();
        Map<String, Integer> productQty = new HashMap<>();

        for (Sale s : sales) {
            try {
                Sale saleWithDetails = saleUseCase.getSaleDetails(s.getSaleId());
                if (saleWithDetails != null && saleWithDetails.getDetails() != null) {
                    for (SaleDetail d : saleWithDetails.getDetails()) {
                        productQty.put(d.getProductName(),
                                productQty.getOrDefault(d.getProductName(), 0) + d.getQuantity());
                    }
                }
            } catch (Exception e) {
            }
        }

        List<Map.Entry<String, Integer>> top = productQty.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(3).collect(Collectors.toList());

        if (top.isEmpty()) {
            pnlTopProducts.getChildren().add(new Label("No hay datos suficientes"));
            return;
        }

        for (Map.Entry<String, Integer> entry : top) {
            HBox box = new HBox(10);
            box.setAlignment(Pos.CENTER_LEFT);
            Label name = new Label(entry.getKey());
            name.setStyle("-fx-text-fill: #334155; -fx-font-size: 12px;");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label qty = new Label(entry.getValue() + "x");
            qty.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-font-size: 12px;");
            box.getChildren().addAll(name, spacer, qty);
            pnlTopProducts.getChildren().add(box);
        }
    }

    @FXML
    private void handleExport() {
        if (allRows.isEmpty()) {
            AlertUtil.showWarning("Sin datos", "No hay datos de clientes para exportar.");
            return;
        }

        StringBuilder csv = new StringBuilder();
        // Encabezado
        csv.append("ID;Nombre;Información;Total Facturado;Pedidos;Estado;Nivel\n");

        for (ClientRow row : allRows) {
            String totalStr = String.format(Locale.getDefault(), "%.2f", row.total);
            csv.append(row.clientId).append(";")
                    .append("\"").append(row.clientName.replace("\"", "\"\"")).append("\";")
                    .append("\"").append(row.info.replace("\"", "\"\"")).append("\";")
                    .append("\"").append(totalStr).append("\";")
                    .append(row.count).append(";")
                    .append(row.isActive ? "Activo" : "Inactivo").append(";")
                    .append(row.tier).append("\n");
        }

        try {
            String fileName = "Reporte_Clientes_" + LocalDate.now() + ".csv";
            java.io.File file = new java.io.File(System.getProperty("user.home") + "/Desktop/" + fileName);

            // Escribir con BOM para compatibilidad total con Excel en Windows
            byte[] bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
            byte[] content = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            fos.write(bom);
            fos.write(content);
            fos.close();

            AlertUtil.showInfo("Exportación Exitosa", "Se ha guardado el archivo " + fileName + " en tu escritorio.");
        } catch (Exception e) {
            AlertUtil.showError("Error al exportar", "No se pudo crear el archivo: " + e.getMessage());
        }
    }

    // ── Inner model ─────────────────────────────────────────────────────────
    public static class ClientRow {
        public final int clientId;
        public final String clientName;
        public final String info;
        public final double total;
        public final int count;
        public final boolean isActive;
        public final String tier;
        public final String color;
        public final List<Sale> sales;

        public ClientRow(int clientId, String clientName, String info, double total, int count, boolean isActive,
                String tier, String color, List<Sale> sales) {
            this.clientId = clientId;
            this.clientName = clientName;
            this.info = info;
            this.total = total;
            this.count = count;
            this.isActive = isActive;
            this.tier = tier;
            this.color = color;
            this.sales = sales;
        }
    }
}
