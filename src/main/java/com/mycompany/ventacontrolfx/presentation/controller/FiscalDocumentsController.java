package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.application.usecase.EmitFiscalDocumentUseCase;
import com.mycompany.ventacontrolfx.application.usecase.QueryFiscalDocumentUseCase;
import com.mycompany.ventacontrolfx.domain.model.FiscalDocument;
import com.mycompany.ventacontrolfx.domain.model.FiscalDocument.Status;
import com.mycompany.ventacontrolfx.domain.model.FiscalDocument.Type;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import com.mycompany.ventacontrolfx.presentation.util.RealTimeSearchBinder;
import com.mycompany.ventacontrolfx.util.PaginationHelper;

public class FiscalDocumentsController implements Injectable {

    @FXML
    private Label lblTotalTickets, lblTotalInvoices, lblTotalReturns, lblTotalCancelled, lblTotalAmount;
    @FXML
    private DatePicker dpFrom, dpTo;
    @FXML
    private HBox quickFilterContainer;
    @FXML
    private ComboBox<String> cbType, cbStatus;
    @FXML
    private ComboBox<Integer> cmbRowLimit;
    @FXML
    private Label lblCount;
    @FXML
    private TextField txtSearch;

    @FXML
    private TableView<FiscalDocument> fiscalTable;
    @FXML
    private TableColumn<FiscalDocument, String> colDate, colRef, colType, colClient, colTotal, colStatus;

    private ServiceContainer container;
    private QueryFiscalDocumentUseCase queryUseCase;
    private EmitFiscalDocumentUseCase emitUseCase;
    private PaginationHelper<FiscalDocument> paginationHelper;
    private final ObservableList<FiscalDocument> masterList = FXCollections.observableArrayList();
    private final DateTimeFormatter dateTimeFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.queryUseCase = container.getQueryFiscalDocumentUseCase();
        this.emitUseCase = container.getEmitFiscalDocumentUseCase();

        setupTable();
        setupFilters();

        com.mycompany.ventacontrolfx.util.DateFilterUtils.addQuickFilters(quickFilterContainer, dpFrom, dpTo,
                container.getBundle(), this::applyFilters);

