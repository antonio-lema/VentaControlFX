package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.application.usecase.SaleUseCase;
import com.mycompany.ventacontrolfx.domain.model.Return;
import com.mycompany.ventacontrolfx.domain.model.ReturnDetail;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.DateFilterUtils;
import com.mycompany.ventacontrolfx.util.ModalService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import com.mycompany.ventacontrolfx.presentation.util.RealTimeSearchBinder;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReturnListController implements Injectable {

    @FXML
    private DatePicker datePickerStart, datePickerEnd;
    @FXML
    private HBox quickFilterContainer;
    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<String> cmbPaymentMethod;
    @FXML
    private TableView<Return> returnsTable;
    @FXML
    private TableColumn<Return, String> colId;
    @FXML
    private TableColumn<Return, Integer> colSaleId, colClosure;
    @FXML
    private TableColumn<Return, String> colUser, colDate, colReason;
    @FXML
    private TableColumn<Return, Double> colAmount;
    @FXML
    private Label lblCount, lblTotalRefunded, lblKpiTotal, lblKpiCount, lblKpiAverage, lblKpiLast;

    // View Details Side Panel
    @FXML
    private VBox detailsPanel;
    @FXML
    private Label lblDetailReturnId;
    @FXML
    private Label lblDetailSaleId;
    @FXML
    private Label lblDetailDate;
    @FXML
    private Label lblDetailReason;
    @FXML
    private Label lblDetailUser;
    @FXML
    private Label lblDetailTotal;
    @FXML
    private VBox detailsItemsContainer;

    private SaleUseCase saleUseCase;
    private com.mycompany.ventacontrolfx.application.usecase.GetSaleTicketUseCase getSaleTicketUseCase;
    private ServiceContainer container;
    private ObservableList<Return> masterData = FXCollections.observableArrayList();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final DateTimeFormatter kpiFormatter = DateTimeFormatter.ofPattern("dd/MM/yy HH:mm");

    private Return currentSelectedReturn;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.saleUseCase = container.getSaleUseCase();
        this.getSaleTicketUseCase = container.getGetSaleTicketUseCase();

        // Inicializar filtros
        if (datePickerStart != null)
            datePickerStart.setValue(LocalDate.now());
        if (datePickerEnd != null)
            datePickerEnd.setValue(LocalDate.now());

        if (cmbPaymentMethod != null) {
            cmbPaymentMethod.setItems(FXCollections.observableArrayList("Todos", "Efectivo", "Tarjeta"));
            cmbPaymentMethod.setValue("Todos");
        }

        setupTable();

        DateFilterUtils.addQuickFilters(quickFilterContainer, datePickerStart,
                datePickerEnd, this::loadReturns);

        loadReturns();

        // Listeners para búsqueda en tiempo real
        if (txtSearch != null)
            RealTimeSearchBinder.bind(txtSearch, query -> applyFilters());
        if (datePickerStart != null)
            datePickerStart.valueProperty().addListener((obs, oldVal, newVal) -> loadReturns());
        if (datePickerEnd != null)
            datePickerEnd.valueProperty().addListener((obs, oldVal, newVal) -> loadReturns());
        if (cmbPaymentMethod != null)
            cmbPaymentMethod.valueProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    private void setupTable() {
        if (returnsTable == null)
            return;

        colId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullReference()));
        colSaleId.setCellValueFactory(new PropertyValueFactory<>("saleId"));
        colReason.setCellValueFactory(new PropertyValueFactory<>("reason"));
        colClosure.setCellValueFactory(new PropertyValueFactory<>("closureId"));

        // Formato de fecha con seguridad ante nulos
        colDate.setCellValueFactory(data -> {
            if (data.getValue() == null || data.getValue().getReturnDatetime() == null) {
                return new SimpleStringProperty("-");
            }
            return new SimpleStringProperty(data.getValue().getReturnDatetime().format(formatter));
        });

        // Formato de importe
        colAmount.setCellValueFactory(new PropertyValueFactory<>("totalRefunded"));
        colAmount.setCellFactory(col -> new TableCell<Return, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    getStyleClass().remove("text-danger");
                } else {
                    setText(String.format("%.2f €", item));
                    getStyleClass().add("text-danger");
                    setStyle("-fx-font-weight: 800;");
                }
            }
        });

        // Badges para Usuarios
        colUser.setCellValueFactory(new PropertyValueFactory<>("userName"));
        colUser.setCellFactory(col -> new TableCell<Return, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item.toUpperCase());
                    badge.getStyleClass().add("badge-info"); // Clase estándar
                    if ("admin".equalsIgnoreCase(item) || "administrador".equalsIgnoreCase(item)) {
                        badge.setStyle(
                                "-fx-background-color: -fx-custom-color-primary-bg; -fx-text-fill: -fx-custom-color-primary;");
                    }
                    setGraphic(badge);
                }
            }
        });

        // Tooltips para Motivo
        colReason.setCellFactory(col -> new TableCell<Return, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    Tooltip tooltip = new Tooltip(item);
                    tooltip.setWrapText(true);
                    tooltip.setMaxWidth(300);
                    setTooltip(tooltip);
                }
            }
        });

        // Indicador de Cierre / Pendiente
        colClosure.setCellFactory(col -> new TableCell<Return, Integer>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else if (item == null || item == 0) {
                    Label badge = new Label("PENDIENTE");
                    badge.getStyleClass().add("badge-warning");
                    setGraphic(badge);
                    setText(null);
                } else {
                    setText("#" + item);
                    setTextFill(Color.web("#64748b"));
                    setGraphic(null);
                }
            }
        });

        returnsTable.setRowFactory(tv -> {
            TableRow<Return> row = new TableRow<>();
            row.setOnMouseClicked((MouseEvent event) -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    showReturnDetails(row.getItem());
                }
            });
            return row;
        });
    }

    private void handleViewTicket(int saleId) {
        try {
            com.mycompany.ventacontrolfx.domain.model.Sale sale = getSaleTicketUseCase.execute(saleId);
            if (sale == null)
                return;

            ModalService.showStandardModal("/view/view_ticket_modal.fxml", "Detalle de Ticket #" + saleId, container,
                    (TicketDetailController controller) -> {
                        controller.setSale(sale);
                    });
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Error", "No se pudo cargar el ticket.");
        }
    }

    private void showReturnDetails(Return returnRecord) {
        if (returnRecord == null)
            return;

        this.currentSelectedReturn = returnRecord;

        try {
            // Cargar detalles desde el repositorio
            List<ReturnDetail> details = saleUseCase.getReturnDetails(returnRecord.getReturnId());
            returnRecord.setDetails(details);

            // Poblar el panel lateral
            if (lblDetailReturnId != null)
                lblDetailReturnId.setText("Devolución: " + returnRecord.getFullReference());
            if (lblDetailSaleId != null)
                lblDetailSaleId.setText("Ticket #" + returnRecord.getSaleId());

            if (lblDetailDate != null) {
                if (returnRecord.getReturnDatetime() != null) {
                    lblDetailDate.setText(returnRecord.getReturnDatetime().format(formatter));
                } else {
                    lblDetailDate.setText("-");
                }
            }

            if (lblDetailReason != null)
                lblDetailReason.setText(returnRecord.getReason() != null ? returnRecord.getReason() : "-");
            if (lblDetailUser != null)
                lblDetailUser.setText(returnRecord.getUserName() != null ? returnRecord.getUserName() : "-");
            if (lblDetailTotal != null)
                lblDetailTotal.setText(String.format("%.2f €", returnRecord.getTotalRefunded()));

            if (detailsItemsContainer != null) {
                detailsItemsContainer.getChildren().clear();
                for (ReturnDetail detail : details) {
                    VBox itemBox = new VBox(2);
                    itemBox.setStyle("-fx-background-color: transparent;");

                    HBox topRow = new HBox();
                    Label nameLabel = new Label(detail.getProductName() != null ? detail.getProductName()
                            : "Producto ID: " + detail.getProductId());
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-font-size: 13px;");

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);

                    Label subtotalLabel = new Label(String.format("%.2f €", detail.getSubtotal()));
                    subtotalLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");

                    topRow.getChildren().addAll(nameLabel, spacer, subtotalLabel);

                    HBox bottomRow = new HBox();
                    Label qtyLabel = new Label(
                            detail.getQuantity() + " x " + String.format("%.2f €", detail.getUnitPrice()));
                    qtyLabel.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
                    bottomRow.getChildren().add(qtyLabel);

                    itemBox.getChildren().addAll(topRow, bottomRow);
                    detailsItemsContainer.getChildren().add(itemBox);

                    // Separador entre productos
                    Separator sep = new Separator();
                    sep.setStyle("-fx-background-color: #f1f5f9; -fx-opacity: 0.5;");
                    detailsItemsContainer.getChildren().add(sep);
                }
            }

            if (detailsPanel != null) {
                detailsPanel.setVisible(true);
                detailsPanel.setManaged(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Error de Apertura", "No se pudo cargar los detalles: " + e.getMessage());
        }
    }

    @FXML
    private void handleCloseDetails() {
        if (detailsPanel != null) {
            detailsPanel.setVisible(false);
            detailsPanel.setManaged(false);
        }
    }

    @FXML
    public void loadReturns() {
        try {
            LocalDate start = (datePickerStart != null) ? datePickerStart.getValue() : null;
            LocalDate end = (datePickerEnd != null) ? datePickerEnd.getValue() : null;

            List<Return> returns;
            if (start == null || end == null) {
                returns = saleUseCase.getReturnsHistory(LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1));
            } else {
                returns = saleUseCase.getReturnsHistory(start, end);
            }
            masterData.setAll(returns);
            applyFilters();
        } catch (SQLException e) {
            AlertUtil.showError("Error de Carga", "Error al obtener historial de devoluciones: " + e.getMessage());
        }
    }

    @FXML
    private void applyFilters() {
        if (txtSearch == null)
            return;

        String filterText = txtSearch.getText().toLowerCase().trim();
        String paymentFilter = (cmbPaymentMethod != null) ? cmbPaymentMethod.getValue() : "Todos";

        FilteredList<Return> filteredData = new FilteredList<>(masterData, r -> {
            // Filtro por texto
            boolean matchesText = true;
            if (!filterText.isEmpty()) {
                String reason = (r.getReason() != null) ? r.getReason().toLowerCase() : "";
                String user = (r.getUserName() != null) ? r.getUserName().toLowerCase() : "";
                String ticket = String.valueOf(r.getSaleId());
                matchesText = reason.contains(filterText) || user.contains(filterText) || ticket.contains(filterText);
            }

            // Filtro por método de pago
            boolean matchesPayment = true;
            if (paymentFilter != null && !"Todos".equals(paymentFilter)) {
                matchesPayment = paymentFilter.equalsIgnoreCase(r.getPaymentMethod());
            }

            return matchesText && matchesPayment;
        });

        returnsTable.setItems(filteredData);
        updateSummary(filteredData);
    }

    private void updateSummary(List<Return> list) {
        int count = list.size();
        double total = list.stream().mapToDouble(Return::getTotalRefunded).sum();
        double average = count > 0 ? (total / count) : 0;

        String lastReturnDate = "-";
        if (count > 0) {
            Return lastReturn = list.stream()
                    .filter(r -> r.getReturnDatetime() != null)
                    .max((r1, r2) -> r1.getReturnDatetime().compareTo(r2.getReturnDatetime()))
                    .orElse(null);
            if (lastReturn != null) {
                lastReturnDate = lastReturn.getReturnDatetime().format(kpiFormatter);
            }
        }

        if (lblCount != null)
            lblCount.setText(count + " registros");
        if (lblTotalRefunded != null)
            lblTotalRefunded.setText(String.format("%.2f €", total));

        if (lblKpiTotal != null)
            lblKpiTotal.setText(String.format("%.2f €", total));
        if (lblKpiCount != null)
            lblKpiCount.setText(String.valueOf(count));
        if (lblKpiAverage != null)
            lblKpiAverage.setText(String.format("%.2f €", average));
        if (lblKpiLast != null)
            lblKpiLast.setText(lastReturnDate);
    }

    @FXML
    private void handleClearFilters() {
        if (txtSearch != null)
            txtSearch.clear();
        if (cmbPaymentMethod != null)
            cmbPaymentMethod.setValue("Todos");
        if (datePickerStart != null)
            datePickerStart.setValue(LocalDate.now());
        if (datePickerEnd != null)
            datePickerEnd.setValue(LocalDate.now());
        loadReturns();
    }

    // Filtros Rápidos
    @FXML
    private void handleFilterToday() {
        if (datePickerStart != null)
            datePickerStart.setValue(LocalDate.now());
        if (datePickerEnd != null)
            datePickerEnd.setValue(LocalDate.now());
    }

    @FXML
    private void handleFilter7Days() {
        if (datePickerStart != null)
            datePickerStart.setValue(LocalDate.now().minusDays(7));
        if (datePickerEnd != null)
            datePickerEnd.setValue(LocalDate.now());
    }

    @FXML
    private void handleFilter30Days() {
        if (datePickerStart != null)
            datePickerStart.setValue(LocalDate.now().minusDays(30));
        if (datePickerEnd != null)
            datePickerEnd.setValue(LocalDate.now());
    }

    // Panel de Detalles - Acciones
    @FXML
    private void handleReprint() {
        if (currentSelectedReturn == null) {
            AlertUtil.showWarning("Aviso", "Por favor, seleccione una devolución primero.");
            return;
        }

        try {
            // 1. Garantizar detalles cargados
            if (currentSelectedReturn.getDetails() == null || currentSelectedReturn.getDetails().isEmpty()) {
                currentSelectedReturn.setDetails(saleUseCase.getReturnDetails(currentSelectedReturn.getReturnId()));
            }

            // 2. Obtener la venta original para ver si tiene cliente asociado
            com.mycompany.ventacontrolfx.domain.model.Sale originalSale = saleUseCase
                    .getSaleDetails(currentSelectedReturn.getSaleId());

            // 3. Abrir vista previa especializada en Devoluciones (Factura Rectificativa)
            ModalService.showStandardModal("/view/print_preview.fxml", "Factura Rectificativa", container,
                    (PrintPreviewController ppc) -> {
                        ppc.setReturnData(currentSelectedReturn, originalSale, currentSelectedReturn.getDetails());
                    });

        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Error", "No se pudieron obtener los datos de la devolución: " + e.getMessage());
        }
    }

    @FXML
    private void handleViewOriginalTicket() {
        if (currentSelectedReturn == null)
            return;
        handleViewTicket(currentSelectedReturn.getSaleId());
    }

}
