package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.application.usecase.ClientUseCase;
import com.mycompany.ventacontrolfx.application.usecase.SaleUseCase;
import com.mycompany.ventacontrolfx.application.usecase.UserUseCase;
import com.mycompany.ventacontrolfx.domain.model.Client;
import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.domain.model.User;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ClientReportController implements Injectable {

    @FXML
    private DatePicker dpFrom, dpTo;
    @FXML
    private TextField txtSearch;
    @FXML
    private TableView<ClientRow> clientTable;
    @FXML
    private TableColumn<ClientRow, String> colClient, colTotal, colOrders;
    @FXML
    private TableView<Sale> purchaseTable;
    @FXML
    private TableColumn<Sale, String> colDate, colSeller, colMethod, colItems, colAmount;
    @FXML
    private Label lblDetailTitle, lblClientInfo;
    @FXML
    private Label lblKpiClients, lblKpiTotal, lblKpiBest, lblKpiAvg;

    private SaleUseCase saleUseCase;
    private ClientUseCase clientUseCase;
    private UserUseCase userUseCase;
    private ServiceContainer container;
    private List<ClientRow> allRows = new ArrayList<>();

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.saleUseCase = container.getSaleUseCase();
        this.clientUseCase = container.getClientUseCase();
        this.userUseCase = container.getUserUseCase();
        setupClientTable();
        setupPurchaseTable();
        // Búsqueda en tiempo real
        if (txtSearch != null) {
            txtSearch.textProperty().addListener((obs, o, n) -> filterClients(n));
        }
        // Rango por defecto: mes actual
        dpFrom.setValue(LocalDate.now().withDayOfMonth(1));
        dpTo.setValue(LocalDate.now());
        loadData();
    }

    private void setupClientTable() {
        colClient.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().clientName));
        colTotal.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f €", c.getValue().total)));
        colOrders.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().count)));

        clientTable.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null)
                showClientDetail(n);
        });
    }

    private void setupPurchaseTable() {
        colDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getSaleDateTime() != null ? c.getValue().getSaleDateTime().format(FMT) : "—"));
        colSeller.setCellValueFactory(c -> {
            try {
                User u = userUseCase.getUserById(c.getValue().getUserId());
                String name = (u != null && u.getFullName() != null && !u.getFullName().isBlank())
                        ? u.getFullName()
                        : (u != null ? u.getUsername() : "ID:" + c.getValue().getUserId());
                return new SimpleStringProperty(name);
            } catch (Exception e) {
                return new SimpleStringProperty("ID:" + c.getValue().getUserId());
            }
        });
        colMethod.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPaymentMethod()));
        colItems.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDetails() != null
                        ? String.valueOf(c.getValue().getDetails().stream().mapToInt(d -> d.getQuantity()).sum())
                        : "—"));
        colAmount.setCellValueFactory(c -> new SimpleStringProperty(String.format("%.2f €", c.getValue().getTotal())));
    }

    @FXML
    private void handleFilter() {
        loadData();
    }

    private void loadData() {
        LocalDate from = dpFrom.getValue() != null ? dpFrom.getValue() : LocalDate.now().withDayOfMonth(1);
        LocalDate to = dpTo.getValue() != null ? dpTo.getValue() : LocalDate.now();

        try {
            List<Sale> allSales = saleUseCase.getSalesByRange(from, to).stream()
                    .filter(s -> !s.isReturn() && s.getClientId() != null)
                    .collect(Collectors.toList());

            // Mapas auxiliares
            List<Client> clients = clientUseCase.getAllClients();
            Map<Integer, String> clientNames = new HashMap<>();
            Map<Integer, String> clientInfo = new HashMap<>();
            for (Client c : clients) {
                clientNames.put(c.getId(), c.getName());
                clientInfo.put(c.getId(), c.getTaxId() != null ? "CIF: " + c.getTaxId() : "");
            }

            // Agrupar ventas por clientId
            Map<Integer, List<Sale>> byClient = allSales.stream()
                    .collect(Collectors.groupingBy(s -> s.getClientId()));

            allRows = new ArrayList<>();
            for (Map.Entry<Integer, List<Sale>> entry : byClient.entrySet()) {
                String name = clientNames.getOrDefault(entry.getKey(), "Cliente #" + entry.getKey());
                String info = clientInfo.getOrDefault(entry.getKey(), "");
                double total = entry.getValue().stream().mapToDouble(Sale::getTotal).sum();
                allRows.add(
                        new ClientRow(entry.getKey(), name, info, total, entry.getValue().size(), entry.getValue()));
            }
            allRows.sort((a, b) -> Double.compare(b.total, a.total));

            // KPIs
            double totalAll = allRows.stream().mapToDouble(r -> r.total).sum();
            int countAll = allRows.stream().mapToInt(r -> r.count).sum();
            lblKpiClients.setText(String.valueOf(allRows.size()));
            lblKpiTotal.setText(String.format("%.2f €", totalAll));
            lblKpiBest.setText(allRows.isEmpty() ? "—" : allRows.get(0).clientName);
            lblKpiAvg.setText(countAll > 0 ? String.format("%.2f €", totalAll / countAll) : "0.00 €");

            filterClients(txtSearch != null ? txtSearch.getText() : "");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void filterClients(String query) {
        String q = query != null ? query.toLowerCase().trim() : "";
        ObservableList<ClientRow> filtered = FXCollections.observableArrayList(
                allRows.stream()
                        .filter(r -> q.isEmpty() || r.clientName.toLowerCase().contains(q))
                        .collect(Collectors.toList()));
        clientTable.setItems(filtered);
    }

    private void showClientDetail(ClientRow row) {
        lblDetailTitle.setText("📋 Compras de " + row.clientName);
        if (lblClientInfo != null)
            lblClientInfo.setText(row.info);
        purchaseTable.setItems(FXCollections.observableArrayList(row.sales));
    }

    // ── Inner model ─────────────────────────────────────────────────────────
    public static class ClientRow {
        public final int clientId;
        public final String clientName;
        public final String info;
        public final double total;
        public final int count;
        public final List<Sale> sales;

        public ClientRow(int clientId, String clientName, String info, double total, int count, List<Sale> sales) {
            this.clientId = clientId;
            this.clientName = clientName;
            this.info = info;
            this.total = total;
            this.count = count;
            this.sales = sales;
        }
    }
}
