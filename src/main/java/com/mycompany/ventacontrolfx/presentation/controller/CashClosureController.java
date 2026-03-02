package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.CashClosure;
import com.mycompany.ventacontrolfx.domain.model.ProductSummary;
import com.mycompany.ventacontrolfx.application.usecase.CashClosureUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.UserSession;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class CashClosureController implements Injectable {

    @FXML
    private Label lblDate, lblTotalCash, lblTotalCard, lblTotalAll, lblSalesCount, lblCurrentUser, lblStatus;
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

    private CashClosureUseCase closureUseCase;
    private UserSession userSession;
    private double currentCash = 0, currentCard = 0;

    @Override
    public void inject(ServiceContainer container) {
        this.closureUseCase = container.getClosureUseCase();
        this.userSession = container.getUserSession();

        lblDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM, yyyy")));
        if (userSession.getCurrentUser() != null)
            lblCurrentUser.setText(userSession.getCurrentUser().getUsername());

        setupTable();
        loadTodayData();
    }

    private void setupTable() {
        colProdName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colProdQty.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        colProdTotal.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));

        colProdTotal.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null)
                    setText(null);
                else
                    setText(String.format("%.2f €", item));
            }
        });
    }

    private void loadTodayData() {
        try {
            Map<String, Double> totals = closureUseCase.getTodayTotals();
            // Keys returned by JdbcCashClosureRepository.getPendingTotals() are "cash",
            // "card", "total"
            currentCash = totals.getOrDefault("cash", 0.0);
            currentCard = totals.getOrDefault("card", 0.0);
            double totalAll = totals.getOrDefault("total", currentCash + currentCard);

            lblTotalCash.setText("💵 " + String.format("%.2f €", currentCash));
            lblTotalCard.setText("💳 " + String.format("%.2f €", currentCard));
            lblTotalAll.setText("💰 " + String.format("%.2f €", totalAll));
            lblSalesCount.setText("📊 " + closureUseCase.getTodayTransactionCount());

            List<ProductSummary> summary = closureUseCase.getPendingSummary();
            tableProductSummary.setItems(FXCollections.observableArrayList(summary));

            if (closureUseCase.isClosureDoneToday())
                markAsClosed();
        } catch (SQLException e) {
            AlertUtil.showError("Error", "No se pudieron cargar los datos del día.");
        }
    }

    private void markAsClosed() {
        lblStatus.setText("Realizado ✅");
        lblStatus.getStyleClass().removeAll("closure-status-pending");
        lblStatus.getStyleClass().add("closure-status-done");
        btnPerformClosure.setDisable(true);
        btnPerformClosure.setText("CIERRE COMPLETADO");
    }

    @FXML
    private void handlePerformClosure() {
        try {
            CashClosure closure = new CashClosure();
            closure.setClosureDate(LocalDate.now());
            closure.setUserId(userSession.getCurrentUser() != null ? userSession.getCurrentUser().getUserId() : 1);
            closure.setTotalCash(currentCash);
            closure.setTotalCard(currentCard);
            closure.setTotalAll(currentCash + currentCard);

            closureUseCase.performClosure(closure);
            markAsClosed();
            AlertUtil.showInfo("Éxito", "Cierre realizado con éxito.");
        } catch (SQLException e) {
            AlertUtil.showError("Error", "No se pudo realizar el cierre: " + e.getMessage());
        }
    }

    @FXML
    private void handleClose() {
        ((Stage) lblDate.getScene().getWindow()).close();
    }
}
