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
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;

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
    private Label lblTotalTickets, lblTotalInvoices, lblTotalCancelled, lblTotalAmount;
    @FXML
    private DatePicker dpFrom, dpTo;
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
    private TableColumn<FiscalDocument, String> colDate, colRef, colType, colClient, colTotal, colStatus, colActions;

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
        paginationHelper = new PaginationHelper<>(fiscalTable, cmbRowLimit, lblCount, "documentos");
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
                }
            }
        });

        colClient.setCellValueFactory(data -> {
            FiscalDocument doc = data.getValue();
            String name = doc.getReceiverName() != null ? doc.getReceiverName() : "VENTA GLOBAL / SIMPLIFICADA";
            return new SimpleStringProperty(name);
        });

        colTotal.setCellValueFactory(
                data -> new SimpleStringProperty(String.format("%.2f €", data.getValue().getTotalAmount())));
        colTotal.setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold;");

        colStatus.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDocStatus().name()));
        colStatus.setCellFactory(column -> new TableCell<FiscalDocument, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("badge");
                    if ("EMITIDO".equals(item))
                        badge.getStyleClass().add("badge-success");
                    else if ("ANULADO".equals(item))
                        badge.getStyleClass().add("badge-danger");
                    setGraphic(badge);
                }
            }
        });

        colActions.setCellFactory(column -> new TableCell<FiscalDocument, String>() {
            private final Button btnPrint = new Button();
            private final Button btnCancel = new Button();
            private final HBox container = new HBox(8, btnPrint, btnCancel);

            {
                btnPrint.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.PRINT));
                btnPrint.getStyleClass().addAll("btn-icon", "btn-history-print");
                btnPrint.setTooltip(new Tooltip("Reimprimir Documento"));
                btnPrint.setOnAction(e -> handlePrint(getTableView().getItems().get(getIndex())));

                btnCancel.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.BAN));
                btnCancel.getStyleClass().addAll("btn-icon", "btn-danger-light");
                btnCancel.setTooltip(new Tooltip("Anular Documento"));
                btnCancel.setOnAction(e -> handleCancel(getTableView().getItems().get(getIndex())));

                container.setStyle("-fx-alignment: CENTER;");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    FiscalDocument doc = getTableView().getItems().get(getIndex());
                    btnCancel.setDisable(doc.getDocStatus() != Status.EMITIDO);
                    setGraphic(container);
                }
            }
        });

        // Eliminamos fiscalTable.setItems(masterList) ya que PaginationHelper lo
        // gestionará internamente

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
            else
                typeFolder = "Tickets";

            String dirPath = "archivos_fiscales/" + year + "/" + month + "/" + typeFolder;
            String fileName = doc.getFullReference().replace("/", "_") + ".pdf";
            File file = new File(dirPath, fileName);

            if (!file.exists()) {
                AlertUtil.showWarning("Archivo no encontrado",
                        "No se encontró el PDF en: " + file.getPath() +
                                "\nEs posible que se haya movido o que el documento sea antiguo.");
                return;
            }

            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file);
            } else {
                AlertUtil.showError("Error", "Tu sistema no permite abrir archivos automáticamente.");
            }
        } catch (Exception e) {
            AlertUtil.showError("Error al abrir PDF", e.getMessage());
        }
    }

    private void setupFilters() {
        cbType.setItems(FXCollections.observableArrayList("Todos los tipos", "TICKET", "FACTURA"));
        cbType.getSelectionModel().selectFirst();

        cbStatus.setItems(FXCollections.observableArrayList("Todos los estados", "EMITIDO", "ANULADO"));
        cbStatus.getSelectionModel().selectFirst();

        dpFrom.setValue(LocalDate.now().minusDays(7));
        dpTo.setValue(LocalDate.now());

        RealTimeSearchBinder.bind(txtSearch, query -> applyFilters());
    }

    @FXML
    public void loadData() {
        try {
            List<FiscalDocument> docs = queryUseCase.search(null, null, null, null);
            masterList.setAll(docs);
            paginationHelper.setData(docs);
            updateKPIs(docs);
        } catch (SQLException e) {
            e.printStackTrace();
            AlertUtil.showError("Error al cargar datos", e.getMessage());
        }
    }

    private void updateKPIs(List<FiscalDocument> docs) {
        long tickets = docs.stream().filter(d -> d.getDocType() == Type.TICKET).count();
        long invoices = docs.stream().filter(d -> d.getDocType() == Type.FACTURA).count();
        long cancelled = docs.stream().filter(d -> d.getDocStatus() == Status.ANULADO).count();
        double total = docs.stream()
                .filter(d -> d.getDocStatus() == Status.EMITIDO)
                .mapToDouble(FiscalDocument::getTotalAmount)
                .sum();

        lblTotalTickets.setText(String.valueOf(tickets));
        lblTotalInvoices.setText(String.valueOf(invoices));
        lblTotalCancelled.setText(String.valueOf(cancelled));
        lblTotalAmount.setText(String.format("%.2f €", total));
    }

    @FXML
    public void applyFilters() {
        LocalDate from = dpFrom.getValue();
        LocalDate to = dpTo.getValue();
        String type = cbType.getValue();
        String status = cbStatus.getValue();
        String search = txtSearch.getText().toLowerCase().trim();

        try {
            Status statusEnum = "Todos los estados".equals(status) ? null : Status.valueOf(status);
            String typeStr = "Todos los tipos".equals(type) ? null : type;

            List<FiscalDocument> filtered = queryUseCase.search(from, to, statusEnum, typeStr).stream()
                    .filter(d -> search.isEmpty() || d.getFullReference().toLowerCase().contains(search)
                            || (d.getReceiverName() != null && d.getReceiverName().toLowerCase().contains(search)))
                    .collect(Collectors.toList());

            paginationHelper.setData(filtered);
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
            AlertUtil.showWarning("Sin datos", "No hay documentos para exportar en la vista actual.");
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

            AlertUtil.showInfo("Exportación Exitosa", "Se ha guardado el archivo " + fileName + " en tu escritorio.");
        } catch (Exception e) {
            AlertUtil.showError("Error al exportar", "No se pudo crear el archivo: " + e.getMessage());
        }
    }

    @FXML
    public void handleOpenArchiveDir() {
        try {
            File folder = new File("archivos_fiscales");
            if (!folder.exists()) {
                AlertUtil.showWarning("Carpeta no encontrada",
                        "La carpeta de archivos aún no se ha creado. Se creará automáticamente cuando emitas tu primer documento fiscal tras una venta.");
                return;
            }
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(folder);
            } else {
                AlertUtil.showError("Error", "Tu sistema no permite abrir carpetas automáticamente.");
            }
        } catch (IOException e) {
            AlertUtil.showError("Error", "No se pudo abrir la carpeta: " + e.getMessage());
        }
    }

    private void handlePrint(FiscalDocument doc) {
        try {
            QueryFiscalDocumentUseCase.PrintData data = queryUseCase.getDataForReprint(doc.getSaleId());
            // Aquí llamaríamos al servicio de impresión o generador de PDF
            AlertUtil.showInfo("Reimpresión", "Generando duplicado de " + doc.getFullReference() + "...");
        } catch (SQLException e) {
            AlertUtil.showError("Error", "No se pudo recuperar los datos para reimprimir.");
        }
    }

    private void handleCancel(FiscalDocument doc) {
        if (!AlertUtil.showConfirmation("Confirmar Anulación", "Anular Documento",
                "¿Está seguro de que desea anular la " + doc.getDocType()
                        + " " + doc.getFullReference()
                        + "? \n\nEsto no revertirá el stock ni el dinero en caja, solo invalidará el documento fiscal.")) {
            return;
        }

        try {
            emitUseCase.cancelDocument(doc.getSaleId());
            AlertUtil.showToast("Documento " + doc.getFullReference() + " anulado correctamente.");
            loadData();
        } catch (SQLException e) {
            AlertUtil.showError("Error al anular", e.getMessage());
        }
    }
}
