package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.CashClosure;
import com.mycompany.ventacontrolfx.domain.model.ProductSummary;
import com.mycompany.ventacontrolfx.application.usecase.CashClosureUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.UserSession;
import com.mycompany.ventacontrolfx.presentation.controller.dialog.CashClosingController;
import com.mycompany.ventacontrolfx.presentation.controller.dialog.CashEntryController;
import com.mycompany.ventacontrolfx.presentation.controller.dialog.CashOpeningController;
import com.mycompany.ventacontrolfx.presentation.controller.dialog.CashWithdrawController;
import com.mycompany.ventacontrolfx.util.ModalService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CashClosureController implements Injectable {

    @FXML
    private Label lblDate, lblTotalCash, lblTotalCard, lblTotalAll, lblSalesCount,
            lblCurrentUser, lblStatus, lblCashInDrawer, lblActiveFund, lblTotalReturns, lblTotalDiscounts;
    @FXML
    private VBox containerReturns, containerDiscounts, cardCash, cardAccounting, cardProductSummary;
    @FXML
    private Button btnPerformClosure, btnWithdrawCash, btnRegisterCashEntry, btnOpenFund, btnRefreshCash;
    @FXML
    private TableView<ProductSummary> tableProductSummary;
    @FXML
    private TableColumn<ProductSummary, String> colProdName;
    @FXML
    private TableColumn<ProductSummary, Integer> colProdQty;
    @FXML
    private TableColumn<ProductSummary, Double> colProdTotal;

    private ServiceContainer container;
    private CashClosureUseCase closureUseCase;
    private UserSession userSession;
    private double currentCash = 0, currentCard = 0;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.closureUseCase = container.getClosureUseCase();
        this.userSession = container.getUserSession();

        lblDate.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM, yyyy")));
        if (userSession.getCurrentUser() != null) {
            lblCurrentUser.setText(userSession.getCurrentUser().getUsername());

            // Verificación de permisos para Ingreso
            boolean canEnterCash = userSession.hasPermission("caja.ingresar");
            if (btnRegisterCashEntry != null) {
                btnRegisterCashEntry.setVisible(canEnterCash);
                btnRegisterCashEntry.setManaged(canEnterCash);
            }

            // Verificación de permisos para ver totales financieros
            boolean canSeeTotals = userSession.hasPermission("caja.ver_totales")
                    || userSession.hasPermission("USUARIOS");
            if (cardCash != null) {
                boolean canOperateCash = userSession.hasPermission("caja.abrir")
                        || userSession.hasPermission("caja.cerrar");
                cardCash.setVisible(canSeeTotals || canOperateCash);
                cardCash.setManaged(canSeeTotals || canOperateCash);

                // Ocultar saldo exacto si no tiene permiso de ver totales (Cierre a ciegas)
                if (lblCashInDrawer != null) {
                    lblCashInDrawer.setVisible(canSeeTotals);
                    lblCashInDrawer.setManaged(canSeeTotals);
                }
            }
            if (cardAccounting != null) {
                cardAccounting.setVisible(canSeeTotals);
                cardAccounting.setManaged(canSeeTotals);
            }
            if (cardProductSummary != null) {
                cardProductSummary.setVisible(canSeeTotals);
                cardProductSummary.setManaged(canSeeTotals);
            }
            if (lblActiveFund != null) {
                lblActiveFund.setVisible(canSeeTotals);
                lblActiveFund.setManaged(canSeeTotals);
            }
        }

        setupTable();
        loadTodayData();
        refreshCashDrawer();
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
            currentCash = totals.getOrDefault("cash", 0.0);
            currentCard = totals.getOrDefault("card", 0.0);
            double totalAll = totals.getOrDefault("total", currentCash + currentCard);
            double returnsTotal = totals.getOrDefault("returns_total", 0.0);

            lblTotalCash.setText("💵 " + String.format("%.2f €", currentCash));
            lblTotalCard.setText("💳 " + String.format("%.2f €", currentCard));
            lblTotalAll.setText(
                    "💰 " + container.getBundle().getString("closure.net") + ": " + String.format("%.2f €", totalAll));

            // Mostrar devoluciones totales
            if (lblTotalReturns != null) {
                if (returnsTotal > 0) {
                    lblTotalReturns.setText(String.format("%.2f €", returnsTotal));
                    if (containerReturns != null) {
                        containerReturns.setVisible(true);
                        containerReturns.setManaged(true);
                    }
                } else {
                    if (containerReturns != null) {
                        containerReturns.setVisible(false);
                        containerReturns.setManaged(false);
                    }
                }
            }

            lblSalesCount.setText("📊 " + closureUseCase.getTodayTransactionCount());

            List<ProductSummary> summary = closureUseCase.getPendingSummary();
            tableProductSummary.setItems(FXCollections.observableArrayList(summary));

            // Descuentos aplicados
            double totalDiscounts = totals.getOrDefault("total_discounts", 0.0);
            if (lblTotalDiscounts != null) {
                if (totalDiscounts > 0) {
                    lblTotalDiscounts.setText(String.format("-%.2f €", totalDiscounts));
                    if (containerDiscounts != null) {
                        containerDiscounts.setVisible(true);
                        containerDiscounts.setManaged(true);
                    }
                } else {
                    if (containerDiscounts != null) {
                        containerDiscounts.setVisible(false);
                        containerDiscounts.setManaged(false);
                    }
                }
            }

            if (closureUseCase.isClosureDoneToday() && !closureUseCase.hasActiveFund()
                    && closureUseCase.getTodayTransactionCount() == 0)
                markAsClosed();
            else {
                lblStatus.setText(container.getBundle().getString("closure.status.in_progress") + " ⏳");
                lblStatus.getStyleClass().removeAll("closure-status-done");
                lblStatus.getStyleClass().add("closure-status-pending");
                btnPerformClosure.setDisable(false);
                btnPerformClosure.setText(container.getBundle().getString("closure.btn.perform"));
            }
        } catch (SQLException e) {
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("closure.error.load_today"));
        }
    }

    private void refreshCashDrawer() {
        try {
            boolean hasFund = closureUseCase.hasActiveFund();
            double cashInDrawer = closureUseCase.getCurrentCashInDrawer();
            double fundAmount = closureUseCase.getActiveFundAmount();

            if (hasFund) {
                lblActiveFund.setText("💼 " + container.getBundle().getString("closure.fund") + ": "
                        + String.format("%.2f €", fundAmount));
                lblActiveFund.setStyle("-fx-text-fill: -color-success; -fx-font-weight: bold;");
                lblCashInDrawer.setText("🏦 " + String.format("%.2f €", cashInDrawer));

                // Color según saldo
                if (cashInDrawer < 20) {
                    lblCashInDrawer
                            .setStyle("-fx-text-fill: -color-danger; -fx-font-weight: bold; -fx-font-size: 22px;");
                } else if (cashInDrawer < 50) {
                    lblCashInDrawer
                            .setStyle("-fx-text-fill: -color-warning; -fx-font-weight: bold; -fx-font-size: 22px;");
                } else {
                    lblCashInDrawer
                            .setStyle("-fx-text-fill: -color-success; -fx-font-weight: bold; -fx-font-size: 22px;");
                }

                btnOpenFund.setDisable(true);
                btnOpenFund.setText("✅ " + container.getBundle().getString("closure.status.open"));
                btnWithdrawCash.setDisable(false);
                if (btnRegisterCashEntry != null)
                    btnRegisterCashEntry.setDisable(false);

                // Asegurar que el botón de cierre esté habilitado si hay sesión activa
                btnPerformClosure.setDisable(false);
                btnPerformClosure.setText(container.getBundle().getString("closure.btn.perform"));
                lblStatus.setText(container.getBundle().getString("closure.status.in_progress") + " ⏳");
                lblStatus.getStyleClass().removeAll("closure-status-done");
                lblStatus.getStyleClass().add("closure-status-pending");
            } else {
                lblActiveFund.setText("⚠️ " + container.getBundle().getString("closure.no_fund"));
                lblActiveFund.setStyle("-fx-text-fill: -color-warning; -fx-font-weight: bold;");
                lblCashInDrawer.setText("🏦 " + container.getBundle().getString("closure.no_session"));
                lblCashInDrawer.setStyle("-fx-text-fill: -text-muted; -fx-font-size: 22px;");
                btnOpenFund.setDisable(false);
                btnOpenFund.setText("💼 " + container.getBundle().getString("closure.btn.open_fund"));
                btnWithdrawCash.setDisable(true);
                if (btnRegisterCashEntry != null)
                    btnRegisterCashEntry.setDisable(true);
            }
        } catch (SQLException e) {
            lblCashInDrawer.setText("🏦 " + container.getBundle().getString("error.load"));
        }
    }

    private void markAsClosed() {
        lblStatus.setText(container.getBundle().getString("closure.status.completed") + " ✅");
        lblStatus.getStyleClass().removeAll("closure-status-pending");
        lblStatus.getStyleClass().add("closure-status-done");
        btnPerformClosure.setDisable(true);
        btnPerformClosure.setText(container.getBundle().getString("closure.btn.completed"));
    }

    @FXML
    private void handlePerformClosure() {
        CashClosingController ctrl = ModalService.showTransparentModal("/view/dialog/cash_closing_dialog.fxml",
                container.getBundle().getString("closure.dialog.closing_title"), container,
                (CashClosingController controller) -> {
                    controller.init(closureUseCase, userSession);
                });

        if (ctrl != null && ctrl.isConfirmed()) {
            loadTodayData();
            refreshCashDrawer();
            if (container.getNavigationService() != null) {
                container.getNavigationService().navigateTo("/view/sell_view.fxml");
            }
        }
    }

    @FXML
    private void handleOpenFund() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/dialog/cash_opening_dialog.fxml"));
            VBox pane = loader.load();
            CashOpeningController controller = loader.getController();
            controller.init(closureUseCase, userSession);

            Stage stage = new Stage();
            stage.setTitle(container.getBundle().getString("closure.dialog.opening_title"));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initOwner(lblDate.getScene().getWindow());
            stage.setScene(new javafx.scene.Scene(pane));
            stage.showAndWait();

            if (controller.isConfirmed()) {
                refreshCashDrawer();
                AlertUtil.showToast(container.getBundle().getString("closure.success.opened"));
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("closure.error.dialog_opening"));
        }
    }

    @FXML
    private void handleWithdrawCash() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/dialog/cash_withdraw_dialog.fxml"));
            javafx.scene.layout.VBox pane = loader.load();
            CashWithdrawController controller = loader.getController();
            controller.init(closureUseCase, userSession);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle(container.getBundle().getString("closure.dialog.withdraw_title"));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initOwner(lblDate.getScene().getWindow());
            stage.setScene(new javafx.scene.Scene(pane));
            stage.showAndWait();

            if (controller.isConfirmed()) {
                refreshCashDrawer();
                loadTodayData();
                AlertUtil.showToast(container.getBundle().getString("closure.success.withdraw"));
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("closure.error.dialog_withdraw"));
        }
    }

    @FXML
    private void handleRegisterCashEntry() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/dialog/cash_entry_dialog.fxml"));
            javafx.scene.layout.VBox pane = loader.load();
            CashEntryController controller = loader.getController();
            controller.init(closureUseCase, userSession);

            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle(container.getBundle().getString("closure.dialog.entry_title"));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.initOwner(lblDate.getScene().getWindow());
            stage.setScene(new javafx.scene.Scene(pane));
            stage.showAndWait();

            if (controller.isConfirmed()) {
                refreshCashDrawer();
                loadTodayData();
                AlertUtil.showToast(container.getBundle().getString("closure.success.entry"));
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("closure.error.dialog_entry"));
        }
    }

    @FXML
    private void handleRefreshCash() {
        loadTodayData();
        refreshCashDrawer();
    }

    @FXML
    private void handleClose() {
        ((Stage) lblDate.getScene().getWindow()).close();
    }
}
