package com.mycompany.ventacontrolfx.presentation.controller.dialog;

import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.infrastructure.external.aeat.JdbcVerifactuRepository;
import com.mycompany.ventacontrolfx.presentation.model.FiscalOperationModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import java.util.List;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import java.io.StringReader;
import java.io.StringWriter;

public class VerifactuDashboardController implements Injectable {

    @FXML
    private TableView<FiscalOperationModel> operationsTable;
    @FXML
    private TableColumn<FiscalOperationModel, String> colDate;
    @FXML
    private TableColumn<FiscalOperationModel, String> colDoc;
    @FXML
    private TableColumn<FiscalOperationModel, String> colType;
    @FXML
    private TableColumn<FiscalOperationModel, Double> colTotal;
    @FXML
    private TableColumn<FiscalOperationModel, String> colStatus;

    @FXML
    private Label lblDetailDoc;
    @FXML
    private Label lblDetailStatus;
    @FXML
    private Label lblDetailError;
    @FXML
    private WebView webXmlSent;
    @FXML
    private WebView webXmlReceived;
    @FXML
    private VBox detailPanel;
    @FXML
    private Button btnRetry;
    @FXML
    private Button btnSubsanar;

    @FXML
    private TableView<com.mycompany.ventacontrolfx.presentation.model.FiscalLogModel> historyTable;
    @FXML
    private TableColumn<com.mycompany.ventacontrolfx.presentation.model.FiscalLogModel, String> colHistDate;
    @FXML
    private TableColumn<com.mycompany.ventacontrolfx.presentation.model.FiscalLogModel, String> colHistStatus;
    @FXML
    private TableColumn<com.mycompany.ventacontrolfx.presentation.model.FiscalLogModel, String> colHistMsg;

    @FXML
    private VBox paneXmlSent;
    @FXML
    private VBox paneXmlReceived;
    @FXML
    private VBox paneHistory;
    @FXML
    private Button btnTabSent;
    @FXML
    private Button btnTabReceived;
    @FXML
    private Button btnTabHistory;

    @FXML
    private javafx.scene.layout.HBox kpiContainer;
    @FXML
    private javafx.scene.layout.HBox skeletonKpi;
    @FXML
    private Label lblTotalOps;
    @FXML
    private Label lblTotalAccepted;
    @FXML
    private Label lblTotalRejected;

