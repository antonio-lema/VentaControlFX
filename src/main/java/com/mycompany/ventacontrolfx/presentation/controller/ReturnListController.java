package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.application.usecase.SaleUseCase;
import com.mycompany.ventacontrolfx.domain.model.Return;
import com.mycompany.ventacontrolfx.domain.model.ReturnDetail;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.layout.*;

import java.io.PrintWriter;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReturnListController implements Injectable {

    @FXML
    private DatePicker datePickerStart, datePickerEnd;
    @FXML
    private TextField txtSearch;
    @FXML
    private ComboBox<String> cmbPaymentMethod;
    @FXML
    private TableView<Return> returnsTable;
    @FXML
    private TableColumn<Return, Integer> colId, colSaleId, colClosure;
    @FXML
    private TableColumn<Return, String> colUser, colDate, colReason;
    @FXML
    private TableColumn<Return, Double> colAmount;
    @FXML
    private TableColumn<Return, Void> colActions;
    @FXML
    private Label lblCount, lblTotalRefunded;

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
    private ObservableList<Return> masterData = FXCollections.observableArrayList();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Override
    public void inject(ServiceContainer container) {
        this.saleUseCase = container.getSaleUseCase();

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
        loadReturns();

        // Listeners para búsqueda en tiempo real
        if (txtSearch != null)
            txtSearch.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
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

        colId.setCellValueFactory(new PropertyValueFactory<>("returnId"));
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
                    setStyle("");
                } else {
                    setText(String.format("%.2f €", item));
                    setTextFill(Color.web("#ef4444")); // text-danger
                    setStyle("-fx-font-weight: bold;");
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
                    if ("admin".equalsIgnoreCase(item) || "administrador".equalsIgnoreCase(item)) {
                        badge.setStyle(
                                "-fx-background-color: #ede9fe; -fx-text-fill: #7c3aed; -fx-padding: 2 8; -fx-background-radius: 10; -fx-font-size: 10; -fx-font-weight: bold;");
                    } else {
                        badge.setStyle(
                                "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-padding: 2 8; -fx-background-radius: 10; -fx-font-size: 10;");
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
                    setText(null);
                    setStyle("");
                } else if (item == null || item == 0) {
                    setText("PENDIENTE");
                    setTextFill(Color.web("#f59e0b"));
                    setStyle("-fx-font-weight: bold; -fx-font-size: 10;");
                } else {
                    setText("#" + item);
                    setTextFill(Color.BLACK);
                    setStyle("");
                }
            }
        });

        // Columna de Acciones (Ir al Ticket)
        if (colActions != null) {
            colActions.setCellFactory(col -> new TableCell<Return, Void>() {
                private final Button btn = new Button();
                {
                    btn.getStyleClass().add("btn-icon-only");
                    FontAwesomeIconView icon = new FontAwesomeIconView(FontAwesomeIcon.TICKET);
                    icon.setSize("14");
                    icon.setFill(Color.web("#6366f1"));
                    btn.setGraphic(icon);
                    btn.setTooltip(new Tooltip("Ver Ticket Original"));
                    btn.setOnAction(event -> {
                        Return r = getTableRow().getItem();
                        if (r != null)
                            handleViewTicket(r.getSaleId());
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    setGraphic(empty ? null : btn);
                }
            });
        }

        // Evento de doble clic para drill-down
        returnsTable.setRowFactory(tv -> {
            TableRow<Return> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    showReturnDetails(row.getItem());
                }
            });
            return row;
        });
    }

    private void handleViewTicket(int saleId) {
        AlertUtil.showInfo("Ver Ticket", "Abriendo detalles del Ticket #" + saleId + "...\n(Módulo de Auditoría)");
    }

    private void showReturnDetails(Return returnRecord) {
        if (returnRecord == null)
            return;
        try {
            // Cargar detalles desde el repositorio
            List<ReturnDetail> details = saleUseCase.getReturnDetails(returnRecord.getReturnId());
            returnRecord.setDetails(details);

            // Poblar el panel lateral
            if (lblDetailReturnId != null)
                lblDetailReturnId.setText("Devolución #" + returnRecord.getReturnId());
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

                    Separator sep = new Separator();
                    sep.setStyle("-fx-opacity: 0.3;");
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
            LocalDate start = (datePickerStart != null) ? datePickerStart.getValue() : LocalDate.now();
            LocalDate end = (datePickerEnd != null) ? datePickerEnd.getValue() : LocalDate.now();

            List<Return> returns = saleUseCase.getReturnsHistory(start, end);
            masterData.setAll(returns);
            applyFilters();
        } catch (SQLException e) {
            AlertUtil.showError("Error de Carga", "Error al obtener historial de devoluciones: " + e.getMessage());
        }
    }

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
        if (lblCount != null)
            lblCount.setText(list.size() + " registros");
        if (lblTotalRefunded != null) {
            double total = list.stream().mapToDouble(Return::getTotalRefunded).sum();
            lblTotalRefunded.setText(String.format("%.2f €", total));
        }
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

    @FXML
    private void handleExport() {
        if (masterData.isEmpty()) {
            AlertUtil.showWarning("Exportar", "No hay datos para exportar.");
            return;
        }

        try {
            String fileName = "Reporte_Devoluciones_" + LocalDate.now() + ".csv";
            try (PrintWriter writer = new PrintWriter(fileName, "UTF-8")) {
                // Header
                writer.println("ID;Ticket;Fecha;Usuario;Importe;Metodo Pago;Cierre;Motivo");

                for (Return r : returnsTable.getItems()) {
                    writer.printf("%d;%d;%s;%s;%.2f;%s;%s;\"%s\"\n",
                            r.getReturnId(),
                            r.getSaleId(),
                            (r.getReturnDatetime() != null) ? r.getReturnDatetime().format(formatter) : "-",
                            r.getUserName() != null ? r.getUserName() : "-",
                            r.getTotalRefunded(),
                            r.getPaymentMethod() != null ? r.getPaymentMethod() : "-",
                            r.getClosureId() != null && r.getClosureId() > 0 ? r.getClosureId() : "PENDIENTE",
                            r.getReason() != null ? r.getReason().replace("\"", "'") : "");
                }
            }
            AlertUtil.showInfo("Exportación Exitosa", "Reporte generado correctamente:\n" + fileName);
        } catch (Exception e) {
            AlertUtil.showError("Error de Exportación", "No se pudo generar el CSV: " + e.getMessage());
        }
    }
}
