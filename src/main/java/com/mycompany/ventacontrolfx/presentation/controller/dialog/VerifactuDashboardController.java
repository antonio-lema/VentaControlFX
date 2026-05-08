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
import java.util.List;

public class VerifactuDashboardController implements Injectable {

    @FXML private TableView<FiscalOperationModel> operationsTable;
    @FXML private TableColumn<FiscalOperationModel, String> colDate;
    @FXML private TableColumn<FiscalOperationModel, String> colDoc;
    @FXML private TableColumn<FiscalOperationModel, String> colType;
    @FXML private TableColumn<FiscalOperationModel, Double> colTotal;
    @FXML private TableColumn<FiscalOperationModel, String> colStatus;

    @FXML private Label lblDetailDoc;
    @FXML private Label lblDetailStatus;
    @FXML private Label lblDetailError;
    @FXML private TextArea txtXmlSent;
    @FXML private TextArea txtXmlReceived;
    @FXML private VBox detailPanel;

    private final JdbcVerifactuRepository repository = new JdbcVerifactuRepository();
    private ObservableList<FiscalOperationModel> masterData = FXCollections.observableArrayList();
    private ServiceContainer container;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        ensureSchemaUpdate(); // Hack temporal para arreglar la DB
        setupTable();
        loadData();
    }

    private void ensureSchemaUpdate() {
        String[] tables = {"sales", "returns"};
        String[] columns = {
            "doc_series VARCHAR(10)",
            "doc_number INT",
            "doc_type VARCHAR(50)",
            "doc_status VARCHAR(50)",
            "control_hash VARCHAR(255)",
            "prev_hash VARCHAR(255)",
            "gen_timestamp VARCHAR(100)",
            "signature TEXT",
            "fiscal_status VARCHAR(50) DEFAULT 'PENDING'",
            "fiscal_msg TEXT",
            "aeat_submission_id VARCHAR(100)",
            "xml_sent LONGTEXT",
            "xml_received LONGTEXT",
            "incident_reason TEXT"
        };

        try (java.sql.Connection conn = com.mycompany.ventacontrolfx.infrastructure.persistence.DBConnection.getConnection()) {
            for (String table : tables) {
                for (String colDef : columns) {
                    String colName = colDef.split(" ")[0];
                    if (!columnExists(conn, table, colName)) {
                        String sql = "ALTER TABLE " + table + " ADD COLUMN " + colDef;
                        try (java.sql.Statement stmt = conn.createStatement()) {
                            stmt.executeUpdate(sql);
                        } catch (java.sql.SQLException ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean columnExists(java.sql.Connection conn, String table, String column) throws java.sql.SQLException {
        java.sql.DatabaseMetaData meta = conn.getMetaData();
        try (java.sql.ResultSet rs = meta.getColumns(null, null, table, column)) {
            return rs.next();
        }
    }

    private void setupTable() {
        colDate.setCellValueFactory(cellData -> cellData.getValue().dateProperty());
        colDoc.setCellValueFactory(cellData -> cellData.getValue().documentProperty());
        colType.setCellValueFactory(cellData -> cellData.getValue().typeProperty());
        colTotal.setCellValueFactory(cellData -> cellData.getValue().totalProperty().asObject());
        colStatus.setCellValueFactory(cellData -> cellData.getValue().statusProperty());

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
                        setStyle("-fx-text-fill: #8b5cf6; -fx-font-weight: bold; -fx-background-color: #f5f3ff; -fx-background-radius: 4; -fx-alignment: center;");
                    } else if (displayType.equals("ANULACION") || displayType.equals("ANULADA")) {
                        setStyle("-fx-text-fill: #f97316; -fx-font-weight: bold; -fx-background-color: #fff7ed; -fx-background-radius: 4; -fx-alignment: center;");
                    } else {
                        setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold; -fx-background-color: #eff6ff; -fx-background-radius: 4; -fx-alignment: center;");
                    }
                }
            }
        });
    }

    private void loadData() {
        masterData.setAll(repository.getRecentFiscalOperations(50));
        operationsTable.setItems(masterData);
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
        
        txtXmlSent.setText(formatXml(op.getXmlSent()));
        txtXmlReceived.setText(formatXml(op.getXmlReceived()));
    }

    private String formatXml(String xml) {
        if (xml == null || xml.isEmpty()) return "No hay datos XML registrados.";
        try {
            // Un formateo muy básico para legibilidad si no viene ya formateado
            return xml.replace("><", ">\n<");
        } catch (Exception e) {
            return xml;
        }
    }

    @FXML
    private void handleVoid() {
        FiscalOperationModel selected = operationsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            com.mycompany.ventacontrolfx.presentation.util.AlertUtil.showWarning("Selecci\u00f3n necesaria", "Por favor, selecciona una operaci\u00f3n para anular.");
            return;
        }

        if (!selected.getType().equals("ALTA") && !selected.getType().equals("RECTIFICATIVA")) {
            com.mycompany.ventacontrolfx.presentation.util.AlertUtil.showError("Error", "S\u00f3lo se pueden anular facturas de tipo ALTA o RECTIFICATIVA.");
            return;
        }

        boolean confirm = com.mycompany.ventacontrolfx.presentation.util.AlertUtil.showConfirmation("Confirmar Anulaci\u00f3n", 
            "Anulaci\u00f3n Fiscal",
            "\u00bfEst\u00e1s seguro de que deseas ANULAR fiscalmente el documento " + selected.getDocument() + "?\nEsta acci\u00f3n enviar\u00e1 un registro de ANULACI\u00d3N a Hacienda.");
        
        if (confirm) {
            repository.requestVoid(selected.getType() + "-" + selected.getId());
            com.mycompany.ventacontrolfx.presentation.util.AlertUtil.showInfo("Solicitud de Anulaci\u00f3n", "Se ha solicitado la anulaci\u00f3n. Se procesar\u00e1 en el pr\u00f3ximo ciclo de sincronizaci\u00f3n.");
            loadData();
        }
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }
}
