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

        // Verificación de permisos para Devoluciones
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
                    setText(String.format("%.2f €", item)); // Formato de moneda sigue igual por ahora, pero podríamos
                                                            // usar cfg
                    if (s != null) {
                        if (s.isReturn()) {
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
                    String emoji = item.contains("Mixed") || item.contains("Mixto") ? "🔄 "
                            : (container.getBundle().getString("payment.method.card").equalsIgnoreCase(item) ? "💳 "
                                    : "💵 ");
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
        try {
            LocalDate start = datePickerStart.getValue();
            LocalDate end = datePickerEnd.getValue();

            List<Sale> sales;
            if (start == null || end == null) {
                // All time (wide range)
                sales = saleUseCase.getHistory(LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1));
            } else {
                if (start.isAfter(end)) {
                    AlertUtil.showError(container.getBundle().getString("alert.error"),
                            container.getBundle().getString("history.error.date_range"));
                    return;
                }
                sales = saleUseCase.getHistory(start, end);
            }
            paginationHelper.setData(sales);
            updateSummaries(sales);
            handleCloseDetails();
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("history.error.load") + ": " + e.getMessage());
        }
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
                updateSummaries(java.util.Collections.singletonList(s));
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

    private void updateSummaries(List<Sale> sales) {
        int count = sales.size();
        double total = 0, cash = 0, card = 0;
        for (Sale s : sales) {
            double netRatio = (s.getTotal() - s.getReturnedAmount()) / (s.getTotal() == 0 ? 1 : s.getTotal());
            total += (s.getTotal() - s.getReturnedAmount());

            double saleCash = s.getCashAmount();
            double saleCard = s.getCardAmount();

            // Fallback para registros antiguos sin desglosar
            if (saleCash == 0 && saleCard == 0) {
                if ("Tarjeta".equalsIgnoreCase(s.getPaymentMethod()))
                    saleCard = s.getTotal();
                else
                    saleCash = s.getTotal();
            }

            cash += saleCash * netRatio;
            card += saleCard * netRatio;
        }

        // Emojis y colores integrados en el valor
        lblTotalSalesCount.setText("📊 " + count);
        lblTotalAmount.setText("💰 " + String.format("%.2f €", total));
        lblTotalCash.setText("💵 " + String.format("%.2f €", cash));
        lblTotalCard.setText("💳 " + String.format("%.2f €", card));

        if (lblCount != null)
            lblCount.setText("🔍 " + count + " " + container.getBundle().getString("history.count_suffix"));
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
        lblSaleId.setText(container.getBundle().getString("history.detail.ticket") + " #"
                + String.format("%04d", sale.getSaleId()));

        boolean hasAnyReturned = sale.getDetails().stream().anyMatch(d -> d.getReturnedQuantity() > 0);

        if (sale.isReturn()) {
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
        // La opacidad y el color ahora se gestionan vía CSS (.btn-warning:disabled)

        lblSaleFullDate.setText(sale.getSaleDateTime().format(fullFormatter) + "\n"
                + container.getBundle().getString("receipt.attended_by") + ": " + sale.getUserName());

        String methodEmoji = container.getBundle().getString("payment.method.card")
                .equalsIgnoreCase(sale.getPaymentMethod()) ? "💳 " : "💵 ";
        lblPaymentMethod.setText(
                methodEmoji + (sale.getPaymentMethod().contains("Mixed") || sale.getPaymentMethod().contains("Mixto")
                        ? container.getBundle().getString("payment.method.mixed")
                        : sale.getPaymentMethod()));
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
                    "📦 " + detail.getQuantity() + " un. x " + String.format("%.2f", detail.getUnitPrice()) + " €");
            qtyLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #95a5a6;");
            nameBox.getChildren().addAll(nameLabel, qtyLabel);

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label priceLabel = new Label("💰 " + String.format("%.2f €", detail.getLineTotal()));
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
        Sale selected = salesTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtil.showWarning(container.getBundle().getString("alert.warning"),
                    container.getBundle().getString("history.error.no_selection"));
            return;
        }

        // 1. Garantizar que los detalles estén cargados
        if (selected.getDetails() == null || selected.getDetails().isEmpty()) {
            try {
                Sale fullSale = saleUseCase.getSaleDetails(selected.getSaleId());
                if (fullSale != null && fullSale.getDetails() != null) {
                    selected.setDetails(fullSale.getDetails());
                    // También actualizar clientId si no lo tiene (aunque debería)
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

        // 2. Abrir vista previa de impresión
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
                            p.setIva(detail.getIvaRate()); // Usar IVA histórico
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

        // Garantizar que los detalles estén cargados antes de abrir el diálogo
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

        // Calcular el máximo devolvible (productos aún no devueltos) usando el precio
        // real pagado
        double maxRefundable = selected.getDetails().stream()
                .mapToDouble(d -> (d.getQuantity() - d.getReturnedQuantity()) * (d.getLineTotal() / d.getQuantity()))
                .sum();

        if (maxRefundable > 0) {
            try {
                com.mycompany.ventacontrolfx.application.usecase.CashClosureUseCase closureUseCase = container
                        .getClosureUseCase();
                double cashInDrawer = closureUseCase.getCurrentCashInDrawer();

                // Calcular cuánto del reembolso total es OBLIGATORIAMENTE en efectivo (basado
                // en el pago original)
                double currentGrossTotal = selected.getTotal() + selected.getDiscountAmount();
                double cashRatio = (currentGrossTotal > 0) ? selected.getCashAmount() / currentGrossTotal : 1.0;
                double maxCashRefundNeeded = maxRefundable * cashRatio;

                if (maxCashRefundNeeded > cashInDrawer) {
                    String oldTicketHint = selected.getClosureId() != null
                            ? "\n\n💡 *Recuerda*: Este ticket es de una sesión antigua."
                            : "";

                    boolean continuar = AlertUtil.showConfirmation("⚠️ Efectivo limitado",
                            "Efectivo insuficiente para la parte de metálico",
                            String.format(
                                    "El efectivo en caja (%.2f €) es menor que la parte pagada en efectivo del ticket (%.2f €).\n\n"
                                            +
                                            "El sistema devolverá %.2f € a la tarjeta automaticamente, pero solo dispone de %.2f € "
                                            + "físicos para la parte de efectivo.%s\n\n"
                                            + "¿Deseas continuar con una devolución parcial o total sabiendo que el cajón quedará bajo?",
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
                    controller.init(selected);
                    controller.setOnSuccess((reason, items) -> {
                        try {
                            // Validación final de efectivo justo antes de confirmar
                            double refundTotal = selected.getDetails().stream()
                                    .filter(d -> items.containsKey(d.getDetailId()))
                                    .mapToDouble(d -> items.get(d.getDetailId()) * (d.getLineTotal() / d.getQuantity()))
                                    .sum();

                            try {
                                container.getClosureUseCase().validateCashAvailableForReturn(refundTotal);
                            } catch (SQLException cashEx) {
                                AlertUtil.showError("❌ Efectivo insuficiente", cashEx.getMessage());
                                return;
                            }

                            int userId = container.getUserSession().getCurrentUser().getUserId();
                            saleUseCase.registerPartialReturn(selected.getSaleId(), items, reason, userId);
                            AlertUtil.showInfo(container.getBundle().getString("alert.success"),
                                    container.getBundle().getString("history.success.return"));
                            loadSalesDirect();
                        } catch (SQLException e) {
                            e.printStackTrace();
                            AlertUtil.showError("Error", "No se pudo procesar la devolución: " + e.getMessage());
                        }
                    });
                });
    }
}
