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
        setupTable();
        loadData();
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
                    } else if (item.equals("REJECTED")) {
                        setStyle("-fx-text-fill: #ef4444; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
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
    private void handleRefresh() {
        loadData();
    }
}
