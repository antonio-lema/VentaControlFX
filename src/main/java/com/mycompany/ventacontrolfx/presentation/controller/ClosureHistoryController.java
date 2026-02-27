package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.CashClosure;
import com.mycompany.ventacontrolfx.domain.model.ProductSummary;
import com.mycompany.ventacontrolfx.application.usecase.CashClosureUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ClosureHistoryController implements Injectable {

    @FXML
    private DatePicker datePickerStart, datePickerEnd;
    @FXML
    private TableView<CashClosure> tableClosures;
    @FXML
    private TableColumn<CashClosure, Integer> colId;
    @FXML
    private TableColumn<CashClosure, String> colDate, colCreated, colUser;
    @FXML
    private TableColumn<CashClosure, Double> colCash, colCard, colTotal;
    @FXML
    private TableColumn<CashClosure, Void> colActions;

    @FXML
    private VBox detailsPanel;
    @FXML
    private Label lblClosureDetailId, lblClosureDetailDate, lblTotalClosures, lblTotalCash, lblTotalCard,
            lblTotalRevenue, lblCount;
    @FXML
    private TableView<ProductSummary> tableProductSummary;
    @FXML
    private TableColumn<ProductSummary, String> colProdName;
    @FXML
    private TableColumn<ProductSummary, Integer> colProdQty;
    @FXML
    private TableColumn<ProductSummary, Double> colProdTotal;

    private CashClosureUseCase closureUseCase;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Override
    public void inject(ServiceContainer container) {
        this.closureUseCase = container.getClosureUseCase();

        datePickerStart.setValue(LocalDate.now().withDayOfMonth(1));
        datePickerEnd.setValue(LocalDate.now());

        setupTable();
        setupProductTable();
        loadClosures();

        tableClosures.getSelectionModel().selectedItemProperty().addListener((obs, old, nv) -> {
            if (nv != null)
                showClosureDetails(nv);
        });
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("closureId"));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getClosureDate().format(dateFormatter)));
        colCreated.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getCreatedAt().format(dateTimeFormatter)));
        colUser.setCellValueFactory(new PropertyValueFactory<>("username"));

        setupCurrencyColumn(colCash, "totalCash");
        setupCurrencyColumn(colCard, "totalCard");
        setupCurrencyColumn(colTotal, "totalAll");

        colActions.setCellFactory(column -> new TableCell<>() {
            private final Button btnPrint = new Button();
            {
                btnPrint.setGraphic(new FontAwesomeIconView(FontAwesomeIcon.PRINT));
                btnPrint.setOnAction(e -> handlePrintClosure(getTableRow().getItem()));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnPrint);
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
                if (empty || item == null)
                    setText(null);
                else {
                    setText(String.format("%.2f €", item));
                    setStyle("-fx-text-fill: " + (item >= 0 ? "#2e7d32" : "#e53935") + "; -fx-font-weight: bold;");
                }
            }
        });
    }

    private void setupCurrencyColumn(TableColumn<CashClosure, Double> col, String prop) {
        col.setCellValueFactory(new PropertyValueFactory<>(prop));
        col.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null)
                    setText(null);
                else {
                    setText(String.format("%.2f €", item));
                    setStyle("-fx-text-fill: " + (item >= 0 ? "#2e7d32" : "#e53935") + "; -fx-font-weight: bold;");
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
                AlertUtil.showWarning("Error", "La fecha inicio no puede ser posterior.");
                return;
            }

            List<CashClosure> closures = closureUseCase.getHistory(start, end);
            tableClosures.setItems(FXCollections.observableArrayList(closures));
            updateSummaries(closures);
            handleCloseDetails();
        } catch (SQLException e) {
            AlertUtil.showError("Error", "No se pudieron cargar los cierres.");
        }
    }

    private void showClosureDetails(CashClosure closure) {
        try {
            lblClosureDetailId.setText("Cierre #" + closure.getClosureId());
            lblClosureDetailDate
                    .setText(closure.getClosureDate().format(dateFormatter) + " - " + closure.getUsername());
            tableProductSummary.setItems(
                    FXCollections.observableArrayList(closureUseCase.getClosureDetails(closure.getClosureId())));
            detailsPanel.setVisible(true);
            detailsPanel.setManaged(true);
        } catch (SQLException e) {
            AlertUtil.showError("Error", "No se pudieron cargar los detalles.");
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
        lblTotalCard.setText(String.format("%.2f €", card));
        lblTotalRevenue.setText(String.format("%.2f €", total));
        if (lblCount != null)
            lblCount.setText(closures.size() + " cierres");
    }

    @FXML
    private void handleClearFilters() {
        datePickerStart.setValue(LocalDate.now().withDayOfMonth(1));
        datePickerEnd.setValue(LocalDate.now());
        loadClosures();
    }

    private void handlePrintClosure(CashClosure closure) {
        AlertUtil.showInfo("Imprimir", "Reimprimiendo cierre #" + closure.getClosureId());
    }
}
