package com.mycompany.ventacontrolfx.presentation.controller.receipt;

import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.shared.util.PaginationHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Gestiona la visualización y renderizado de la tabla de historial de ventas.
 */
public class HistoryTableManager {

    private final ServiceContainer container;
    private final TableView<Sale> salesTable;
    private final TableColumn<Sale, Integer> colId;
    private final TableColumn<Sale, String> colUser, colDate, colMethod, colFiscalStatus;
    private final TableColumn<Sale, Double> colTotal;
    private final ComboBox<Integer> cmbRowLimit;
    private final Label lblCount;

    private PaginationHelper<Sale> paginationHelper;
    private final DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public HistoryTableManager(
            ServiceContainer container,
            TableView<Sale> salesTable,
            TableColumn<Sale, Integer> colId,
            TableColumn<Sale, String> colUser,
            TableColumn<Sale, String> colDate,
            TableColumn<Sale, String> colMethod,
            TableColumn<Sale, String> colFiscalStatus,
            TableColumn<Sale, Double> colTotal,
            ComboBox<Integer> cmbRowLimit,
            Label lblCount) {
        this.container = container;
        this.salesTable = salesTable;
        this.colId = colId;
        this.colUser = colUser;
        this.colDate = colDate;
        this.colMethod = colMethod;
        this.colFiscalStatus = colFiscalStatus;
        this.colTotal = colTotal;
        this.cmbRowLimit = cmbRowLimit;
        this.lblCount = lblCount;
    }

    public void init() {
        setupTable();
        paginationHelper = new PaginationHelper<>(salesTable, cmbRowLimit, lblCount,
                container.getBundle().getString("sales.entity_plural"), container.getBundle());
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("saleId"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("userName"));
        
        colDate.setCellValueFactory(data -> {
            Sale sale = data.getValue();
            String formattedDate = sale.getSaleDateTime().format(fullFormatter);
            String status = sale.getFiscalStatus();
            String aeatEmoji = "ACCEPTED".equals(status) ? "✅ "
                    : ("REJECTED".equals(status) ? "❌ " : ("PENDING".equals(status) ? "⏳ " : ""));
            return new SimpleStringProperty(aeatEmoji + formattedDate);
        });

        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colMethod.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        colFiscalStatus.setCellValueFactory(new PropertyValueFactory<>("fiscalStatus"));

        // Cell Factory: Fiscal Status (Badges)
        colFiscalStatus.setCellFactory(col -> new TableCell<Sale, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label lbl = new Label();
                    lbl.getStyleClass().add("badge-base");
                    if ("ACCEPTED".equals(item)) {
                        lbl.setText(container.getBundle().getString("history.status.accepted"));
                        lbl.getStyleClass().add("badge-success");
                    } else if ("REJECTED".equals(item)) {
                        lbl.setText(container.getBundle().getString("history.status.rejected"));
                        lbl.getStyleClass().add("badge-danger");
                    } else {
                        lbl.setText(container.getBundle().getString("history.status.pending"));
                        lbl.getStyleClass().add("badge-warning");
                    }
                    setGraphic(lbl);
                }
            }
        });

        // Cell Factory: Total (Colors & Returns)
        colTotal.setCellFactory(col -> new TableCell<Sale, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    Sale s = getTableRow().getItem();
                    setText(String.format("%.2f \u20ac", item));
                    if (s != null) {
                        boolean isFullReturn = s.isReturn()
                                || (s.getReturnedAmount() >= (s.getTotal() - 0.01) && s.getTotal() > 0);
                        if (isFullReturn) {
                            setStyle("-fx-font-weight: bold; -fx-text-fill: #ef4444; -fx-strikethrough: true;");
                        } else if (s.getReturnedAmount() > 0) {
                            setStyle("-fx-font-weight: bold; -fx-text-fill: #f59e0b;");
                        } else {
                            setStyle("-fx-font-weight: bold; -fx-text-fill: #3b82f6;");
                        }
                    }
                }
            }
        });

        // Cell Factory: Method (Emojis)
        colMethod.setCellFactory(col -> new TableCell<Sale, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String translated;
                    String emoji;
                    String lowerItem = item.toLowerCase();
                    if (lowerItem.contains("mixed") || lowerItem.contains("mixto")) {
                        translated = container.getBundle().getString("payment.method.mixed");
                        emoji = "\ud83d\udd04 ";
                    } else if (lowerItem.contains("card") || lowerItem.contains("tarjeta")) {
                        translated = container.getBundle().getString("payment.method.card");
                        emoji = "\ud83d\udcb3 ";
                    } else {
                        translated = container.getBundle().getString("payment.method.cash");
                        emoji = "\ud83d\udcb5 ";
                    }
                    setText(emoji + translated);
                }
            }
        });

        // Row Factory: Return Styling
        salesTable.setRowFactory(tv -> new TableRow<Sale>() {
            @Override
            protected void updateItem(Sale item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("row-return", "row-partial-return");
                if (item != null) {
                    if (item.isReturn())
                        getStyleClass().add("row-return");
                    else if (item.getReturnedAmount() > 0)
                        getStyleClass().add("row-partial-return");
                }
            }
        });
    }

    public void setData(List<Sale> data) {
        if (paginationHelper != null) {
            paginationHelper.setData(data);
        }
    }

    public Sale getSelection() {
        return salesTable.getSelectionModel().getSelectedItem();
    }

    public void clearSelection() {
        salesTable.getSelectionModel().clearSelection();
    }

    public void select(Sale sale) {
        salesTable.getSelectionModel().select(sale);
    }
}


