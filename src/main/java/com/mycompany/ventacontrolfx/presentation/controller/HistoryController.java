package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.Sale;
import com.mycompany.ventacontrolfx.domain.model.SaleDetail;
import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.domain.model.CartItem;
import com.mycompany.ventacontrolfx.application.usecase.SaleUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.ModalService;
import com.mycompany.ventacontrolfx.util.PaginationHelper;
import com.mycompany.ventacontrolfx.util.Searchable;
import com.mycompany.ventacontrolfx.presentation.util.RealTimeSearchBinder;
import com.mycompany.ventacontrolfx.util.DateFilterUtils;
import javafx.beans.property.SimpleStringProperty;
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

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
    private ComboBox<Integer> cmbRowLimit;
    @FXML
    private VBox detailsPanel, detailsItemsContainer;
    @FXML
    private Label lblSaleId, lblSaleFullDate, lblPaymentMethod, lblTotalAmountDetail, lblReturnBadge;
    @FXML
    private Button btnReturn, btnPrint;
    @FXML
    private HBox quickFilterContainer;

    private SaleUseCase saleUseCase;
    private ServiceContainer container;
    private PaginationHelper<Sale> paginationHelper;
    private final DateTimeFormatter fullFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.saleUseCase = container.getSaleUseCase();

        datePickerStart.setValue(LocalDate.now());
        datePickerEnd.setValue(LocalDate.now());
        setupTable();
        paginationHelper = new PaginationHelper<>(salesTable, cmbRowLimit, lblCount,
                container.getBundle().getString("sales.entity_plural"), container.getBundle());

        DateFilterUtils.addQuickFilters(quickFilterContainer, datePickerStart,
                datePickerEnd, this::loadSalesDirect);

        loadSalesDirect();

        salesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null)
                showDetails(newVal);
        });

        if (txtSearchId != null) {
            RealTimeSearchBinder.bind(txtSearchId, query -> handleSearch());
        }

        // Verificaci\u00f3n de permisos para Devoluciones
        boolean canReturn = container.getUserSession().hasPermission("venta.devolucion");
        btnReturn.setVisible(canReturn);
        btnReturn.setManaged(canReturn);
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
                    setText(String.format("%.2f \u20ac", item)); // Formato de moneda sigue igual por ahora, pero
                                                                 // podr\u00edamos
                    // usar cfg
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

        colMethod.setCellFactory(col -> new TableCell<Sale, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String emoji = item.contains("Mixed") || item.contains("Mixto") ? "\ud83d\udd04 "
                            : (container.getBundle().getString("payment.method.card").equalsIgnoreCase(item)
                                    ? "\ud83d\udcb3 "
                                    : "\ud83d\udcb5 ");
                    setText(emoji + item);
                }
            }
        });

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

    @FXML
    private void loadSales() {
        if (txtSearchId != null && !txtSearchId.getText().trim().isEmpty()) {
            handleSearch();
            return;
        }
        loadSalesDirect();
    }

    private void loadSalesDirect() {
        LocalDate start = datePickerStart.getValue();
        LocalDate end = datePickerEnd.getValue();

        if (start == null || end == null) {
            start = LocalDate.now().minusDays(30);
            end = LocalDate.now();
            datePickerStart.setValue(start);
            datePickerEnd.setValue(end);
        }

        if (start.isAfter(end)) {
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("history.error.date_range"));
            return;
        }

        final LocalDate finalStart = start;
        final LocalDate finalEnd = end;

        // ASYNC LOADING: Prevents UI from freezing during DB queries
        container.getAsyncManager().runAsyncTask(() -> {
            try {
                com.mycompany.ventacontrolfx.domain.model.HistoryStats stats = saleUseCase.getHistoryStats(finalStart,
                        finalEnd);
                List<Sale> sales = saleUseCase.getSalesByRange(finalStart, finalEnd, 500);

                return new Object[] { stats, sales };
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, result -> {
            Object[] data = (Object[]) result;
            updateSummaries((com.mycompany.ventacontrolfx.domain.model.HistoryStats) data[0]);
            paginationHelper.setData((List<Sale>) data[1]);
            handleCloseDetails();
        }, e -> {
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("history.error.load") + ": " + e.getMessage());
        });
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
                paginationHelper.setData(java.util.Collections.singletonList(s));
                // Update stats to show ONLY this sale info in the KPIs temporarily?
                // Or just keep the general stats. For UX, showing the current sale stats is
                // okay.
                updateSummaries(new com.mycompany.ventacontrolfx.domain.model.HistoryStats(1, s.getTotal(),
                        s.getCashAmount(), s.getCardAmount()));
                salesTable.getSelectionModel().select(s);
            } else {
                AlertUtil.showInfo(container.getBundle().getString("alert.not_found"),
                        container.getBundle().getString("history.error.not_found") + " #" + id);
            }
        } catch (NumberFormatException e) {
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("history.error.numeric_id"));
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("history.error.search") + ": " + e.getMessage());
        }
    }

    @FXML
    private void handleClearFilters() {
        txtSearchId.clear();
        datePickerStart.setValue(LocalDate.now());
        datePickerEnd.setValue(LocalDate.now());
        loadSalesDirect();
    }

    private void updateSummaries(com.mycompany.ventacontrolfx.domain.model.HistoryStats stats) {
        lblTotalSalesCount.setText("\ud83d\udcca " + stats.getCount());
        lblTotalAmount.setText("\ud83d\udcb0 " + String.format("%.2f \u20ac", stats.getTotalRevenue()));
        lblTotalCash.setText("\ud83d\udcb5 " + String.format("%.2f \u20ac", stats.getTotalCash()));
        lblTotalCard.setText("\ud83d\udcb3 " + String.format("%.2f \u20ac", stats.getTotalCard()));

        if (lblCount != null)
            lblCount.setText(
                    "\ud83d\udd0d " + stats.getCount() + " " + container.getBundle().getString("history.count_suffix"));
    }

    private void showDetails(Sale sale) {
        detailsPanel.setVisible(true);
        detailsPanel.setManaged(true);
        lblSaleId.setText(container.getBundle().getString("history.detail.ticket") + " #"
                + String.format("%04d", sale.getSaleId()));

        // ASYNC LOADING of details: Prevents lag when scrolling selection
        container.getAsyncManager().runAsyncTask(() -> {
            try {
                // Return original sale if details are already there, otherwise fetch.
                if (sale.getDetails() == null || sale.getDetails().isEmpty()) {
                    Sale detailed = saleUseCase.getSaleDetails(sale.getSaleId());
                    return detailed;
                }
                return sale;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }, result -> {
            Sale detailedSale = (Sale) result;
            if (detailedSale != null && detailedSale.getDetails() != null) {
                updateDetailsUI(detailedSale);
            }
        }, e -> {
            e.printStackTrace();
        });
    }

    private void updateDetailsUI(Sale sale) {
        boolean hasAnyReturned = sale.getDetails().stream().anyMatch(d -> d.getReturnedQuantity() > 0);
        boolean allReturned = !sale.getDetails().isEmpty()
                && sale.getDetails().stream().allMatch(d -> d.getReturnedQuantity() >= d.getQuantity());

        if (sale.isReturn() || allReturned) {
            lblReturnBadge.setText(container.getBundle().getString("history.status.returned"));
            lblReturnBadge.setVisible(true);
            lblReturnBadge.setStyle(
                    "-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2px 6px; -fx-background-radius: 4px;");
        } else if (hasAnyReturned) {
            lblReturnBadge.setText(container.getBundle().getString("history.status.partial"));
            lblReturnBadge.setVisible(true);
            lblReturnBadge.setStyle(
                    "-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2px 6px; -fx-background-radius: 4px;");
        } else {
            lblReturnBadge.setVisible(false);
        }

        btnReturn.setDisable(sale.isReturn());

        lblSaleFullDate.setText(sale.getSaleDateTime().format(fullFormatter) + "\n"
                + container.getBundle().getString("receipt.attended_by") + ": " + sale.getUserName());

        String methodEmoji = container.getBundle().getString("payment.method.card")
                .equalsIgnoreCase(sale.getPaymentMethod()) ? "\ud83d\udcb3 " : "\ud83d\udcb5 ";
        lblPaymentMethod.setText(
                methodEmoji + (sale.getPaymentMethod().contains("Mixed") || sale.getPaymentMethod().contains("Mixto")
                        ? container.getBundle().getString("payment.method.mixed")
                        : sale.getPaymentMethod()));

        lblTotalAmountDetail.setText(String.format("%.2f \u20ac", sale.getTotal()));
        lblTotalAmountDetail.getStyleClass().removeAll("text-total", "text-error");
        lblTotalAmountDetail.getStyleClass().add(sale.isReturn() ? "text-error" : "text-total");

        detailsItemsContainer.getChildren().clear();
        for (SaleDetail detail : sale.getDetails()) {
            detailsItemsContainer.getChildren().add(createDetailRow(detail));
        }
    }

    private HBox createDetailRow(SaleDetail detail) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-padding: 8 0; -fx-border-color: #f8f9fa; -fx-border-width: 0 0 1 0;");

        VBox nameBox = new VBox(2);
        Label nameLabel = new Label(detail.getProductName());
        String color = detail.getReturnedQuantity() > 0 ? "#f39c12" : "#2c3e50";
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: " + color + ";");

        Label qtyLabel = new Label("\ud83d\udce6 " + detail.getQuantity() + " un. x "
                + String.format("%.2f", detail.getUnitPrice()) + " \u20ac");
        qtyLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6;");
        nameBox.getChildren().addAll(nameLabel, qtyLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label priceLabel = new Label("\ud83d\udcb0 " + String.format("%.2f \u20ac", detail.getLineTotal()));
        priceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");

        row.getChildren().addAll(nameBox, spacer, priceLabel);
        return row;
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
        if (selected == null) {
            AlertUtil.showWarning(container.getBundle().getString("alert.warning"),
                    container.getBundle().getString("history.error.no_selection"));
            return;
        }

        // 1. Garantizar que los detalles est\u00e9n cargados
        if (selected.getDetails() == null || selected.getDetails().isEmpty()) {
            try {
                Sale fullSale = saleUseCase.getSaleDetails(selected.getSaleId());
                if (fullSale != null && fullSale.getDetails() != null) {
                    selected.setDetails(fullSale.getDetails());
                    // Tambi\u00e9n actualizar clientId si no lo tiene (aunque deber\u00eda)
                    if (selected.getClientId() == null) {
                        selected.setClientId(fullSale.getClientId());
                    }
                }
            } catch (SQLException e) {
                AlertUtil.showError(container.getBundle().getString("alert.error"),
                        container.getBundle().getString("history.error.load_details") + ": " + e.getMessage());
                return;
            }
        }

        if (selected.getDetails() == null || selected.getDetails().isEmpty()) {
            AlertUtil.showWarning(container.getBundle().getString("alert.warning"),
                    container.getBundle().getString("history.error.no_details"));
            return;
        }

        // 2. Abrir vista previa de impresi\u00f3n
        ModalService.showStandardModal("/view/print_preview.fxml",
                selected.getClientId() != null ? container.getBundle().getString("receipt.title.invoice")
                        : container.getBundle().getString("receipt.title.simplified"),
                container,
                (PrintPreviewController ppc) -> {
                    try {
                        // Convertir SaleDetails a CartItems
                        List<CartItem> cartItems = new java.util.ArrayList<>();
                        for (SaleDetail detail : selected.getDetails()) {
                            Product p = new Product();
                            p.setName(detail.getProductName());
                            p.setPrice(detail.getUnitPrice());
                            p.setIva(detail.getIvaRate()); // Usar IVA hist\u00f3rico
                            cartItems.add(new CartItem(p, detail.getQuantity()));
                        }

                        // Cargar cliente si existe
                        if (selected.getClientId() != null) {
                            com.mycompany.ventacontrolfx.domain.model.Client client = container.getClientUseCase()
                                    .getById(selected.getClientId());
                            if (client != null) {
                                ppc.setClientInfo(client);
                            }
                        }

                        // Pasar los datos al controlador de vista previa
                        ppc.setReceiptData(cartItems, selected.getTotal(), selected.getTotal(), 0.0,
                                selected.getPaymentMethod(), selected.getSaleId());

                    } catch (Exception e) {
                        e.printStackTrace();
                        AlertUtil.showError(container.getBundle().getString("alert.error"),
                                container.getBundle().getString("history.error.load_preview"));
                    }
                });
    }

    @FXML
    private void handleRegisterReturn() {
        Sale selected = salesTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        // Garantizar que los detalles est\u00e9n cargados antes de abrir el
        // di\u00e1logo
        if (selected.getDetails() == null || selected.getDetails().isEmpty()) {
            try {
                Sale fullSale = saleUseCase.getSaleDetails(selected.getSaleId());
                if (fullSale != null && fullSale.getDetails() != null) {
                    selected.setDetails(fullSale.getDetails());
                }
            } catch (SQLException e) {
                AlertUtil.showError(container.getBundle().getString("alert.error"),
                        container.getBundle().getString("history.error.load_details") + ": " + e.getMessage());
                return;
            }
        }

        if (selected.getDetails() == null || selected.getDetails().isEmpty()) {
            AlertUtil.showWarning(container.getBundle().getString("alert.warning"),
                    container.getBundle().getString("history.error.already_returned_all"));
            return;
        }

        // Calcular el m\u00e1ximo devolvible (productos a\u00fan no devueltos) usando
        // el precio
        // real pagado
        double maxRefundable = selected.getDetails().stream()
                .mapToDouble(d -> (d.getQuantity() - d.getReturnedQuantity()) * (d.getLineTotal() / d.getQuantity()))
                .sum();

        if (maxRefundable > 0) {
            try {
                com.mycompany.ventacontrolfx.application.usecase.CashClosureUseCase closureUseCase = container
                        .getClosureUseCase();
                double cashInDrawer = closureUseCase.getCurrentCashInDrawer();

                // Calcular cu\u00e1nto del reembolso total es OBLIGATORIAMENTE en efectivo
                // (basado
                // en el pago original)
                double currentGrossTotal = selected.getTotal() + selected.getDiscountAmount();
                double cashRatio = (currentGrossTotal > 0) ? selected.getCashAmount() / currentGrossTotal : 1.0;
                double maxCashRefundNeeded = maxRefundable * cashRatio;

                if (maxCashRefundNeeded > cashInDrawer) {
                    String oldTicketHint = selected.getClosureId() != null
                            ? "\n\n\ud83d\udca1 *Recuerda*: Este ticket es de una sesi\u00f3n antigua."
                            : "";

                    boolean continuar = AlertUtil.showConfirmation(
                            "\u00e2\u0161\u00a0\u00ef\u00b8\u008f Efectivo limitado",
                            "Efectivo insuficiente para la parte de met\u00e1lico",
                            String.format(
                                    "El efectivo en caja (%.2f \u20ac) es menor que la parte pagada en efectivo del ticket (%.2f \u20ac).\n\n"
                                            +
                                            "El sistema devolver\u00e1 %.2f \u20ac a la tarjeta automaticamente, pero solo dispone de %.2f \u20ac "
                                            + "f\u00edsicos para la parte de efectivo.%s\n\n"
                                            + "\ubfdeseas continuar con una devoluci\u00f3n parcial o total sabiendo que el caj\u00f3n quedar\u00e1 bajo?",
                                    cashInDrawer, maxCashRefundNeeded, maxRefundable * (1 - cashRatio), cashInDrawer,
                                    oldTicketHint));
                    if (!continuar)
                        return;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        ModalService.showModal("/view/return_dialog.fxml",
                container.getBundle().getString("history.btn.register_return"), Modality.APPLICATION_MODAL,
                StageStyle.TRANSPARENT, container, (ReturnDialogController controller) -> {
                    controller.init(selected, container);
                    controller.setOnSuccess((reason, items) -> {
                        try {
                            int userId = container.getUserSession().getCurrentUser().getUserId();
                            saleUseCase.registerPartialReturn(selected.getSaleId(), items, reason, userId);
                            AlertUtil.showInfo(container.getBundle().getString("alert.success"),
                                    container.getBundle().getString("history.success.return"));
                            loadSalesDirect();
                        } catch (SQLException e) {
                            e.printStackTrace();
                            AlertUtil.showError("Error", "No se pudo procesar la devoluci\u00f3n: " + e.getMessage());
                        }
                    });
                });
    }
}