    private final JdbcVerifactuRepository repository = new JdbcVerifactuRepository();
    private ObservableList<FiscalOperationModel> masterData = FXCollections.observableArrayList();
    private ServiceContainer container;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        // La migraci\u00f3n ahora se hace en ServiceContainer al arrancar
        setupTable();
        loadData();
    }

    private void setupTable() {
        colDate.setCellValueFactory(cellData -> cellData.getValue().dateProperty());
        colDoc.setCellValueFactory(cellData -> cellData.getValue().documentProperty());
        colType.setCellValueFactory(cellData -> cellData.getValue().typeProperty());
        colTotal.setCellValueFactory(cellData -> cellData.getValue().totalProperty().asObject());
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

        // History Table Setup
        colHistDate.setCellValueFactory(cellData -> cellData.getValue().dateProperty());
        colHistStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        colHistMsg.setCellValueFactory(cellData -> cellData.getValue().messageProperty());

        operationsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            showDetails(newVal);
        });

        // Cell factory for status to show colors
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.equals("ACCEPTED")) {
                        setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                    } else if (item.equals("REJECTED") || item.equals("ERROR")) {
                        setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    } else if (item.equals("NON_FISCAL") || item.equals("LEGACY")) {
                        setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
                    } else if (item.equals("VOID_PENDING")) {
                        setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Cell factory for Type to distinguish operations
        colType.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("");
                } else {
                    FiscalOperationModel row = getTableRow().getItem();
                    String displayType = item;

                    // Si el documento está anulado internamente, lo mostramos como ANULADA
                    if (row != null && "VOIDED".equals(row.getDocStatus())) {
                        displayType = "ANULADA";
                    }

                    setText(displayType);

                    if (displayType.equals("RECTIFICATIVA")) {
                        setStyle(
                                "-fx-text-fill: #8b5cf6; -fx-font-weight: bold; -fx-background-color: #f5f3ff; -fx-background-radius: 4; -fx-alignment: center;");
                    } else if (displayType.equals("ANULACION") || displayType.equals("ANULADA")) {
                        setStyle(
                                "-fx-text-fill: #f97316; -fx-font-weight: bold; -fx-background-color: #fff7ed; -fx-background-radius: 4; -fx-alignment: center;");
                    } else {
                        setStyle(
                                "-fx-text-fill: #3b82f6; -fx-font-weight: bold; -fx-background-color: #eff6ff; -fx-background-radius: 4; -fx-alignment: center;");
                    }
                }
            }
        });
    }

    private void loadData() {
        // Show Skeletons
        kpiContainer.setVisible(false);
        kpiContainer.setManaged(false);
        skeletonKpi.setVisible(true);
        skeletonKpi.setManaged(true);

        // Async-like pause for effect and to allow UI to breathe
        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.millis(600));
        pause.setOnFinished(e -> {
            masterData.setAll(repository.getRecentFiscalOperations(50));
            operationsTable.setItems(masterData);

            // Update KPIs
            int[] summary = repository.getFiscalSummary();
            lblTotalOps.setText(String.valueOf(summary[0]));
            lblTotalAccepted.setText(String.valueOf(summary[1]));
            lblTotalRejected.setText(String.valueOf(summary[2]));

            // Hide Skeletons
            skeletonKpi.setVisible(false);
            skeletonKpi.setManaged(false);
            kpiContainer.setVisible(true);
            kpiContainer.setManaged(true);
        });
        pause.play();
    }

    private void showDetails(FiscalOperationModel op) {
        if (op == null) {
            detailPanel.setVisible(false);
            return;
        }

        detailPanel.setVisible(true);
        lblDetailDoc.setText(op.getDocument());
        lblDetailStatus.setText(op.getStatus());
        lblDetailError.setText(op.getError() != null ? op.getError() : "-");

        // Mostrar botones de reintento/subsanaci\u00f3n si est\u00e1 rechazado o hay
        // error
        String status = op.getStatus();
        boolean canRetry = "REJECTED".equals(status) || "ERROR".equals(status) || "PENDING".equals(status);
        btnRetry.setVisible(canRetry);
        btnSubsanar.setVisible(canRetry && (op.getType().equals("ALTA") || op.getType().equals("RECTIFICATIVA")));

        loadXmlIntoWebView(webXmlSent, op.getXmlSent());
        loadXmlIntoWebView(webXmlReceived, op.getXmlReceived());

        // Cargar historial
        historyTable.setItems(javafx.collections.FXCollections
                .observableArrayList(repository.getFiscalHistory(op.getType(), op.getId())));
    }

    private void loadXmlIntoWebView(WebView webView, String xml) {
        String formatted = formatXml(xml);
        String html = highlightXml(formatted);
        webView.getEngine().loadContent(html);
    }

    private String highlightXml(String xml) {
        if (xml == null || xml.startsWith("No hay")) {
            return "<html><body style='background-color:#1e293b; color:#94a3b8; font-family:sans-serif; padding:20px;'><i>"
                    + xml + "</i></body></html>";
        }

        // Escape HTML
        String escaped = xml.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");

        // Simple Regex Highlighting
        // Tags: &lt;(/?)(\w+)(.*?)&gt;
        String highlighted = escaped.replaceAll("&lt;(/?)(\\w+)(.*?)&gt;",
                "<span style='color:#569cd6;'>&lt;$1$2</span><span style='color:#9cdcfe;'>$3</span><span style='color:#569cd6;'>&gt;</span>");

        // Attributes: (\w+)=&quot;(.*?)&quot;
        highlighted = highlighted.replaceAll("(\\w+)=&quot;(.*?)&quot;",
                "<span style='color:#9cdcfe;'>$1</span>=<span style='color:#ce9178;'>&quot;$2&quot;</span>");

        return "<html><body style='background-color:#1e1e1e; color:#d4d4d4; font-family:Consolas,monospace; font-size:12px; margin:0; padding:15px; white-space:pre;'>"
                +
                highlighted +
                "</body></html>";
    }

    private String formatXml(String xml) {
        if (xml == null || xml.isEmpty())
            return "No hay datos XML registrados.";
        try {
            Source xmlInput = new StreamSource(new StringReader(xml));
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", 4);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(xmlInput, xmlOutput);
            return xmlOutput.getWriter().toString();
        } catch (Exception e) {
            return xml.replace("><", ">\n<");
        }
    }

    @FXML
    private void handleVoid() {
        FiscalOperationModel selected = operationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            com.mycompany.ventacontrolfx.presentation.util.AlertUtil.showWarning("Selecci\u00f3n necesaria",
                    "Por favor, selecciona una operaci\u00f3n para anular.");
            return;
        }

        if (!selected.getType().equals("ALTA") && !selected.getType().equals("RECTIFICATIVA")) {
            com.mycompany.ventacontrolfx.presentation.util.AlertUtil.showError("Error",
                    "S\u00f3lo se pueden anular facturas de tipo ALTA o RECTIFICATIVA.");
            return;
        }

        boolean confirm = com.mycompany.ventacontrolfx.presentation.util.AlertUtil
                .showConfirmation("Confirmar Anulaci\u00f3n",
                        "Anulaci\u00f3n Fiscal",
                        "\u00bfEst\u00e1s seguro de que deseas ANULAR fiscalmente el documento "
                                + selected.getDocument()
                                + "?\nEsta acci\u00f3n enviar\u00e1 un registro de ANULACI\u00d3N a Hacienda.");

        if (confirm) {
            repository.requestVoid(selected.getType() + "-" + selected.getId());
            com.mycompany.ventacontrolfx.presentation.util.AlertUtil.showInfo("Solicitud de Anulaci\u00f3n",
                    "Se ha solicitado la anulaci\u00f3n. Se procesar\u00e1 en el pr\u00f3ximo ciclo de sincronizaci\u00f3n.");
            loadData();
        }
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    @FXML
    private void handleRetry() {
        FiscalOperationModel selected = operationsTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        repository.updateStatus(selected.getType() + "-" + selected.getId(), "PENDING",
                "Reintento manual solicitado por el usuario");
        loadData();

        // Buscar el objeto en la nueva lista para volver a seleccionarlo
        for (FiscalOperationModel m : operationsTable.getItems()) {
            if (m.getId() == selected.getId() && m.getType().equals(selected.getType())) {
                operationsTable.getSelectionModel().select(m);
                break;
            }
        }
    }

    @FXML
    private void handleSubsanar() {
        FiscalOperationModel selected = operationsTable.getSelectionModel().getSelectedItem();
        if (selected == null)
            return;

        try {
            if (selected.getType().equals("ALTA")) {
                com.mycompany.ventacontrolfx.domain.model.Sale sale = container.getSaleUseCase()
                        .getSaleDetails(selected.getId());
                if (sale == null) {
                    com.mycompany.ventacontrolfx.presentation.util.AlertUtil.showError("Error",
                            "No se encontró la venta con ID: " + selected.getId());
                    return;
                }

                com.mycompany.ventacontrolfx.presentation.navigation.ModalService.showModal(
                        "/view/dialog/correction_dialog.fxml",
                        "Subsanar Datos Fiscales", javafx.stage.Modality.APPLICATION_MODAL,
                        javafx.stage.StageStyle.TRANSPARENT, container,
                        (com.mycompany.ventacontrolfx.presentation.controller.dialog.CorrectionDialogController controller) -> {
                            controller.init(sale);
                            controller.setOnSuccess((newName, newNif) -> {
                                try {
                                    container.getSaleUseCase().registerCorrection(sale.getSaleId(), newName, newNif);
                                    com.mycompany.ventacontrolfx.presentation.util.AlertUtil.showInfo(
                                            "Correcci\u00f3n registrada",
                                            "Los datos han sido corregidos y se reenviar\u00e1n.");
                                    loadData();
                                } catch (java.sql.SQLException e) {
                                    com.mycompany.ventacontrolfx.presentation.util.AlertUtil.showError("Error",
                                            "No se pudo registrar la correcci\u00f3n: " + e.getMessage());
                                }
                            });
                        });
            } else if (selected.getType().equals("RECTIFICATIVA")) {
                com.mycompany.ventacontrolfx.domain.model.Return ret = container.getReturnUseCase()
                        .getReturnDetails(selected.getId());
                if (ret == null) {
                    com.mycompany.ventacontrolfx.presentation.util.AlertUtil.showError("Error",
                            "No se encontró la devolución con ID: " + selected.getId());
                    return;
                }

                // Usamos la misma ventana de correcci\u00f3n pero adaptada para Return (Duck
                // typing / Manual mapping)
                com.mycompany.ventacontrolfx.presentation.navigation.ModalService.showModal(
                        "/view/dialog/correction_dialog.fxml",
                        "Subsanar Datos Fiscales (Devoluci\u00f3n)", javafx.stage.Modality.APPLICATION_MODAL,
                        javafx.stage.StageStyle.TRANSPARENT, container,
                        (com.mycompany.ventacontrolfx.presentation.controller.dialog.CorrectionDialogController controller) -> {

                            // Mapeo manual temporal para no cambiar el Controller de la ventana
                            com.mycompany.ventacontrolfx.domain.model.Sale dummy = new com.mycompany.ventacontrolfx.domain.model.Sale();
                            dummy.setCustomerNameSnapshot(ret.getCustomerNameSnapshot());
                            dummy.setCustomerNifSnapshot(ret.getCustomerNifSnapshot());

                            controller.init(dummy);
                            controller.setOnSuccess((newName, newNif) -> {
                                try {
                                    container.getReturnUseCase().registerCorrection(ret.getReturnId(), newName, newNif);
                                    com.mycompany.ventacontrolfx.presentation.util.AlertUtil.showInfo(
                                            "Correcci\u00f3n registrada",
                                            "Los datos de la devoluci\u00f3n han sido corregidos.");
                                    loadData();
                                } catch (java.sql.SQLException e) {
                                    com.mycompany.ventacontrolfx.presentation.util.AlertUtil.showError("Error",
                                            "No se pudo registrar la correcci\u00f3n: " + e.getMessage());
                                }
                            });
                        });
            } else {
                com.mycompany.ventacontrolfx.presentation.util.AlertUtil.showWarning("Operaci\u00f3n no permitida",
                        "S\u00f3lo se pueden subsanar facturas o rectificativas.");
            }
        } catch (Exception e) {
            com.mycompany.ventacontrolfx.presentation.util.AlertUtil.showError("Error",
                    "No se pudo cargar la operaci\u00f3n: " + e.getMessage());
        }
    }

    @FXML
    private void handleShowSent() {
        switchTab(paneXmlSent, btnTabSent);
    }

    @FXML
    private void handleShowReceived() {
        switchTab(paneXmlReceived, btnTabReceived);
    }

    @FXML
    private void handleShowHistory() {
        switchTab(paneHistory, btnTabHistory);
    }

    private void switchTab(VBox activePane, Button activeButton) {
        paneXmlSent.setVisible(activePane == paneXmlSent);
        paneXmlReceived.setVisible(activePane == paneXmlReceived);
        paneHistory.setVisible(activePane == paneHistory);

        btnTabSent.getStyleClass().remove("hub-nav-button-active");
        btnTabReceived.getStyleClass().remove("hub-nav-button-active");
        btnTabHistory.getStyleClass().remove("hub-nav-button-active");

        activeButton.getStyleClass().add("hub-nav-button-active");
    }
}
