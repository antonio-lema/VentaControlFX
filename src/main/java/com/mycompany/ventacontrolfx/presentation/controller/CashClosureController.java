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
            lblCurrentUser, lblStatus, lblCashInDrawer, lblActiveFund, lblTotalReturns;
    @FXML
    private VBox containerReturns;
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

    private CashClosureUseCase closureUseCase;
    private UserSession userSession;
    private double currentCash = 0, currentCard = 0;

    @Override
    public void inject(ServiceContainer container) {
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
            double returnsCash = totals.getOrDefault("returns_cash", 0.0);
            double returnsCard = totals.getOrDefault("returns_card", 0.0);

            // Ventas netas (ya descontadas las devoluciones)
            double grossCash = currentCash + returnsCash;
            double grossCard = currentCard + returnsCard;

            lblTotalCash.setText("💵 " + String.format("%.2f €", grossCash)
                    + (returnsCash > 0 ? " (−" + String.format("%.2f €", returnsCash) + " dev.)" : ""));
            lblTotalCard.setText("💳 " + String.format("%.2f €", grossCard)
                    + (returnsCard > 0 ? " (−" + String.format("%.2f €", returnsCard) + " dev.)" : ""));
            lblTotalAll.setText("💰 Neto: " + String.format("%.2f €", totalAll));

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

            if (closureUseCase.isClosureDoneToday() && !closureUseCase.hasActiveFund()
                    && closureUseCase.getTodayTransactionCount() == 0)
                markAsClosed();
            else {
                lblStatus.setText("En curso... ⏳");
                lblStatus.getStyleClass().removeAll("closure-status-done");
                lblStatus.getStyleClass().add("closure-status-pending");
                btnPerformClosure.setDisable(false);
                btnPerformClosure.setText("REALIZAR CIERRE DE CAJA");
            }
        } catch (SQLException e) {
            AlertUtil.showError("Error", "No se pudieron cargar los datos del día.");
        }
    }

    private void refreshCashDrawer() {
        try {
            boolean hasFund = closureUseCase.hasActiveFund();
            double cashInDrawer = closureUseCase.getCurrentCashInDrawer();
            double fundAmount = closureUseCase.getActiveFundAmount();

            if (hasFund) {
                lblActiveFund.setText("💼 Fondo: " + String.format("%.2f €", fundAmount));
                lblActiveFund.setStyle("-fx-text-fill: -color-success; -fx-font-weight: bold;");
                lblCashInDrawer.setText("🏦 " + String.format("%.2f €", cashInDrawer));

                // Color según saldo
                if (cashInDrawer < 20) {
                    lblCashInDrawer.setStyle("-fx-text-fill: -color-danger; -fx-font-weight: bold; -fx-font-size: 22px;");
                } else if (cashInDrawer < 50) {
                    lblCashInDrawer.setStyle("-fx-text-fill: -color-warning; -fx-font-weight: bold; -fx-font-size: 22px;");
                } else {
                    lblCashInDrawer.setStyle("-fx-text-fill: -color-success; -fx-font-weight: bold; -fx-font-size: 22px;");
                }

                btnOpenFund.setDisable(true);
                btnOpenFund.setText("✅ CAJA ABIERTA");
                btnWithdrawCash.setDisable(false);
                if (btnRegisterCashEntry != null)
                    btnRegisterCashEntry.setDisable(false);

                // Asegurar que el botón de cierre esté habilitado si hay sesión activa
                btnPerformClosure.setDisable(false);
                btnPerformClosure.setText("REALIZAR CIERRE DE CAJA");
                lblStatus.setText("En curso... ⏳");
                lblStatus.getStyleClass().removeAll("closure-status-done");
                lblStatus.getStyleClass().add("closure-status-pending");
            } else {
                lblActiveFund.setText("⚠️ Sin fondo de caja");
                lblActiveFund.setStyle("-fx-text-fill: -color-warning; -fx-font-weight: bold;");
                lblCashInDrawer.setText("🏦 Sin sesión");
                lblCashInDrawer.setStyle("-fx-text-fill: -text-muted; -fx-font-size: 22px;");
                btnOpenFund.setDisable(false);
                btnOpenFund.setText("💼 ABRIR CAJA CON FONDO");
                btnWithdrawCash.setDisable(true);
                if (btnRegisterCashEntry != null)
                    btnRegisterCashEntry.setDisable(true);
            }
        } catch (SQLException e) {
            lblCashInDrawer.setText("🏦 Error al cargar");
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
        double expectedCash = 0;
        try {
            expectedCash = closureUseCase.getCurrentCashInDrawer();
        } catch (SQLException e) {
            AlertUtil.showError("Error", "No se pudo obtener el efectivo en caja.");
            return;
        }

        Dialog<CashClosure> dialog = new Dialog<>();
        dialog.setTitle("Cierre de Caja (Arqueo)");
        dialog.setHeaderText("🏦 Finalizar jornada y contrastar efectivo");

        // Aplicar tema al diálogo
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles/variables.css").toExternalForm());
        dialogPane.getStylesheets().add(getClass().getResource("/styles/components/formularios.css").toExternalForm());
        dialogPane.getStyleClass().add("modern-register-card");
        dialogPane.setStyle("-fx-background-color: -bg-surface;");

        ButtonType btnTypeCerrar = new ButtonType("REALIZAR CIERRE", ButtonBar.ButtonData.OK_DONE);
        dialogPane.getButtonTypes().addAll(btnTypeCerrar, ButtonType.CANCEL);
        
        // Estilizar botones del diálogo
        Button okBtn = (Button) dialogPane.lookupButton(btnTypeCerrar);
        Button cancelBtn = (Button) dialogPane.lookupButton(ButtonType.CANCEL);
        okBtn.getStyleClass().add("btn-primary");
        cancelBtn.getStyleClass().add("btn-secondary");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(25));

        Label lblExpected = new Label(String.format("Efectivo esperado (en caja): %.2f €", expectedCash));
        lblExpected.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: -text-main;");

        final double finalExpected = expectedCash;

        TextField txtActual = new TextField();
        txtActual.setPromptText("Efectivo real contado");
        txtActual.getStyleClass().add("modern-input-field");
        txtActual.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label lblDiff = new Label("Diferencia: 0,00 €");
        lblDiff.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        TextArea txtNotes = new TextArea();
        txtNotes.setPromptText("Observaciones (obligatorio si hay descuadre)");
        txtNotes.getStyleClass().add("text-field"); // Reutilizamos estilo base
        txtNotes.setPrefRowCount(3);
        txtNotes.setWrapText(true);

        txtActual.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                double actual = Double.parseDouble(newVal.replace(",", ".").trim());
                double diff = actual - finalExpected;
                lblDiff.setText(String.format("Diferencia: %.2f €", diff));
                if (Math.abs(diff) < 0.01) {
                    lblDiff.setStyle("-fx-text-fill: -color-success; -fx-font-weight: bold;");
                } else {
                    lblDiff.setStyle("-fx-text-fill: -color-danger; -fx-font-weight: bold;");
                }
            } catch (Exception e) {
                lblDiff.setText("Diferencia: --");
            }
        });

        grid.add(lblExpected, 0, 0, 2, 1);
        grid.add(new Label("Efectivo Real (€):"), 0, 1);
        grid.add(txtActual, 1, 1);
        grid.add(lblDiff, 1, 2);
        grid.add(new Label("Notas:"), 0, 3);
        grid.add(txtNotes, 1, 3);

        dialog.getDialogPane().setContent(grid);
        javafx.application.Platform.runLater(txtActual::requestFocus);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(btnTypeCerrar);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                double actual = Double.parseDouble(txtActual.getText().replace(",", ".").trim());
                double diff = actual - finalExpected;
                String notes = txtNotes.getText().trim();

                if (Math.abs(diff) > 0.01 && notes.isEmpty()) {
                    AlertUtil.showWarning("Atención",
                            "Es obligatorio introducir una nota explicando el descuadre de caja.");
                    event.consume();
                }
            } catch (Exception e) {
                AlertUtil.showError("Error", "El importe introducido no es válido.");
                event.consume();
            }
        });

        dialog.setResultConverter(btn -> {
            if (btn == btnTypeCerrar) {
                try {
                    double actual = Double.parseDouble(txtActual.getText().replace(",", ".").trim());
                    CashClosure c = new CashClosure();
                    c.setClosureDate(LocalDate.now());
                    c.setUserId(userSession.getCurrentUser() != null ? userSession.getCurrentUser().getUserId() : 1);
                    c.setTotalCash(finalExpected);
                    c.setTotalCard(currentCard);
                    c.setTotalAll(finalExpected + currentCard);
                    c.setActualCash(actual);
                    c.setDifference(actual - finalExpected);
                    c.setNotes(txtNotes.getText().trim());
                    return c;
                } catch (Exception ignored) {
                }
            }
            return null;
        });

        Optional<CashClosure> result = dialog.showAndWait();
        result.ifPresent(closure -> {
            try {
                closureUseCase.performClosure(closure);
                markAsClosed();
                AlertUtil.showToast("Cierre realizado con éxito.");
                refreshCashDrawer();
            } catch (SQLException e) {
                AlertUtil.showError("Error", "No se pudo realizar el cierre: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleOpenFund() {
        // Diálogo para introducir el fondo inicial
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Abrir Caja");
        dialog.setHeaderText("💼 Introduce el fondo de caja inicial");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles/variables.css").toExternalForm());
        dialogPane.getStylesheets().add(getClass().getResource("/styles/components/formularios.css").toExternalForm());
        dialogPane.getStyleClass().add("modern-register-card");
        dialogPane.setStyle("-fx-background-color: -bg-surface;");

        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialogPane.lookupButton(ButtonType.OK)).getStyleClass().add("btn-primary");
        ((Button) dialogPane.lookupButton(ButtonType.CANCEL)).getStyleClass().add("btn-secondary");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(25));

        double lastClosure = 0;
        try {
            lastClosure = closureUseCase.getLastClosureAmount();
        } catch (SQLException ignored) {
        }

        Label lblLastClosure = new Label(String.format("Efectivo del cierre anterior: %.2f €", lastClosure));
        lblLastClosure.setStyle("-fx-text-fill: -color-primary; -fx-font-weight: bold;");

        TextField txtAmount = new TextField(String.format("%.2f", lastClosure).replace(".", ","));
        txtAmount.setPrefWidth(220);
        txtAmount.getStyleClass().add("modern-input-field");
        txtAmount.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        txtAmount.setPromptText("Importe en euros");

        grid.add(lblLastClosure, 0, 0, 2, 1);
        grid.add(new Label("¿Cuánto dejas en caja? (€):"), 0, 1);
        grid.add(txtAmount, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Poner foco en el campo
        javafx.application.Platform.runLater(txtAmount::requestFocus);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    return Double.parseDouble(txtAmount.getText().replace(",", ".").trim());
                } catch (NumberFormatException e) {
                    AlertUtil.showError("Error", "Importe inválido. Por favor introduce un número.");
                    return null;
                }
            }
            return null;
        });

        Optional<Double> result = dialog.showAndWait();
        result.ifPresent(amount -> {
            try {
                int userId = userSession.getCurrentUser() != null ? userSession.getCurrentUser().getUserId() : 1;
                closureUseCase.openCashFund(amount, userId);
                refreshCashDrawer();
                AlertUtil.showInfo("✅ Caja Abierta",
                        String.format("Fondo inicial establecido en %.2f €.\n¡La caja está lista!", amount));
            } catch (SQLException e) {
                AlertUtil.showError("Error", e.getMessage());
            }
        });
    }

    @FXML
    private void handleWithdrawCash() {
        Dialog<double[]> dialog = new Dialog<>();
        dialog.setTitle("Retirada de Efectivo");
        dialog.setHeaderText("💸 Registrar retirada de efectivo de caja");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles/variables.css").toExternalForm());
        dialogPane.getStylesheets().add(getClass().getResource("/styles/components/formularios.css").toExternalForm());
        dialogPane.getStyleClass().add("modern-register-card");
        dialogPane.setStyle("-fx-background-color: -bg-surface;");

        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialogPane.lookupButton(ButtonType.OK)).getStyleClass().add("btn-danger");
        ((Button) dialogPane.lookupButton(ButtonType.CANCEL)).getStyleClass().add("btn-secondary");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        try {
            double available = closureUseCase.getCurrentCashInDrawer();
            Label lblAvailable = new Label(String.format("Efectivo disponible: %.2f €", available));
            lblAvailable.setStyle("-fx-text-fill: -color-success; -fx-font-weight: bold; -fx-font-size: 13px;");
            grid.add(lblAvailable, 0, 0, 2, 1);
        } catch (SQLException ignored) {
        }

        TextField txtAmount = new TextField();
        txtAmount.setPromptText("0,00 €");
        txtAmount.getStyleClass().add("modern-input-field");
        txtAmount.setPrefWidth(200);

        TextField txtReason = new TextField();
        txtReason.setPromptText("Especifique motivo...");
        txtReason.getStyleClass().add("modern-input-field");
        txtReason.setPrefWidth(200);

        grid.add(new Label("Importe (€):"), 0, 1);
        grid.add(txtAmount, 1, 1);
        grid.add(new Label("Motivo:"), 0, 2);
        grid.add(txtReason, 1, 2);

        dialog.getDialogPane().setContent(grid);
        javafx.application.Platform.runLater(txtAmount::requestFocus);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                double amount = Double.parseDouble(txtAmount.getText().replace(",", ".").trim());
                String reason = txtReason.getText().trim();

                if (amount <= 0) {
                    AlertUtil.showError("Error", "El importe debe ser mayor que cero.");
                    event.consume();
                    return;
                }
                if (reason.isEmpty()) {
                    AlertUtil.showWarning("Aviso", "Debes indicar el motivo de la retirada.");
                    event.consume();
                    return;
                }

                int userId = userSession.getCurrentUser() != null ? userSession.getCurrentUser().getUserId() : 1;
                closureUseCase.withdrawCash(amount, reason, userId);
                refreshCashDrawer();
                AlertUtil.showInfo("✅ Retirada registrada",
                        String.format("Se han retirado %.2f € de la caja.\nMotivo: %s", amount, reason));

            } catch (NumberFormatException e) {
                AlertUtil.showError("Error", "Importe inválido.");
                event.consume();
            } catch (SQLException e) {
                AlertUtil.showError("❌ Sin efectivo suficiente", e.getMessage());
                event.consume();
            }
        });

        dialog.showAndWait();
    }

    @FXML
    private void handleRegisterCashEntry() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Ingresar Efectivo");
        dialog.setHeaderText("➕ Registrar entrada manual de efectivo a caja");

        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles/variables.css").toExternalForm());
        dialogPane.getStylesheets().add(getClass().getResource("/styles/components/formularios.css").toExternalForm());
        dialogPane.getStyleClass().add("modern-register-card");
        dialogPane.setStyle("-fx-background-color: -bg-surface;");

        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) dialogPane.lookupButton(ButtonType.OK)).getStyleClass().add("btn-primary");
        ((Button) dialogPane.lookupButton(ButtonType.CANCEL)).getStyleClass().add("btn-secondary");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField txtAmount = new TextField();
        txtAmount.setPromptText("0,00 €");
        txtAmount.getStyleClass().add("modern-input-field");
        txtAmount.setPrefWidth(200);

        ComboBox<String> comboType = new ComboBox<>(FXCollections.observableArrayList(
                "Cambio banco", "Ajuste manual", "Ingreso extraordinario", "Otro"));
        comboType.setValue("Cambio banco");
        comboType.getStyleClass().add("modern-combo-box");
        comboType.setPrefWidth(200);

        TextField txtReason = new TextField();
        txtReason.setPromptText("Justificación...");
        txtReason.getStyleClass().add("modern-input-field");
        txtReason.setPrefWidth(200);

        grid.add(new Label("Importe (€):"), 0, 0);
        grid.add(txtAmount, 1, 0);
        grid.add(new Label("Tipo:"), 0, 1);
        grid.add(comboType, 1, 1);
        grid.add(new Label("Motivo:"), 0, 2);
        grid.add(txtReason, 1, 2);

        dialog.getDialogPane().setContent(grid);
        javafx.application.Platform.runLater(txtAmount::requestFocus);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            try {
                double amount = Double.parseDouble(txtAmount.getText().replace(",", ".").trim());
                String type = comboType.getValue();
                String reason = txtReason.getText().trim();

                if (amount <= 0) {
                    AlertUtil.showError("Error", "El importe debe ser mayor que cero.");
                    event.consume();
                    return;
                }
                if (reason.isEmpty()) {
                    AlertUtil.showWarning("Aviso", "Es obligatorio introducir un motivo justificado.");
                    event.consume();
                    return;
                }

                int userId = userSession.getCurrentUser() != null ? userSession.getCurrentUser().getUserId() : 1;
                String fullReason = "[" + type + "] " + reason;

                closureUseCase.registerCashEntry(amount, fullReason, userId);
                refreshCashDrawer();
                AlertUtil.showInfo("✅ Ingreso registrado",
                        String.format("Se han ingresado %.2f € en la caja.\nMotivo: %s", amount, fullReason));

            } catch (NumberFormatException e) {
                AlertUtil.showError("Error", "Importe inválido.");
                event.consume();
            } catch (SQLException e) {
                AlertUtil.showError("Error", "No se pudo registrar el ingreso: " + e.getMessage());
                event.consume();
            }
        });

        dialog.showAndWait();
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