        paginationHelper = new PaginationHelper<>(fiscalTable, cmbRowLimit, lblCount, container.getBundle().getString("fiscal.doc.label.documentos"));
        loadData();
    }

    private void setupTable() {
        colDate.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getIssuedAt() != null ? data.getValue().getIssuedAt().format(dateTimeFmt) : "-"));

        colRef.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullReference()));

        colType.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDocType().name()));
        colType.setCellFactory(column -> new TableCell<FiscalDocument, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if ("TICKET".equals(item))
                        getStyleClass().add("text-primary");
                    else if ("FACTURA".equals(item))
                        getStyleClass().add("text-success");
                    else if ("RECTIFICATIVA".equals(item))
                        getStyleClass().add("text-danger");
                }
            }
        });

        colClient.setCellValueFactory(data -> {
            FiscalDocument doc = data.getValue();
            String name = doc.getReceiverName() != null ? doc.getReceiverName() : container.getBundle().getString("fiscal.doc.client.simplified");
            return new SimpleStringProperty(name);
        });

        colTotal.setCellValueFactory(
                data -> new SimpleStringProperty(String.format("%.2f \u20AC", data.getValue().getTotalAmount())));
        colTotal.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");

        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDocStatus().name()));
        colStatus.setCellFactory(column -> new TableCell<FiscalDocument, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(container.getBundle().getString("fiscal.doc.status." + item.toLowerCase()));
                    badge.getStyleClass().add("badge");
                    if ("EMITIDO".equals(item))
                        badge.getStyleClass().add("badge-success");
                    else if ("ANULADO".equals(item))
                        badge.getStyleClass().add("badge-danger");
                    setGraphic(badge);
                }
            }
        });

        // Eliminamos fiscalTable.setItems(masterList) ya que PaginationHelper lo
        // gestionar\u00c3\u00a1 internamente

        // Doble clic para abrir PDF
        fiscalTable.setRowFactory(tv -> {
            TableRow<FiscalDocument> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    FiscalDocument rowData = row.getItem();
                    openDocPdf(rowData);
                }
            });
            return row;
        });
    }

    private void openDocPdf(FiscalDocument doc) {
        try {
            String year = String.valueOf(doc.getIssuedAt().getYear());
            String month = String.format("%02d", doc.getIssuedAt().getMonthValue());

            String typeFolder;
            if (doc.getDocType() == Type.FACTURA)
                typeFolder = "Facturas";
            else if (doc.getDocType() == Type.RECTIFICATIVA)
                typeFolder = "Devoluciones";
            else
                typeFolder = "Tickets";

            String dirPath = "archivos_fiscales/" + year + "/" + month + "/" + typeFolder;
            String fileName = doc.getFullReference().replace("/", "_") + ".pdf";
            File file = new File(dirPath, fileName);

            if (!file.exists()) {
                AlertUtil.showWarning("Archivo no encontrado",
                        "No se encontr\u00c3\u00b3 el PDF en: " + file.getPath() +
                                "\nEs posible que se haya movido o que el documento sea antiguo.");
                return;
            }

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            } else {
                AlertUtil.showError(container.getBundle().getString("alert.error"), container.getBundle().getString("fiscal.doc.error.os_open"));
            }
        } catch (Exception e) {
            AlertUtil.showError(container.getBundle().getString("fiscal.doc.error.open_pdf"), e.getMessage());
        }
    }

    private void setupFilters() {
        cbType.setItems(FXCollections.observableArrayList(
                container.getBundle().getString("fiscal.doc.type.all"),
                "TICKET", "FACTURA", "RECTIFICATIVA"));
        cbType.getSelectionModel().selectFirst();

        cbStatus.setItems(FXCollections.observableArrayList(
                container.getBundle().getString("fiscal.doc.status.all"),
                "EMITIDO", "ANULADO"));
        cbStatus.getSelectionModel().selectFirst();

        dpFrom.setValue(LocalDate.now().minusDays(7));
        dpTo.setValue(LocalDate.now());

        RealTimeSearchBinder.bind(txtSearch, query -> applyFilters());
    }

    @FXML
    public void loadData() {
        try {
            // Cargar por defecto sin filtro (o con el filtro inicial de setupFilters)
            applyFilters();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateKPIs(List<FiscalDocument> docs) {
        long tickets = docs.stream().filter(d -> d.getDocType() == Type.TICKET).count();
        long invoices = docs.stream().filter(d -> d.getDocType() == Type.FACTURA).count();
        long returns = docs.stream().filter(d -> d.getDocType() == Type.RECTIFICATIVA).count();
        long cancelled = docs.stream().filter(d -> d.getDocStatus() == Status.ANULADO).count();

        // El total facturado neto suele ser Ventas - Devoluciones, pero aqu\u00c3\u00ad mostramos
        // el bruto emitido seg\u00c3\u00ban requisito.
        // Si el usuario prefiere restar devoluciones, se puede ajustar aqu\u00c3\u00ad.
        double total = docs.stream()
                .filter(d -> d.getDocStatus() == Status.EMITIDO)
                .mapToDouble(d -> d.getDocType() == Type.RECTIFICATIVA ? -d.getTotalAmount() : d.getTotalAmount())
                .sum();

        lblTotalTickets.setText(String.valueOf(tickets));
        lblTotalInvoices.setText(String.valueOf(invoices));
        if (lblTotalReturns != null)
            lblTotalReturns.setText(String.valueOf(returns));
        lblTotalCancelled.setText(String.valueOf(cancelled));
        lblTotalAmount.setText(String.format("%.2f \u20AC", total));
    }

    @FXML
    public void applyFilters() {
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();
        String type = cbType.getValue();
        String status = cbStatus.getValue();
        String search = txtSearch.getText().toLowerCase().trim();

        try {
            Status statusEnum = container.getBundle().getString("fiscal.doc.status.all").equals(status) ? null : Status.valueOf(status);
            String typeStr = container.getBundle().getString("fiscal.doc.type.all").equals(type) ? null : type;

            LocalDate finalFrom = (from == null) ? LocalDate.of(2000, 1, 1) : from;
            LocalDate finalTo = (to == null) ? LocalDate.of(2100, 1, 1) : to;

            List<FiscalDocument> docs = queryUseCase.search(finalFrom, finalTo, statusEnum, typeStr);
            masterList.setAll(docs); // Necesario para la exportaci\u00c3\u00b3n que usa masterList

            List<FiscalDocument> filtered = docs.stream()
                    .filter(d -> search.isEmpty() || d.getFullReference().toLowerCase().contains(search)
                            || (d.getReceiverName() != null && d.getReceiverName().toLowerCase().contains(search)))
                    .collect(Collectors.toList());

            paginationHelper.setData(filtered);
            updateKPIs(docs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void refresh() {
        loadData();
    }

    @FXML
    public void handleExportData(ActionEvent event) {
        if (masterList.isEmpty()) {
            AlertUtil.showWarning(container.getBundle().getString("alert.warning"), container.getBundle().getString("fiscal.doc.archive.not_found"));
            return;
        }

        StringBuilder csv = new StringBuilder();
        // Encabezado
        csv.append("Fecha;Referencia;Tipo;Receptor;CIF;Base;IVA;Total;Estado\n");

        for (FiscalDocument doc : masterList) {
            String dateStr = doc.getIssuedAt() != null ? doc.getIssuedAt().format(dateTimeFmt) : "-";
            String baseStr = String.format(Locale.getDefault(), "%.2f", doc.getBaseAmount());
            String vatStr = String.format(Locale.getDefault(), "%.2f", doc.getVatAmount());
            String totalStr = String.format(Locale.getDefault(), "%.2f", doc.getTotalAmount());

            csv.append("\"").append(dateStr).append("\";")
                    .append("\"").append(doc.getFullReference()).append("\";")
                    .append("\"").append(doc.getDocType()).append("\";")
                    .append("\"")
                    .append(doc.getReceiverName() != null ? doc.getReceiverName().replace("\"", "\"\"")
                            : "Venta Simplificada")
                    .append("\";")
                    .append("\"")
                    .append(doc.getReceiverTaxId() != null ? doc.getReceiverTaxId().replace("\"", "\"\"") : "")
                    .append("\";")
                    .append("\"").append(baseStr).append("\";")
                    .append("\"").append(vatStr).append("\";")
                    .append("\"").append(totalStr).append("\";")
                    .append("\"").append(doc.getDocStatus()).append("\"\n");
        }

        try {
            String fileName = "Reporte_Fiscal_" + LocalDate.now() + ".csv";
            java.io.File file = new java.io.File(System.getProperty("user.home") + "/Desktop/" + fileName);

            // Escribir con BOM para compatibilidad total con Excel en Windows
            byte[] bom = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
            byte[] content = csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);

            java.nio.file.Path path = file.toPath();
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            fos.write(bom);
            fos.write(content);
            fos.close();

            AlertUtil.showInfo(container.getBundle().getString("fiscal.doc.export.success"), 
                    String.format(container.getBundle().getString("fiscal.doc.export.success_msg"), fileName));
        } catch (Exception e) {
            AlertUtil.showError(container.getBundle().getString("fiscal.doc.error.export"), e.getMessage());
        }
    }

    @FXML
    public void handleOpenArchiveDir() {
        try {
            File folder = new File("archivos_fiscales");
            if (!folder.exists()) {
                AlertUtil.showWarning(container.getBundle().getString("fiscal.doc.archive.not_found"),
                        container.getBundle().getString("fiscal.doc.archive.not_found_msg"));
                return;
            }
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(folder);
            } else {
                AlertUtil.showError(container.getBundle().getString("alert.error"), container.getBundle().getString("fiscal.doc.error.os_open"));
            }
        } catch (IOException e) {
            AlertUtil.showError(container.getBundle().getString("alert.error"), e.getMessage());
        }
    }

}
