package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.ProductSummary;
import com.mycompany.ventacontrolfx.model.User;
import com.mycompany.ventacontrolfx.service.CashClosureService;
import com.mycompany.ventacontrolfx.util.UserSession;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

public class CashClosureController {

    @FXML
    private Label lblDate;
    @FXML
    private Label lblTotalCash;
    @FXML
    private Label lblTotalCard;
    @FXML
    private Label lblTotalAll;
    @FXML
    private Label lblSalesCount;
    @FXML
    private Label lblCurrentUser;
    @FXML
    private Label lblStatus;
    @FXML
    private Button btnPerformClosure;

    @FXML
    private TableView<ProductSummary> tableProductSummary;
    @FXML
    private TableColumn<ProductSummary, String> colProdName;
    @FXML
    private TableColumn<ProductSummary, Integer> colProdQty;
    @FXML
    private TableColumn<ProductSummary, Double> colProdTotal;

    private final CashClosureService closureService = new CashClosureService();
    private double currentCash = 0;
    private double currentCard = 0;

    @FXML
    public void initialize() {
        lblDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM, yyyy")));

        User user = UserSession.getInstance().getCurrentUser();
        if (user != null) {
            lblCurrentUser.setText(user.getUsername());
        }

        setupTable();
        loadTodayData();
    }

    private void setupTable() {
        colProdName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colProdQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colProdTotal.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));

        // Estilo para nombre
        colProdName.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-alignment: CENTER-LEFT; -fx-padding: 0 0 0 15;");
                }
            }
        });

        // Alinhamento e estilo para quantidade
        colProdQty.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.valueOf(item));
                    setStyle("-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold; -fx-padding: 0 15 0 0;");
                }
            }
        });

        // Alinhamento e estilo para total
        colProdTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%.2f €", item));
                    setStyle(
                            "-fx-alignment: CENTER-RIGHT; -fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-padding: 0 15 0 0;");
                }
            }
        });
    }

    private void loadTodayData() {
        try {
            Map<String, Double> totals = closureService.getTodaySalesTotals();
            currentCash = totals.getOrDefault("Efectivo", 0.0);
            currentCard = totals.getOrDefault("Tarjeta", 0.0);
            double totalAll = currentCash + currentCard;

            lblTotalCash.setText(String.format("%.2f €", currentCash));
            lblTotalCard.setText(String.format("%.2f €", currentCard));
            lblTotalAll.setText(String.format("%.2f €", totalAll));

            int count = closureService.getTodaySalesCount();
            lblSalesCount.setText(String.valueOf(count));

            // Load product summary
            List<ProductSummary> summary = closureService.getPendingProductSummary();
            tableProductSummary.setItems(FXCollections.observableArrayList(summary));

            if (closureService.isClosureDoneToday()) {
                markAsClosed();
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Error al cargar datos del día: " + e.getMessage());
        }
    }

    private void markAsClosed() {
        lblStatus.setText("Realizado ✅");
        lblStatus.setStyle("-fx-text-fill: #34a853; -fx-font-weight: bold;");
        btnPerformClosure.setDisable(true);
        btnPerformClosure.setText("CIERRE COMPLETADO");
        btnPerformClosure.setStyle("-fx-background-color: #e8eaed; -fx-text-fill: #5f6368; -fx-background-radius: 30;");
    }

    @FXML
    private void handlePerformClosure() {
        try {
            closureService.performClosure(currentCash, currentCard);
            markAsClosed();

            AlertUtil.showInfo("Cierre Exitoso", "Se han registrado " + lblTotalAll.getText() + " en ventas totales.");

        } catch (SQLException e) {
            e.printStackTrace();
            showError("No se pudo realizar el cierre: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) lblDate.getScene().getWindow();
        stage.close();
    }

    private void showError(String message) {
        AlertUtil.showError("Error", message);
    }
}
