package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.Sale;
import com.mycompany.ventacontrolfx.model.SaleDetail;
import com.mycompany.ventacontrolfx.service.SaleService;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.paint.Color;
import javafx.fxml.FXMLLoader;
import java.io.IOException;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

public class HistoryController implements com.mycompany.ventacontrolfx.util.Injectable {

    @FXML
    private DatePicker datePickerStart;
    @FXML
    private DatePicker datePickerEnd;
    @FXML
    private TextField txtSearchId;

    @FXML
    private TableView<Sale> salesTable;
    @FXML
    private TableColumn<Sale, Integer> colId;
    @FXML
    private TableColumn<Sale, String> colUser;
    @FXML
    private TableColumn<Sale, String> colDate;
    @FXML
    private TableColumn<Sale, Double> colTotal;
    @FXML
    private TableColumn<Sale, String> colMethod;

    // Summary Labels
    @FXML
    private Label lblTotalSalesCount;
    @FXML
    private Label lblTotalAmount;
    @FXML
    private Label lblTotalCash;
    @FXML
    private Label lblTotalCard;
    @FXML
    private Label lblCount;

    // Details Panel
    @FXML
    private VBox detailsPanel;
    @FXML
    private Label lblSaleId;
    @FXML
    private Label lblSaleFullDate;
    @FXML
    private VBox detailsItemsContainer;

    // Quick Filter Buttons
    @FXML
    private Button btnQuickToday;
    @FXML
    private Button btnQuickWeek;
    @FXML
    private Button btnQuickMonth;

    private final String ACTIVE_STYLE = "-fx-background-color: #1e88e5; -fx-text-fill: white; -fx-border-color: #1e88e5; -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 5 15; -fx-cursor: hand;";
    private final String INACTIVE_STYLE = "-fx-background-color: white; -fx-text-fill: #2c3e50; -fx-border-color: #ddd; -fx-border-radius: 20; -fx-background-radius: 20; -fx-padding: 5 15; -fx-cursor: hand;";
    @FXML
    private Label lblPaymentMethod;
    @FXML
    private Label lblTotalAmountDetail;
    @FXML
    private Label lblReturnBadge;
    @FXML
    private Button btnReturn;
    @FXML
    private Button btnPrint;

