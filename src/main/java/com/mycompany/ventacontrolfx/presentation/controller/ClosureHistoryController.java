package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.CashClosure;
import com.mycompany.ventacontrolfx.domain.repository.ICashClosureRepository.CashMovement;
import com.mycompany.ventacontrolfx.application.usecase.CashClosureUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.DateFilterUtils;
import com.mycompany.ventacontrolfx.util.ModalService;
import com.mycompany.ventacontrolfx.util.PaginationHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ClosureHistoryController implements Injectable {

    @FXML
    private DatePicker datePickerStart, datePickerEnd;
    @FXML
    private HBox quickFilterContainer;
    @FXML
    private ComboBox<String> cmbStatusFilter;
    @FXML
    private ComboBox<Integer> cmbRowLimit;

    @FXML
    private TableView<CashClosure> tableClosures;
    @FXML
    private TableColumn<CashClosure, String> colStatus, colCreated, colUser;
    @FXML
    private TableColumn<CashClosure, Double> colInitialFund, colExpected, colActual, colDifference;

    @FXML
    private VBox detailsPanel;
    @FXML
    private Label lblClosureDetailId, lblTotalClosures, lblTotalDifference, lblPendingReview, lblCurrentCash, lblCount;
    @FXML
    private Label lblDetInitial, lblDetSales, lblDetIn, lblDetOut, lblDetExpected, lblDetActual, lblDetNotes;
    @FXML
    private Button btnMarkReviewed, btnEditClosure;

    @FXML
    private TableView<CashMovement> tableMovements;
    @FXML
    private TableColumn<CashMovement, String> colMovType, colMovReason, colMovUser;
    @FXML
    private TableColumn<CashMovement, LocalDateTime> colMovCreated;
    @FXML
    private TableColumn<CashMovement, Double> colMovAmount;

    private CashClosureUseCase closureUseCase;
    private com.mycompany.ventacontrolfx.util.UserSession userSession;
    private com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer container;
    private PaginationHelper<CashClosure> paginationHelper;
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private ObservableList<CashClosure> allClosures = FXCollections.observableArrayList();

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.closureUseCase = container.getClosureUseCase();
        this.userSession = container.getUserSession();

        setupTable();
        setupMovementsTable();
        setupFilters();
        DateFilterUtils.addQuickFilters(quickFilterContainer, datePickerStart,
                datePickerEnd, container.getBundle(), this::loadClosures);
        paginationHelper = new PaginationHelper<>(tableClosures, cmbRowLimit, lblCount, container.getBundle().getString("closure.history.label.arqueos"));
        loadClosures();

        tableClosures.getSelectionModel().selectedItemProperty().addListener((obs, old, nv) -> {
            if (nv != null)
                showClosureDetails(nv);
        });
    }

    private void setupFilters() {
        cmbStatusFilter.setItems(FXCollections.observableArrayList(
            container.getBundle().getString("closure.history.status.all"),
            container.getBundle().getString("closure.history.status.squared"),
            container.getBundle().getString("closure.history.status.offset"),
            container.getBundle().getString("closure.history.status.reviewed"),
            container.getBundle().getString("closure.history.status.excluded")
        ));
        cmbStatusFilter.setValue(container.getBundle().getString("closure.history.status.all"));
        cmbStatusFilter.setOnAction(e -> applyFilters());
    }

    private void setupTable() {
        colCreated.setCellValueFactory(
                c -> new SimpleStringProperty(c.getValue().getCreatedAt().format(dateTimeFormatter)));
        colUser.setCellValueFactory(new PropertyValueFactory<>("username"));

        setupStatusColumn();
        setupCurrencyColumn(colInitialFund, "initialFund");
        setupCurrencyColumn(colExpected, "expectedCash");
        setupCurrencyColumn(colActual, "actualCash");
        setupDifferenceColumn();
    }

    private void setupStatusColumn() {
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String statusKey = "closure.history.status.all"; // Default
                    if ("CUADRADO".equals(item)) statusKey = "closure.history.status.squared";
                    else if ("DESCUADRE".equals(item)) statusKey = "closure.history.status.offset";
                    else if ("REVISADO".equals(item)) statusKey = "closure.history.status.reviewed";
                    else if ("EXCLUIDO".equals(item)) statusKey = "closure.history.status.excluded";
                    
                    Label label = new Label(container.getBundle().getString(statusKey));
                    label.getStyleClass().add("badge-info");
                    if ("DESCUADRE".equals(item))
                        label.setStyle(
                                "-fx-background-color: -fx-custom-color-danger-bg; -fx-text-fill: -fx-custom-color-danger-dark;");
                    else if ("CUADRADO".equals(item))
                        label.setStyle(
                                "-fx-background-color: -fx-custom-color-success-bg; -fx-text-fill: -fx-custom-color-success-dark;");
                    else if ("REVISADO".equals(item))
                        label.setStyle(
                                "-fx-background-color: -fx-custom-color-primary-bg; -fx-text-fill: -fx-custom-color-primary-dark;");
                    else if ("EXCLUIDO".equals(item))
                        label.setStyle(
                                "-fx-background-color: #f1f5f9; -fx-text-fill: #64748b;");
                    setGraphic(label);
                }
            }
        });
    }

    private void setupDifferenceColumn() {
        colDifference.setCellValueFactory(
                c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getDifference()));
        colDifference.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%+.2f \u20ac", item));
                    if (Math.abs(item) < 0.01)
                        setStyle("-fx-text-fill: -fx-custom-color-success-dark; -fx-font-weight: bold;");
                    else
                        setStyle("-fx-text-fill: -fx-custom-color-danger-dark; -fx-font-weight: bold;");
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
                else
                    setText(String.format("%.2f \u20ac", item));
            }
        });
    }

    private void setupMovementsTable() {
        colMovCreated.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        colMovCreated.setCellFactory(col -> new TableCell<>() {
            private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.format(timeFormatter));
                }
            }
        });

        colMovType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colMovType.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    String typeKey = "closure.history.mov.type.in"; // Default
                    if ("RETIRADA".equals(item)) typeKey = "closure.history.mov.type.out";
                    else if ("DEVOLUCION".equals(item)) typeKey = "closure.history.mov.type.return";
                    
                    Label label = new Label(container.getBundle().getString(typeKey));
                    label.setStyle(
                            "-fx-font-size: 10px; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-weight: bold;");
                    if ("INGRESO".equals(item)) {
                        label.setStyle(label.getStyle()
                                + "-fx-background-color: -fx-custom-color-success-bg; -fx-text-fill: -fx-custom-color-success-dark;");
                    } else if ("RETIRADA".equals(item)) {
                        label.setStyle(label.getStyle()
                                + "-fx-background-color: -fx-custom-color-danger-bg; -fx-text-fill: -fx-custom-color-danger-dark;");
                    } else if ("DEVOLUCION".equals(item)) {
                        label.setStyle(label.getStyle()
                                + "-fx-background-color: -fx-custom-color-warning-bg; -fx-text-fill: -fx-custom-color-warning-dark;");
                    } else {
                        label.setStyle(label.getStyle()
                                + "-fx-background-color: -fx-neutral-100; -fx-text-fill: -fx-neutral-700;");
                    }
                    setGraphic(label);
                }
            }
        });

        colMovReason.setCellValueFactory(new PropertyValueFactory<>("reason"));
        colMovReason.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    setTooltip(new Tooltip(item));
                }
            }
        });
        colMovUser.setCellValueFactory(new PropertyValueFactory<>("username"));

        colMovAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colMovAmount.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    CashMovement mov = getTableRow().getItem();
                    if (mov != null && ("RETIRADA".equals(mov.getType()) || "DEVOLUCION".equals(mov.getType()))) {
                        setText(String.format("-%.2f \u20ac", item));
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    } else {
                        setText(String.format("+%.2f \u20ac", item));
                        setStyle("-fx-text-fill: #16a34a; -fx-font-weight: bold;");
                    }
                }
            }
        });
    }

    @FXML
    public void loadClosures() {
        try {
            LocalDate start = datePickerStart.getValue();
            LocalDate end = datePickerEnd.getValue();

            List<CashClosure> closures;
            if (start == null || end == null) {
                // If Todo is selected, we fetch a wide range or all.
                // Using 2000-01-01 to 2100-01-01 as a safe "All" range if the repository
                // requires dates.
                closures = closureUseCase.getHistory(LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1));
            } else {
                closures = closureUseCase.getHistory(start, end);
            }
            allClosures.setAll(closures);
            applyFilters();
            updateKPIs();
            handleCloseDetails();
        } catch (SQLException e) {
            AlertUtil.showError(container.getBundle().getString("alert.error"), container.getBundle().getString("closure.history.error.load"));
        }
    }

    private void applyFilters() {
        String status = cmbStatusFilter.getValue();
        String allText = container.getBundle().getString("closure.history.status.all");
        String squaredText = container.getBundle().getString("closure.history.status.squared");
        String offsetText = container.getBundle().getString("closure.history.status.offset");
        String reviewedText = container.getBundle().getString("closure.history.status.reviewed");
        String excludedText = container.getBundle().getString("closure.history.status.excluded");

        List<CashClosure> filtered = allClosures.stream()
                .filter(c -> {
                    if (allText.equals(status)) return true;
                    if (squaredText.equals(status)) return "CUADRADO".equals(c.getStatus());
                    if (offsetText.equals(status)) return "DESCUADRE".equals(c.getStatus());
                    if (reviewedText.equals(status)) return "REVISADO".equals(c.getStatus());
                    if (excludedText.equals(status)) return "EXCLUIDO".equals(c.getStatus());
                    return false;
                })
                .collect(Collectors.toList());
        paginationHelper.setData(filtered);
    }

    private void updateKPIs() {
        double totalDiff = allClosures.stream()
                .filter(c -> !"EXCLUIDO".equals(c.getStatus()))
                .mapToDouble(CashClosure::getDifference)
                .sum();
        long pending = allClosures.stream()
                .filter(c -> !"REVISADO".equals(c.getStatus()))
                .count();

        lblTotalClosures.setText(String.valueOf(allClosures.size()));
        lblTotalDifference.setText(String.format("%+.2f \u20ac", totalDiff));
        lblPendingReview.setText(String.valueOf(pending));

        try {
            lblCurrentCash.setText(String.format("%.2f \u20ac", closureUseCase.getCurrentCashInDrawer()));
        } catch (SQLException e) {
            lblCurrentCash.setText("---");
        }
    }

    private void showClosureDetails(CashClosure closure) {
        try {
            lblClosureDetailId.setText(String.format(container.getBundle().getString("closure.history.detail.title"), closure.getClosureId()));

            // Financial Breakdown
            lblDetInitial.setText(String.format("%.2f \u20ac", closure.getInitialFund()));
            // Simulaci\u00f3n de desglose si no hay campos espec\u00edficos a\u00fan
            // En una versi\u00f3n real, esto vendr\u00eda de una consulta agregada de movimientos
            lblDetSales.setText(String.format("%.2f \u20ac",
                    closure.getTotalCash() - closure.getInitialFund() - closure.getCashIn() + closure.getCashOut()));
            lblDetIn.setText(String.format("%.2f \u20ac", closure.getCashIn()));
            lblDetOut.setText(String.format("%.2f \u20ac", closure.getCashOut()));
            lblDetExpected.setText(String.format("%.2f \u20ac", closure.getExpectedCash()));
            lblDetActual.setText(String.format("%.2f \u20ac", closure.getActualCash()));
            lblDetNotes.setText(closure.getNotes() != null && !closure.getNotes().isEmpty() ? closure.getNotes()
                    : container.getBundle().getString("closure.history.detail.notes.none"));

            // Movements Table
            List<CashMovement> movements = closureUseCase.getMovementsByClosure(closure.getClosureId());
            tableMovements.setItems(FXCollections.observableArrayList(movements));

            btnMarkReviewed
                    .setDisable("REVISADO".equals(closure.getStatus()) || "EXCLUIDO".equals(closure.getStatus()));
            boolean isAdmin = userSession != null && userSession.getCurrentUser() != null
                    && ("ADMIN".equalsIgnoreCase(userSession.getCurrentUser().getRole())
                            || "Administrador".equalsIgnoreCase(userSession.getCurrentUser().getRole()));
            btnEditClosure.setDisable(!isAdmin);

            detailsPanel.setVisible(true);
            detailsPanel.setManaged(true);
        } catch (SQLException e) {
            AlertUtil.showError(container.getBundle().getString("alert.error"), container.getBundle().getString("closure.history.detail.error.load"));
        }
    }

    @FXML
    private void handleMarkReviewed() {
        CashClosure closure = tableClosures.getSelectionModel().getSelectedItem();
        if (closure == null)
            return;

        try {
            int reviewerId = userSession != null && userSession.getCurrentUser() != null
                    ? userSession.getCurrentUser().getUserId()
                    : 1;
            closureUseCase.markAsReviewed(closure.getClosureId(), reviewerId);
            AlertUtil.showInfo(container.getBundle().getString("closure.history.mark.success"),
                    container.getBundle().getString("closure.history.mark.success_msg"));
            loadClosures(); // Recargar para ver cambios
        } catch (SQLException e) {
            AlertUtil.showError(container.getBundle().getString("alert.error"), container.getBundle().getString("closure.history.mark.error"));
        }
    }

    @FXML
    private void handleEditClosure() {
        CashClosure closure = tableClosures.getSelectionModel().getSelectedItem();
        if (closure == null)
            return;

        com.mycompany.ventacontrolfx.presentation.controller.dialog.EditClosureDialogController controller = ModalService
                .showStandardModal(
                        "/view/dialog/edit_closure_dialog.fxml",
                        container.getBundle().getString("closure.history.btn.modify"),
                        container,
                        c -> {
                            ((com.mycompany.ventacontrolfx.presentation.controller.dialog.EditClosureDialogController) c)
                                     .init(closureUseCase, userSession, closure.getClosureId(), closure.getActualCash());
                         });

        if (controller != null && controller.isConfirmed()) {
            AlertUtil.showToast(container.getBundle().getString("closure.history.edit.success"));
            loadClosures();
        }
    }

    @FXML
    private void handleMarkAllReviewed() {
        String status = cmbStatusFilter.getValue();
        List<CashClosure> pendingClosures = allClosures.stream()
                .filter(c -> ("Todos".equals(status) || status.equals(c.getStatus()))
                        && !"REVISADO".equals(c.getStatus()))
                .collect(Collectors.toList());

        if (pendingClosures.isEmpty()) {
            return;
        }

        boolean confirm = AlertUtil.showConfirmation(container.getBundle().getString("closure.history.bulk.confirm.title"), 
                container.getBundle().getString("closure.history.bulk.confirm.header"),
                String.format(container.getBundle().getString("closure.history.bulk.confirm.msg"), pendingClosures.size()));
        if (!confirm)
            return;

        try {
            int reviewerId = userSession != null && userSession.getCurrentUser() != null
                    ? userSession.getCurrentUser().getUserId()
                    : 1;
            for (CashClosure closure : pendingClosures) {
                closureUseCase.markAsReviewed(closure.getClosureId(), reviewerId);
            }
            AlertUtil.showToast(String.format(container.getBundle().getString("closure.history.bulk.success"), pendingClosures.size()));
            loadClosures();
        } catch (SQLException e) {
            AlertUtil.showError(container.getBundle().getString("alert.error"), container.getBundle().getString("closure.history.bulk.error"));
        }
    }

    @FXML
    private void handleCloseDetails() {
        detailsPanel.setVisible(false);
        detailsPanel.setManaged(false);
        tableClosures.getSelectionModel().clearSelection();
    }

    @FXML
    private void handlePrintAudit() {
        CashClosure closure = tableClosures.getSelectionModel().getSelectedItem();
        if (closure != null) {
            handlePrintClosure(closure);
        } else {
            AlertUtil.showWarning(container.getBundle().getString("closure.history.print.warning.title"), 
                    container.getBundle().getString("closure.history.print.warning.msg"));
        }
    }

    private void handlePrintClosure(CashClosure closure) {
        if (closure == null)
            return;

        ModalService.showStandardModal(
                "/view/print_preview.fxml",
                String.format(container.getBundle().getString("closure.history.print.preview.title"), closure.getClosureId()),
                container,
                (PrintPreviewController controller) -> {
                    controller.setClosureData(closure);
                });
    }
}
