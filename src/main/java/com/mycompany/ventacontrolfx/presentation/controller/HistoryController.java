package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.domain.model.SaleDetail;
import com.mycompany.ventacontrolfx.application.usecase.SaleUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.ModalService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.util.Pair;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import com.mycompany.ventacontrolfx.util.Searchable;

public class HistoryController implements Injectable, Searchable {

    @FXML
    private DatePicker datePickerStart, datePickerEnd;
    @FXML
    private TextField txtSearchId;
    @FXML
    private TableView<Sale> salesTable;
    @FXML
    private TableColumn<Sale, Integer> colId;
    @FXML
    private TableColumn<Sale, String> colUser, colDate, colMethod;
    @FXML
    private TableColumn<Sale, Double> colTotal;
    @FXML
    private Label lblTotalSalesCount, lblTotalAmount, lblTotalCash, lblTotalCard, lblCount;
    @FXML
    private VBox detailsPanel, detailsItemsContainer;
    @FXML
    private Label lblSaleId, lblSaleFullDate, lblPaymentMethod, lblTotalAmountDetail, lblReturnBadge;
    @FXML
    private Button btnReturn, btnPrint, btnQuickToday, btnQuickWeek, btnQuickMonth;

    private SaleUseCase saleUseCase;
    private ServiceContainer container;
    private final DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final String ACTIVE_CLASS = "active-filter";

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.saleUseCase = container.getSaleUseCase();

        datePickerStart.setValue(LocalDate.now());
        datePickerEnd.setValue(LocalDate.now());
        setupTable();
        setActiveFilter(btnQuickToday);
        loadSalesDirect();

        salesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null)
                showDetails(newVal);
        });
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("saleId"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("userName"));
        colDate.setCellValueFactory(
                data -> new SimpleStringProperty(data.getValue().getSaleDateTime().format(fullFormatter)));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colMethod.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));

        colTotal.setCellFactory(col -> new TableCell<Sale, Double>() {
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

        salesTable.setRowFactory(tv -> new TableRow<Sale>() {
            @Override
            protected void updateItem(Sale item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null) {
                    if (item.isReturn())
                        setStyle("-fx-background-color: #fff5f5;");
                    else if (item.getReturnedAmount() > 0)
                        setStyle("-fx-background-color: #fff8e1;");
                    else
                        setStyle("");
                } else
                    setStyle("");
            }
        });
    }

    @FXML
    private void loadSales() {
        setActiveFilter(null);
        if (txtSearchId != null && !txtSearchId.getText().trim().isEmpty()) {
            handleSearch();
            return;
        }
        loadSalesDirect();
    }

    private void loadSalesDirect() {
        try {
            LocalDate start = datePickerStart.getValue();
            LocalDate end = datePickerEnd.getValue();
            if (start == null || end == null)
                return;
            if (start.isAfter(end)) {
                AlertUtil.showError("Error", "La fecha de inicio no puede ser posterior a la de fin.");
                return;
            }
            List<Sale> sales = saleUseCase.getHistory(start, end);
            salesTable.setItems(FXCollections.observableArrayList(sales));
            updateSummaries(sales);
            handleCloseDetails();
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Error", "No se pudieron cargar las ventas: " + e.getMessage());
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

    private void setActiveFilter(Button btn) {
        if (btnQuickToday != null)
            btnQuickToday.getStyleClass().remove(ACTIVE_CLASS);
        if (btnQuickWeek != null)
            btnQuickWeek.getStyleClass().remove(ACTIVE_CLASS);
        if (btnQuickMonth != null)
            btnQuickMonth.getStyleClass().remove(ACTIVE_CLASS);
        if (btn != null && !btn.getStyleClass().contains(ACTIVE_CLASS))
            btn.getStyleClass().add(ACTIVE_CLASS);
    }

    @Override
    public void handleSearch(String text) {
        if (txtSearchId != null) {
            txtSearchId.setText(text);
            handleSearch();
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
            Sale s = saleUseCase.getSaleDetails(id);
            if (s != null) {
                salesTable.setItems(FXCollections.observableArrayList(s));
                updateSummaries(java.util.Collections.singletonList(s));
                salesTable.getSelectionModel().select(s);
            } else {
                AlertUtil.showInfo("No encontrado", "No se encontró el ticket #" + id);
            }
        } catch (NumberFormatException e) {
            AlertUtil.showError("Error", "El número de ticket debe ser numérico.");
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Error", "Error en la búsqueda: " + e.getMessage());
        }
    }

    @FXML
    private void handleClearFilters() {
        txtSearchId.clear();
        filterToday();
    }

    private void updateSummaries(List<Sale> sales) {
        int count = sales.size();
        double total = 0, cash = 0, card = 0;
        for (Sale s : sales) {
            double net = s.getTotal() - s.getReturnedAmount();
            total += net;
            if ("Tarjeta".equalsIgnoreCase(s.getPaymentMethod()))
                card += net;
            else
                cash += net;
        }

        // Emojis y colores integrados en el valor
        lblTotalSalesCount.setText("📊 " + count);
        lblTotalAmount.setText("💰 " + String.format("%.2f €", total));
        lblTotalCash.setText("💵 " + String.format("%.2f €", cash));
        lblTotalCard.setText("💳 " + String.format("%.2f €", card));

        if (lblCount != null)
            lblCount.setText("🔍 " + count + " tickets encontrados");
    }

    private void showDetails(Sale sale) {
        // Cargar detalles si no están presentes
        if (sale.getDetails() == null || sale.getDetails().isEmpty()) {
            try {
                List<SaleDetail> details = saleUseCase.getSaleDetails(sale.getSaleId()).getDetails();
                sale.setDetails(details);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        detailsPanel.setVisible(true);
        detailsPanel.setManaged(true);
        lblSaleId.setText("Ticket #" + String.format("%04d", sale.getSaleId()));

        boolean hasReturnable = sale.getDetails().stream()
                .anyMatch(d -> (d.getQuantity() - d.getReturnedQuantity()) > 0);
        boolean hasAnyReturned = sale.getDetails().stream().anyMatch(d -> d.getReturnedQuantity() > 0);

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
        // La opacidad y el color ahora se gestionan vía CSS (.btn-warning:disabled)

        lblSaleFullDate.setText(sale.getSaleDateTime().format(fullFormatter) + "\nAtendido por: " + sale.getUserName());

        String methodEmoji = "Tarjeta".equalsIgnoreCase(sale.getPaymentMethod()) ? "💳 " : "💵 ";
        lblPaymentMethod.setText(methodEmoji + sale.getPaymentMethod());

        lblTotalAmountDetail.setText(String.format("%.2f €", sale.getTotal()));
        // El color se gestiona mediante clases de utilidad
        lblTotalAmountDetail.getStyleClass().removeAll("text-total", "text-error");
        lblTotalAmountDetail.getStyleClass().add(sale.isReturn() ? "text-error" : "text-total");

        detailsItemsContainer.getChildren().clear();
        for (SaleDetail detail : sale.getDetails()) {
            HBox row = new HBox(10);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 8 0; -fx-border-color: #f8f9fa; -fx-border-width: 0 0 1 0;");

            VBox nameBox = new VBox(2);
            Label nameLabel = new Label(detail.getProductName());
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: "
                    + (detail.getReturnedQuantity() >= detail.getQuantity() ? "#e74c3c"
                            : (detail.getReturnedQuantity() > 0 ? "#f39c12" : "#2c3e50"))
                    + ";");

            Label qtyLabel = new Label(
                    detail.getQuantity() + " un. x " + String.format("%.2f", detail.getUnitPrice()) + " €");
            qtyLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6;");
            nameBox.getChildren().addAll(nameLabel, qtyLabel);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label priceLabel = new Label(String.format("%.2f €", detail.getLineTotal()));
            priceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");

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
        Sale s = salesTable.getSelectionModel().getSelectedItem();
        if (s != null)
            AlertUtil.showInfo("Impresión", "Enviando ticket #" + s.getSaleId() + "...");
    }

    @FXML
    private void handleRegisterReturn() {
        Sale selected = salesTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        ModalService.showModal("/view/return_dialog.fxml", "Devolución", Modality.APPLICATION_MODAL,
                StageStyle.TRANSPARENT, container, (ReturnDialogController controller) -> {
                    controller.init(selected);
                    controller.setOnSuccess((reason, items) -> {
                        try {
                            int userId = container.getUserSession().getCurrentUser().getUserId();
                            saleUseCase.registerPartialReturn(selected.getSaleId(), items, reason, userId);
                            AlertUtil.showInfo("Éxito", "Devolución registrada correctamente.");
                            loadSalesDirect();
                        } catch (SQLException e) {
                            e.printStackTrace();
                            AlertUtil.showError("Error", "No se pudo procesar la devolución: " + e.getMessage());
                        }
                    });
                });
    }
}
