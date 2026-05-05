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
    private Button btnReturn, btnPrint, btnCorrection, btnResendAeat;
    @FXML
    private HBox quickFilterContainer;
    @FXML
    private VBox skeletonTableContainer;
    @FXML
    private TableColumn<Sale, String> colFiscalStatus;

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

        // Verificación de permisos para Devoluciones y Subsanaciones
        boolean canReturn = container.getUserSession().hasPermission("venta.devolucion");
        btnReturn.setVisible(canReturn);
        btnReturn.setManaged(canReturn);

        boolean canCorrect = container.getUserSession().hasPermission("venta.subsanar");
        btnCorrection.setVisible(canCorrect);
        btnCorrection.setManaged(canCorrect);

        boolean canResend = container.getUserSession().hasPermission("fiscal.reenviar");
        if (btnResendAeat != null) {
            btnResendAeat.setVisible(canResend);
            btnResendAeat.setManaged(canResend);
        }
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("saleId"));
        colUser.setCellValueFactory(new PropertyValueFactory<>("userName"));
        colDate.setCellValueFactory(data -> {
            Sale sale = data.getValue();
            String formattedDate = sale.getSaleDateTime().format(fullFormatter);
            String status = sale.getFiscalStatus();
            String aeatEmoji = "ACCEPTED".equals(status) ? "✅ " : ("REJECTED".equals(status) ? "❌ " : ("PENDING".equals(status) ? "⏳ " : ""));
            return new SimpleStringProperty(aeatEmoji + formattedDate);
        });
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colMethod.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        colFiscalStatus.setCellValueFactory(new PropertyValueFactory<>("fiscalStatus"));

        colFiscalStatus.setCellFactory(col -> new TableCell<Sale, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label lbl = new Label();
                    lbl.setStyle("-fx-font-weight: bold; -fx-padding: 2 6; -fx-background-radius: 4; -fx-text-fill: white; -fx-font-size: 10px;");
                    
                    if ("ACCEPTED".equals(item)) {
                        lbl.setText("ACEPTADO");
                        lbl.setStyle(lbl.getStyle() + "-fx-background-color: #27ae60;");
                    } else if ("REJECTED".equals(item)) {
                        lbl.setText("ERROR");
                        lbl.setStyle(lbl.getStyle() + "-fx-background-color: #c0392b;");
                    } else {
                        lbl.setText("PENDIENTE");
                        lbl.setStyle(lbl.getStyle() + "-fx-background-color: #f39c12;");
                    }
                    setGraphic(lbl);
                }
            }
        });

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

        // Show skeletons for KPI cards and table
        showKpiSkeletons(true);
        showTableSkeletons(true);

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
            showKpiSkeletons(false);
            showTableSkeletons(false);
            updateSummaries((com.mycompany.ventacontrolfx.domain.model.HistoryStats) data[0]);
            paginationHelper.setData((List<Sale>) data[1]);
            handleCloseDetails();
        }, e -> {
            showKpiSkeletons(false);
            showTableSkeletons(false);
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("history.error.load") + ": " + e.getMessage());
        });
    }

    private void showTableSkeletons(boolean show) {
        if (skeletonTableContainer == null) return;
        
        if (show) {
            skeletonTableContainer.getChildren().clear();
            for (int i = 0; i < 15; i++) {
                skeletonTableContainer.getChildren().add(new com.mycompany.ventacontrolfx.component.SkeletonHistoryRow());
            }
            skeletonTableContainer.setVisible(true);
            skeletonTableContainer.setManaged(true);
        } else {
            skeletonTableContainer.setVisible(false);
            skeletonTableContainer.setManaged(false);
        }
    }

    private void showKpiSkeletons(boolean show) {
        if (show) {
            lblTotalSalesCount.setGraphic(new com.mycompany.ventacontrolfx.component.SkeletonStatCard());
            lblTotalSalesCount.setText("");
            lblTotalAmount.setGraphic(new com.mycompany.ventacontrolfx.component.SkeletonStatCard());
            lblTotalAmount.setText("");
            lblTotalCash.setGraphic(new com.mycompany.ventacontrolfx.component.SkeletonStatCard());
            lblTotalCash.setText("");
            lblTotalCard.setGraphic(new com.mycompany.ventacontrolfx.component.SkeletonStatCard());
            lblTotalCard.setText("");
        } else {
            lblTotalSalesCount.setGraphic(null);
            lblTotalAmount.setGraphic(null);
            lblTotalCash.setGraphic(null);
            lblTotalCard.setGraphic(null);
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
        
        // Control de visibilidad del botón de re-envío AEAT
        String fStatus = sale.getFiscalStatus();
        boolean needsResend = "PENDING".equals(fStatus) || "REJECTED".equals(fStatus);
        btnResendAeat.setVisible(needsResend);
        btnResendAeat.setManaged(needsResend);

        lblSaleFullDate.setText(sale.getSaleDateTime().format(fullFormatter) + "\n"
                + container.getBundle().getString("receipt.attended_by") + ": " + sale.getUserName() + "\n"
                + "Cliente: " + (sale.getCustomerNameSnapshot() != null ? sale.getCustomerNameSnapshot() : "Consumidor Final") + "\n"
                + "NIF: " + (sale.getCustomerNifSnapshot() != null ? sale.getCustomerNifSnapshot() : "---"));

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

        // 1. Añadir Status AEAT / VeriFactu
        if (fStatus != null && !fStatus.isEmpty()) {
            String textStatus = "PENDING".equals(fStatus) ? "⏳ Pendiente envío AEAT" : 
                               ("ACCEPTED".equals(fStatus) ? "✅ VeriFactu OK (" + (sale.getAeatSubmissionId() != null ? sale.getAeatSubmissionId() : "S/D") + ")" : 
                               ("REJECTED".equals(fStatus) ? "❌ Error de validación: " + sale.getFiscalMsg() : fStatus));
            String colorStatus = "PENDING".equals(fStatus) ? "#f39c12" : ("ACCEPTED".equals(fStatus) ? "#27ae60" : "#c0392b");
            Label lblFiscalStatus = new Label(textStatus);
            lblFiscalStatus.setStyle("-fx-text-fill: white; -fx-background-color: " + colorStatus + "; -fx-padding: 4 8; -fx-background-radius: 4; -fx-font-weight: bold; -fx-font-size: 11px;");
            detailsItemsContainer.getChildren().add(lblFiscalStatus);
        }

        // 2. Iterar items
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
                        // Priorizar los snapshots del ticket (datos fiscales reales enviados/corregidos)
                        String fiscalName = selected.getCustomerNameSnapshot();
                        String fiscalNif = selected.getCustomerNifSnapshot();
                        
                        if (fiscalNif != null && !fiscalNif.isEmpty()) {
                            com.mycompany.ventacontrolfx.domain.model.Client virtualClient = new com.mycompany.ventacontrolfx.domain.model.Client();
                            virtualClient.setName(fiscalName != null ? fiscalName : "Cliente");
                            virtualClient.setTaxId(fiscalNif);
                            
                            // Si existe el cliente real, intentamos traer la dirección (que no está en el snapshot)
                            if (selected.getClientId() != null) {
                                com.mycompany.ventacontrolfx.domain.model.Client realClient = container.getClientUseCase().getById(selected.getClientId());
                                if (realClient != null) {
                                    virtualClient.setAddress(realClient.getAddress());
                                    virtualClient.setPostalCode(realClient.getPostalCode());
                                    virtualClient.setCity(realClient.getCity());
                                    virtualClient.setProvince(realClient.getProvince());
                                }
                            }
                            ppc.setClientInfo(virtualClient);
                        } else if (selected.getClientId() != null) {
                            com.mycompany.ventacontrolfx.domain.model.Client client = container.getClientUseCase().getById(selected.getClientId());
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
    @FXML
    private void handleCorrection() {
        Sale selected = salesTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        // Solo permitir subsanar si ya fue aceptado o rechazado (si tiene hash)
        if (selected.getControlHash() == null) {
            AlertUtil.showWarning("Operación no permitida", "Solo se pueden subsanar tickets que ya tengan un registro fiscal generado.");
            return;
        }

        ModalService.showModal("/view/correction_dialog.fxml",
                "Subsanar Datos Fiscales", Modality.APPLICATION_MODAL,
                StageStyle.TRANSPARENT, container, (CorrectionDialogController controller) -> {
                    controller.init(selected);
                    controller.setOnSuccess((newName, newNif) -> {
                        try {
                            saleUseCase.registerCorrection(selected.getSaleId(), newName, newNif);
                            AlertUtil.showInfo("Corrección registrada", "Los datos han sido corregidos. El ticket se reenviará a la AEAT automáticamente.");
                            loadSalesDirect();
                        } catch (SQLException e) {
                            e.printStackTrace();
                            AlertUtil.showError("Error", "No se pudo registrar la corrección: " + e.getMessage());
                        }
                    });
                });
    }

    @FXML
    private void handleResendAeat() {
        Sale selected = salesTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        btnResendAeat.setDisable(true);
        container.getAsyncManager().runAsyncTask(() -> {
            try {
                return saleUseCase.resendToAeat(selected.getSaleId());
            } catch (Exception e) {
                return e.getMessage();
            }
        }, result -> {
            btnResendAeat.setDisable(false);
            if ("OK".equals(result)) {
                AlertUtil.showInfo("Éxito", "El ticket ha sido procesado por AEAT correctamente.");
                loadSalesDirect();
            } else if ("SOLICITADO".equals(result)) {
                AlertUtil.showInfo("Envío en curso", "El ticket ha sido puesto en cola. Se enviará a la AEAT en unos segundos.");
                loadSalesDirect();
            } else {
                AlertUtil.showError("Error AEAT", "No se pudo completar el envío: " + result);
                loadSalesDirect();
            }
        }, null);
    }
}
