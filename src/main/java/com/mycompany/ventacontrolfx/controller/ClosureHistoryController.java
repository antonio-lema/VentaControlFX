package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.CashClosure;
import com.mycompany.ventacontrolfx.model.ProductSummary;
import com.mycompany.ventacontrolfx.service.CashClosureService;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

public class ClosureHistoryController {

    @FXML
    private DatePicker datePickerStart;
    @FXML
    private DatePicker datePickerEnd;

    @FXML
    private TableView<CashClosure> tableClosures;
    @FXML
    private TableColumn<CashClosure, Integer> colId;
    @FXML
    private TableColumn<CashClosure, String> colDate;
    @FXML
    private TableColumn<CashClosure, String> colCreated;
    @FXML
    private TableColumn<CashClosure, String> colUser;
    @FXML
    private TableColumn<CashClosure, Double> colCash;
    @FXML
    private TableColumn<CashClosure, Double> colCard;
    @FXML
    private TableColumn<CashClosure, Double> colTotal;
    @FXML
    private TableColumn<CashClosure, Void> colActions;

    // Details Panel Components
    @FXML
    private VBox detailsPanel;
    @FXML
    private Label lblClosureDetailId;
    @FXML
    private Label lblClosureDetailDate;
    @FXML
    private TableView<ProductSummary> tableProductSummary;
    @FXML
    private TableColumn<ProductSummary, String> colProdName;
    @FXML
    private TableColumn<ProductSummary, Integer> colProdQty;
    @FXML
    private TableColumn<ProductSummary, Double> colProdTotal;

    @FXML
    private Label lblTotalClosures;
    @FXML
    private Label lblTotalCash;
    @FXML
    private Label lblTotalCard;
    @FXML
    private Label lblTotalRevenue;

    private final CashClosureService closureService = new CashClosureService();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @FXML
    public void initialize() {
        datePickerStart.setValue(LocalDate.now().withDayOfMonth(1));
        datePickerEnd.setValue(LocalDate.now());
        setupTable();
        setupProductTable();
        loadClosures();

        // Selection listener
        tableClosures.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showClosureDetails(newVal);
            }
        });
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("closureId"));
        colDate.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getClosureDate().format(dateFormatter)));
        colCreated.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getCreatedAt().format(dateTimeFormatter)));
        colUser.setCellValueFactory(new PropertyValueFactory<>("username"));
        colCash.setCellValueFactory(new PropertyValueFactory<>("totalCash"));
        colCard.setCellValueFactory(new PropertyValueFactory<>("totalCard"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalAll"));

        // Format currency columns
        setupCurrencyColumn(colCash);
        setupCurrencyColumn(colCard);
        setupCurrencyColumn(colTotal);

        // Actions button
        colActions.setCellFactory(column -> new TableCell<>() {
            private final Button btnPrint = new Button();
            {
                btnPrint.setGraphic(new de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView(
                        de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.PRINT));
                btnPrint.getStyleClass().add("btn-icon");
                btnPrint.setOnAction(event -> {
                    CashClosure closure = getTableRow().getItem();
                    if (closure != null) {
                        handlePrintClosure(closure);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnPrint);
                    setAlignment(javafx.geometry.Pos.CENTER);
                }
            }
        });
    }

    private void setupProductTable() {
        colProdName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colProdQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colProdTotal.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));

        colProdTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%.2f €", item));
                    String color = item >= 0 ? "#2e7d32" : "#e53935";
                    setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
                }
            }
        });
    }

    private void setupCurrencyColumn(TableColumn<CashClosure, Double> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%.2f €", item));
                    String color = item >= 0 ? "#2e7d32" : "#e53935";
                    setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
                }
            }
        });
    }

    @FXML
    public void loadClosures() {
        try {
            LocalDate start = datePickerStart.getValue();
            LocalDate end = datePickerEnd.getValue();

            if (start == null || end == null)
                return;

            if (start.isAfter(end)) {
                showAlert("Error", "La fecha de inicio no puede ser posterior a la de fin.");
                return;
            }

            List<CashClosure> closures = closureService.getClosureHistory(start, end);
            tableClosures.setItems(FXCollections.observableArrayList(closures));
            updateSummaries(closures);
            handleCloseDetails(); // Close panel on reload
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "No se pudieron cargar los cierres: " + e.getMessage());
        }
    }

    private void showClosureDetails(CashClosure closure) {
        try {
            lblClosureDetailId.setText("Cierre #" + closure.getClosureId());
            lblClosureDetailDate.setText(
                    closure.getClosureDate().format(dateFormatter) + " - Realizado por " + closure.getUsername());

            List<ProductSummary> summary = closureService.getProductSummary(closure.getClosureId());
            tableProductSummary.setItems(FXCollections.observableArrayList(summary));

            detailsPanel.setVisible(true);
            detailsPanel.setManaged(true);
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Error", "No se pudieron cargar los detalles: " + e.getMessage());
        }
    }

    @FXML
    private void handleCloseDetails() {
        detailsPanel.setVisible(false);
        detailsPanel.setManaged(false);
        tableClosures.getSelectionModel().clearSelection();
    }

    private void updateSummaries(List<CashClosure> closures) {
        double cash = 0, card = 0, total = 0;
        for (CashClosure c : closures) {
            cash += c.getTotalCash();
            card += c.getTotalCard();
            total += c.getTotalAll();
        }

        lblTotalClosures.setText(String.valueOf(closures.size()));

        lblTotalCash.setText(String.format("%.2f €", cash));
        lblTotalCash.setStyle("-fx-font-weight: bold; -fx-font-size: 26px; -fx-text-fill: "
                + (cash >= 0 ? "#2e7d32" : "#e53935") + ";");

        lblTotalCard.setText(String.format("%.2f €", card));
        lblTotalCard.setStyle("-fx-font-weight: bold; -fx-font-size: 26px; -fx-text-fill: "
                + (card >= 0 ? "#2e7d32" : "#e53935") + ";");

        lblTotalRevenue.setText(String.format("%.2f €", total));
        // We don't change textFill for revenue if it's the blue card unless requested,
        // but let's keep it white as the background is blue.
        // If the background was white, we would color it.
    }

    @FXML
    private void handleClearFilters() {
        datePickerStart.setValue(LocalDate.now().withDayOfMonth(1));
        datePickerEnd.setValue(LocalDate.now());
        loadClosures();
    }

    private void handlePrintClosure(CashClosure closure) {
        // Placeholder for future print functionality
        showAlert("Imprimir", "Reimprimiendo cierre #" + closure.getClosureId() + " del "
                + closure.getClosureDate().format(dateFormatter));
    }

    private void showAlert(String title, String content) {
        if (title.contains("Error")) {
            AlertUtil.showError(title, content);
        } else if (title.contains("Imprimir")) {
            AlertUtil.showInfo(title, content);
        } else {
            AlertUtil.showWarning(title, content);
        }
    }
}
