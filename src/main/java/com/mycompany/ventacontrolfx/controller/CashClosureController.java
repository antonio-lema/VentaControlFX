package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.model.User;
import com.mycompany.ventacontrolfx.service.CashClosureService;
import com.mycompany.ventacontrolfx.util.UserSession;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
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

        loadTodayData();
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

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Cierre Exitoso");
            alert.setHeaderText("Cierre de caja guardado con éxito");
            alert.setContentText("Se han registrado " + lblTotalAll.getText() + " en ventas totales.");
            alert.showAndWait();

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
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
