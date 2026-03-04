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
                    setText(String.format("%.2f €", item));
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
                    String emoji = "Tarjeta".equalsIgnoreCase(item) ? "💳 " : "💵 ";
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

        btnReturn.setDisable(sale.isReturn());
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
            AlertUtil.showWarning("Aviso", "Por favor, seleccione un ticket primero.");
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
                AlertUtil.showError("Error", "No se pudieron cargar los detalles del ticket: " + e.getMessage());
                return;
            }
        }

        if (selected.getDetails() == null || selected.getDetails().isEmpty()) {
            AlertUtil.showWarning("Aviso", "Este ticket no tiene productos para mostrar.");
            return;
        }

        // 2. Abrir vista previa de impresión
        ModalService.showStandardModal("/view/print_preview.fxml",
                selected.getClientId() != null ? "Factura" : "Factura Simplificada", container,
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
                        AlertUtil.showError("Error", "Error al preparar la vista previa de impresión.");
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
                AlertUtil.showError("Error", "No se pudieron cargar los detalles del ticket: " + e.getMessage());
                return;
            }
        }

        if (selected.getDetails() == null || selected.getDetails().isEmpty()) {
            AlertUtil.showWarning("Aviso", "Este ticket no tiene productos registrados o ya han sido devueltos todos.");
            return;
        }

        // ── Validar efectivo disponible para devoluciones en efectivo ──────────
        boolean isCashSale = "Efectivo".equalsIgnoreCase(selected.getPaymentMethod());
        if (isCashSale) {
            // Calcular el máximo devolvible (productos aún no devueltos)
            double maxRefundable = selected.getDetails().stream()
                    .mapToDouble(d -> (d.getQuantity() - d.getReturnedQuantity()) * d.getUnitPrice())
                    .sum();

            if (maxRefundable > 0) {
                try {
                    com.mycompany.ventacontrolfx.application.usecase.CashClosureUseCase closureUseCase = container
                            .getClosureUseCase();
                    double cashAvailable = closureUseCase.getCurrentCashInDrawer();

                    if (cashAvailable <= 0) {
                        AlertUtil.showError("❌ Sin efectivo en caja",
                                String.format(
                                        "No hay efectivo disponible en la caja para procesar esta devolución.\n\n" +
                                                "💵 Efectivo en caja: %.2f €\n" +
                                                "💰 Importe máximo a devolver: %.2f €\n\n" +
                                                "Abre la caja con un fondo o verifica el saldo disponible.",
                                        cashAvailable, maxRefundable));
                        return;
                    }

                    if (maxRefundable > cashAvailable) {
                        // Advertir, pero dejar que el usuario elija cuánto devolver (puede ser parcial)
                        boolean continuar = AlertUtil.showConfirmation("⚠️ Efectivo limitado",
                                "Efectivo insuficiente para devolver el total",
                                String.format(
                                        "El efectivo en caja (%.2f €) es menor que el importe total del ticket (%.2f €).\n\n"
                                                +
                                                "Solo podrás devolver hasta %.2f € en efectivo.\n\n" +
                                                "¿Deseas continuar con una devolución parcial?",
                                        cashAvailable, maxRefundable, cashAvailable));
                        if (!continuar)
                            return;
                    }
                } catch (SQLException e) {
                    // Si no podemos leer el efectivo, dejar pasar (mejor UX que bloquear)
                    e.printStackTrace();
                }
            }
        }

        ModalService.showModal("/view/return_dialog.fxml", "Registrar Devolución", Modality.APPLICATION_MODAL,
                StageStyle.TRANSPARENT, container, (ReturnDialogController controller) -> {
                    controller.init(selected);
                    controller.setOnSuccess((reason, items) -> {
                        try {
                            // Validación final de efectivo justo antes de confirmar
                            if (isCashSale) {
                                double refundTotal = selected.getDetails().stream()
                                        .filter(d -> items.containsKey(d.getDetailId()))
                                        .mapToDouble(d -> items.get(d.getDetailId()) * d.getUnitPrice())
                                        .sum();

                                try {
                                    container.getClosureUseCase().validateCashAvailableForReturn(refundTotal);
                                } catch (SQLException cashEx) {
                                    AlertUtil.showError("❌ Efectivo insuficiente", cashEx.getMessage());
                                    return;
                                }
                            }

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