    private SaleService saleService;
    private com.mycompany.ventacontrolfx.service.ServiceContainer container;
    private final DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Override
    public void inject(com.mycompany.ventacontrolfx.service.ServiceContainer container) {
        this.container = container;
        this.saleService = container.getSaleService();

        // CRÍTICO: Todo lo que necesita saleService debe ir aquí, NO en initialize().
        // JavaFX llama a initialize() ANTES de inject(), por lo que saleService sería
        // null allí.
        datePickerStart.setValue(LocalDate.now());
        datePickerEnd.setValue(LocalDate.now());
        setupTable();
        setActiveFilter(btnQuickToday);
        loadSalesDirect();

        salesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showDetails(newSelection);
            }
        });
    }

    @FXML
    public void initialize() {
        // Vacío intencionalmente: initialize() es llamado por JavaFX ANTES de inject().
        // Toda la lógica que dependa de servicios debe ir en inject().
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("saleId"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("userName"));
        colDate.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getSaleDateTime().format(fullFormatter)));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colMethod.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));

        // Format total column
        colTotal.setCellFactory(column -> new TableCell<Sale, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    Sale s = getTableRow().getItem();
                    setText(String.format("%.2f €", item));
                    if (s != null) {
                        if (s.isReturn()) {
                            setStyle("-fx-font-weight: bold; -fx-text-fill: #e74c3c; -fx-strikethrough: true;");
                        } else if (s.getReturnedAmount() > 0) {
                            setStyle("-fx-font-weight: bold; -fx-text-fill: #f39c12;");
                        } else {
                            setStyle("-fx-font-weight: bold; -fx-text-fill: #1e88e5;");
                        }
                    }
                }
            }
        });

        salesTable.setRowFactory(tv -> {
            TableRow<Sale> row = new TableRow<Sale>() {
                @Override
                protected void updateItem(Sale item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item != null) {
                        if (item.isReturn()) {
                            setStyle("-fx-background-color: #fff5f5;");
                        } else if (item.getReturnedAmount() > 0) {
                            setStyle("-fx-background-color: #fff8e1;");
                        } else {
                            setStyle("");
                        }
                    } else {
                        setStyle("");
                    }
                }
            };
            row.setPrefHeight(50);
            return row;
        });
    }

    @FXML
    private void loadSales() {
        // When manually filtering, clear quick filter highlights
        setActiveFilter(null);

        if (txtSearchId != null && !txtSearchId.getText().trim().isEmpty()) {
            handleSearch();
            return;
        }
        try {
            LocalDate start = datePickerStart.getValue();
            LocalDate end = datePickerEnd.getValue();

            if (start == null || end == null)
                return;
            if (start.isAfter(end)) {
                showAlert("Error", "La fecha de inicio no puede ser posterior a la de fin.");
                return;
            }

            List<Sale> sales = saleService.getSalesHistory(start, end);
            salesTable.setItems(FXCollections.observableArrayList(sales));

            updateSummaries(sales);
            handleCloseDetails();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "No se pudieron cargar las ventas: " + e.getMessage());
        }
    }

    @FXML
    private void filterToday() {
        datePickerStart.setValue(LocalDate.now());
        datePickerEnd.setValue(LocalDate.now());
        setActiveFilter(btnQuickToday);
        loadSalesDirect();
    }

    @FXML
    private void filterLast7Days() {
        datePickerStart.setValue(LocalDate.now().minusDays(7));
        datePickerEnd.setValue(LocalDate.now());
        setActiveFilter(btnQuickWeek);
        loadSalesDirect();
    }

    @FXML
    private void filterThisMonth() {
        datePickerStart.setValue(LocalDate.now().withDayOfMonth(1));
        datePickerEnd.setValue(LocalDate.now());
        setActiveFilter(btnQuickMonth);
        loadSalesDirect();
    }

    private void setActiveFilter(Button activeBtn) {
        if (btnQuickToday != null)
            btnQuickToday.setStyle(INACTIVE_STYLE);
        if (btnQuickWeek != null)
            btnQuickWeek.setStyle(INACTIVE_STYLE);
        if (btnQuickMonth != null)
            btnQuickMonth.setStyle(INACTIVE_STYLE);

        if (activeBtn != null) {
            activeBtn.setStyle(ACTIVE_STYLE);
        }
    }

    // New internal method to load without resetting highlights
    private void loadSalesDirect() {
        try {
            LocalDate start = datePickerStart.getValue();
            LocalDate end = datePickerEnd.getValue();

            if (start == null || end == null)
                return;
            List<Sale> sales = saleService.getSalesHistory(start, end);
            salesTable.setItems(FXCollections.observableArrayList(sales));
            updateSummaries(sales);
            handleCloseDetails();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSearch() {
        String search = txtSearchId.getText().trim();
        if (search.isEmpty()) {
            loadSales();
            return;
        }

        try {
            int id = Integer.parseInt(search);
            Sale s = saleService.getSaleById(id);

            if (s != null) {
                salesTable.setItems(FXCollections.observableArrayList(s));
                updateSummaries(java.util.Collections.singletonList(s));
                salesTable.getSelectionModel().select(s);
                showDetails(s);
            } else {
                showAlert("No encontrado", "No se encontró el ticket #" + id);
            }
        } catch (NumberFormatException e) {
            showAlert("Error", "El número de ticket debe ser un valor numérico.");
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "Error en la búsqueda: " + e.getMessage());
        }
    }

    @FXML
    private void handleClearFilters() {
        txtSearchId.clear();
        datePickerStart.setValue(LocalDate.now());
        datePickerEnd.setValue(LocalDate.now());
        setActiveFilter(btnQuickToday);
        loadSalesDirect();
    }

    private void updateSummaries(List<Sale> sales) {
        int count = 0;
        double total = 0;
        double cash = 0;
        double card = 0;

        for (Sale s : sales) {
            count++;

            // Initial sale total
            double netSaleTotal = s.getTotal();

            // Subtract returned amount if any
            netSaleTotal -= s.getReturnedAmount();

            total += netSaleTotal;
            if ("Tarjeta".equalsIgnoreCase(s.getPaymentMethod())) {
                card += netSaleTotal;
            } else {
                cash += netSaleTotal;
            }
        }

        lblTotalSalesCount.setText(String.valueOf(count));
        lblTotalAmount.setText(String.format("%.2f €", total));
        lblTotalCash.setText(String.format("%.2f €", cash));
        lblTotalCard.setText(String.format("%.2f €", card));

        if (lblCount != null) {
            lblCount.setText(count + " tickets encontrados");
        }
    }

    private void showDetails(Sale sale) {
        detailsPanel.setVisible(true);
        detailsPanel.setManaged(true);
        lblSaleId.setText("Ticket #" + String.format("%04d", sale.getSaleId()));

        boolean hasReturnable = false;
        boolean hasAnyReturned = false;

        for (SaleDetail d : sale.getDetails()) {
            if (d.getQuantity() > d.getReturnedQuantity())
                hasReturnable = true;
            if (d.getReturnedQuantity() > 0)
                hasAnyReturned = true;
        }

        if (sale.isReturn()) {
            lblReturnBadge.setText("DEVUELTO");
            lblReturnBadge.setVisible(true);
            lblReturnBadge.setStyle(
                    "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-background-radius: 4;");
        } else if (hasAnyReturned) {
            lblReturnBadge.setText("PARCIAL");
            lblReturnBadge.setVisible(true);
            lblReturnBadge.setStyle(
                    "-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-background-radius: 4;");
        } else {
            lblReturnBadge.setVisible(false);
        }

        btnReturn.setDisable(!hasReturnable);
        btnReturn.setOpacity(hasReturnable ? 1.0 : 0.5);

        // Add worker name too
        lblSaleFullDate.setText(sale.getSaleDateTime().format(fullFormatter) + "\nAtendido por: " + sale.getUserName());
        lblPaymentMethod.setText(sale.getPaymentMethod());
        lblTotalAmountDetail.setText(String.format("%.2f €", sale.getTotal()));

        if (sale.isReturn()) {
            lblTotalAmountDetail.setStyle(
                    "-fx-font-weight: bold; -fx-font-size: 24px; -fx-text-fill: #e74c3c; -fx-strikethrough: true;");
        } else {
            lblTotalAmountDetail.setStyle("-fx-font-weight: bold; -fx-font-size: 24px; -fx-text-fill: #1e88e5;");
        }

        detailsItemsContainer.getChildren().clear();
        for (SaleDetail detail : sale.getDetails()) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 8 0; -fx-border-color: #f8f9fa; -fx-border-width: 0 0 1 0;");

            VBox nameBox = new VBox(2);
            Label nameLabel = new Label();
            if (detail.getReturnedQuantity() >= detail.getQuantity()) {
                nameLabel.setText(detail.getProductName() + " (Devuelto)");
                nameLabel.setStyle(
                        "-fx-font-weight: bold; -fx-text-fill: #e74c3c; -fx-strikethrough: true; -fx-font-size: 13px;");
            } else if (detail.getReturnedQuantity() > 0) {
                nameLabel.setText(detail.getProductName() + " (Devuelto: " + detail.getReturnedQuantity() + ")");
                nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #f39c12; -fx-font-size: 13px;");
            } else {
                nameLabel.setText(detail.getProductName());
                nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 13px;");
            }
            Label qtyLabel = new Label(
                    detail.getQuantity() + " un. x " + String.format("%.2f", detail.getUnitPrice()) + " €");
            qtyLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6;");
            nameBox.getChildren().addAll(nameLabel, qtyLabel);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            Label priceLabel = new Label(String.format("%.2f €", detail.getLineTotal()));
            priceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 14px;");

            row.getChildren().addAll(nameBox, spacer, priceLabel);
            detailsItemsContainer.getChildren().add(row);
        }
    }

    @FXML
    private void handleCloseDetails() {
        detailsPanel.setVisible(false);
        detailsPanel.setManaged(false);
        salesTable.getSelectionModel().clearSelection();
    }

    @FXML
    private void handlePrintTicket() {
        Sale selected = salesTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            AlertUtil.showInfo("Impresión",
                    "Enviando copia del ticket #" + selected.getSaleId() + " a la impresora...");
        }
    }

    @FXML
    private void handleRegisterReturn() {
        Sale selected = salesTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        // Check if there are items left to return
        boolean hasReturnableItems = selected.getDetails().stream()
                .anyMatch(d -> (d.getQuantity() - d.getReturnedQuantity()) > 0);

        if (!hasReturnableItems) {
            AlertUtil.showInfo("Información", "Todos los productos de esta venta ya han sido devueltos.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/return_dialog.fxml"));
            Parent root = loader.load();
            ReturnDialogController controller = loader.getController();
            controller.init(selected);

            Stage stage = new Stage();
            stage.initStyle(StageStyle.TRANSPARENT);
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);

            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/view/style.css").toExternalForm());
            scene.setFill(Color.TRANSPARENT);
            stage.setScene(scene);

            stage.showAndWait();

            if (controller.isConfirmClicked()) {
                Pair<String, Map<Integer, Integer>> request = controller.getResult();
                String reason = request.getKey();
                Map<Integer, Integer> items = request.getValue();

                saleService.registerPartialReturn(selected.getSaleId(), items, reason);

                // Refresh data
                loadSales();

                // Show updated details
                Sale updatedSale = saleService.getSaleById(selected.getSaleId());
                if (updatedSale != null) {
                    showDetails(updatedSale);
                }

                AlertUtil.showInfo("Éxito", "La devolución parcial se ha registrado correctamente.");
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Error", "No se pudo procesar la devolución: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        AlertUtil.showInfo(title, content);
    }
}
