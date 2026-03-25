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

        datePickerStart.setValue(LocalDate.now().minusDays(7));
        datePickerEnd.setValue(LocalDate.now());

        setupTable();
        setupMovementsTable();
        setupFilters();
        DateFilterUtils.addQuickFilters(quickFilterContainer, datePickerStart,
                datePickerEnd, this::loadClosures);
        paginationHelper = new PaginationHelper<>(tableClosures, cmbRowLimit, lblCount, "arqueos");
        loadClosures();

        tableClosures.getSelectionModel().selectedItemProperty().addListener((obs, old, nv) -> {
            if (nv != null)
                showClosureDetails(nv);
        });
    }

    private void setupFilters() {
        cmbStatusFilter
                .setItems(FXCollections.observableArrayList("Todos", "CUADRADO", "DESCUADRE", "REVISADO", "EXCLUIDO"));
        cmbStatusFilter.setValue("Todos");
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
                    Label label = new Label(item);
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
                    setText(String.format("%+.2f €", item));
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
                    setText(String.format("%.2f €", item));
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
                    Label label = new Label(item);
                    label.setStyle(
                            "-fx-font-size: 10px; -fx-padding: 2 6; -fx-background-radius: 4; -fx-font-weight: bold;");
                    if ("INGRESO".equals(item)) {
                        label.setStyle(label.getStyle()
                                + "-fx-background-color: -fx-custom-color-success-bg; -fx-text-fill: -fx-custom-color-success-dark;");
                        label.setText("INGRESO");
                    } else if ("RETIRADA".equals(item)) {
                        label.setStyle(label.getStyle()
                                + "-fx-background-color: -fx-custom-color-danger-bg; -fx-text-fill: -fx-custom-color-danger-dark;");
                        label.setText("RETIRADA");
                    } else if ("DEVOLUCION".equals(item)) {
                        label.setStyle(label.getStyle()
                                + "-fx-background-color: -fx-custom-color-warning-bg; -fx-text-fill: -fx-custom-color-warning-dark;");
                        label.setText("DEV.");
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
                        setText(String.format("-%.2f €", item));
                        setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold;");
                    } else {
                        setText(String.format("+%.2f €", item));
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
            AlertUtil.showError("Error", "No se pudieron cargar los cierres.");
        }
    }

    private void applyFilters() {
        String status = cmbStatusFilter.getValue();
        List<CashClosure> filtered = allClosures.stream()
                .filter(c -> "Todos".equals(status) || status.equals(c.getStatus()))
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
        lblTotalDifference.setText(String.format("%+.2f €", totalDiff));
        lblPendingReview.setText(String.valueOf(pending));

        try {
            lblCurrentCash.setText(String.format("%.2f €", closureUseCase.getCurrentCashInDrawer()));
        } catch (SQLException e) {
            lblCurrentCash.setText("---");
        }
    }

    private void showClosureDetails(CashClosure closure) {
        try {
            lblClosureDetailId.setText("AUDITORÍA CIERRE #" + closure.getClosureId());

            // Financial Breakdown
            lblDetInitial.setText(String.format("%.2f €", closure.getInitialFund()));
            // Simulación de desglose si no hay campos específicos aún
            // En una versión real, esto vendría de una consulta agregada de movimientos
            lblDetSales.setText(String.format("%.2f €",
                    closure.getTotalCash() - closure.getInitialFund() - closure.getCashIn() + closure.getCashOut()));
            lblDetIn.setText(String.format("%.2f €", closure.getCashIn()));
            lblDetOut.setText(String.format("%.2f €", closure.getCashOut()));
            lblDetExpected.setText(String.format("%.2f €", closure.getExpectedCash()));
            lblDetActual.setText(String.format("%.2f €", closure.getActualCash()));
            lblDetNotes.setText(closure.getNotes() != null && !closure.getNotes().isEmpty() ? closure.getNotes()
                    : "Sin observaciones.");

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
            AlertUtil.showError("Error", "No se pudieron cargar los detalles.");
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
            AlertUtil.showInfo("Éxito", "El cierre ha sido marcado como revisado.");
            loadClosures(); // Recargar para ver cambios
        } catch (SQLException e) {
            AlertUtil.showError("Error", "No se pudo actualizar el estado.");
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
                        "Modificar Arqueo",
                        container,
                        c -> {
                            ((com.mycompany.ventacontrolfx.presentation.controller.dialog.EditClosureDialogController) c)
                                    .init(closureUseCase, userSession, closure.getClosureId(), closure.getActualCash());
                        });

        if (controller != null && controller.isConfirmed()) {
            AlertUtil.showToast("Cierre modificado correctamente.");
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

        boolean confirm = AlertUtil.showConfirmation("Confirmar", "Revisión masiva de arqueos",
                "¿Estás seguro de marcar " + pendingClosures.size() + " arqueos como revisados?");
        if (!confirm)
            return;

        try {
            int reviewerId = userSession != null && userSession.getCurrentUser() != null
                    ? userSession.getCurrentUser().getUserId()
                    : 1;
            for (CashClosure closure : pendingClosures) {
                closureUseCase.markAsReviewed(closure.getClosureId(), reviewerId);
            }
            AlertUtil.showToast(pendingClosures.size() + " arqueos marcados como revisados.");
            loadClosures();
        } catch (SQLException e) {
            AlertUtil.showError("Error", "No se pudieron actualizar todos los estados.");
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
            AlertUtil.showWarning("Imprimir", "Seleccione un cierre para imprimir.");
        }
    }

    private void handlePrintClosure(CashClosure closure) {
        if (closure == null)
            return;

        ModalService.showStandardModal(
                "/view/print_preview.fxml",
                "Vista Previa de Impresión - Cierre #" + closure.getClosureId(),
                container,
                (PrintPreviewController controller) -> {
                    controller.setClosureData(closure);
                });
    }
}
