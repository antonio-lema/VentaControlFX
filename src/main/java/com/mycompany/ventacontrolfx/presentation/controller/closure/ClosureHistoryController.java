package com.mycompany.ventacontrolfx.presentation.controller.closure;

import com.mycompany.ventacontrolfx.domain.model.CashClosure;
import com.mycompany.ventacontrolfx.domain.repository.ICashClosureRepository.CashMovement;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.shared.util.DateFilterUtils;
import com.mycompany.ventacontrolfx.shared.util.PaginationHelper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.time.LocalDateTime;

public class ClosureHistoryController implements Injectable {

    @FXML private DatePicker datePickerStart, datePickerEnd;
    @FXML private HBox quickFilterContainer;
    @FXML private ComboBox<String> cmbStatusFilter;
    @FXML private ComboBox<Integer> cmbRowLimit;
    @FXML private TableView<CashClosure> tableClosures;
    @FXML private TableColumn<CashClosure, String> colStatus, colCreated, colUser;
    @FXML private TableColumn<CashClosure, Double> colInitialFund, colExpected, colActual, colDifference;
    @FXML private VBox detailsPanel;
    @FXML private Label lblClosureDetailId, lblTotalClosures, lblTotalDifference, lblPendingReview, lblCurrentCash, lblCount;
    @FXML private Label lblDetInitial, lblDetSales, lblDetIn, lblDetOut, lblDetExpected, lblDetActual, lblDetNotes;
    @FXML private Button btnMarkReviewed, btnEditClosure;
    @FXML private TableView<CashMovement> tableMovements;
    @FXML private TableColumn<CashMovement, String> colMovType, colMovReason, colMovUser;
    @FXML private TableColumn<CashMovement, LocalDateTime> colMovCreated;
    @FXML private TableColumn<CashMovement, Double> colMovAmount;

    private ServiceContainer container;
    private ClosureTableManager tableManager;
    private ClosureDataManager dataManager;
    private ClosureDetailsManager detailsManager;
    private ClosureActionManager actionManager;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        
        // 1. Inicializar Managers
        this.tableManager = new ClosureTableManager(container, tableClosures, tableMovements);
        this.tableManager.setup(colStatus, colCreated, colUser, colInitialFund, colExpected, colActual, colDifference);
        this.tableManager.setupMovements(colMovType, colMovReason, colMovUser, colMovCreated, colMovAmount);

        PaginationHelper<CashClosure> pagination = new PaginationHelper<>(tableClosures, cmbRowLimit, lblCount, container.getBundle().getString("closure.history.label.arqueos"));
        this.dataManager = new ClosureDataManager(container, pagination);
        
        this.detailsManager = new ClosureDetailsManager(container, container.getUserSession(), detailsPanel, 
                                                       lblClosureDetailId, lblDetInitial, lblDetSales, lblDetIn, lblDetOut, 
                                                       lblDetExpected, lblDetActual, lblDetNotes, btnMarkReviewed, btnEditClosure);
        
        this.actionManager = new ClosureActionManager(container);

        // 2. Configuración de UI
        setupFilters();
        DateFilterUtils.addQuickFilters(quickFilterContainer, datePickerStart, datePickerEnd, container.getBundle(), this::loadClosures);
        
        tableClosures.getSelectionModel().selectedItemProperty().addListener((obs, old, nv) -> {
            if (nv != null) detailsManager.show(nv, tableManager);
        });

        loadClosures();
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

    @FXML public void loadClosures() {
        dataManager.load(datePickerStart.getValue(), datePickerEnd.getValue(), () -> {
            applyFilters();
            dataManager.updateKPIs(lblTotalClosures, lblTotalDifference, lblPendingReview, lblCurrentCash);
            handleCloseDetails();
        });
    }

    @FXML private void applyFilters() { dataManager.applyFilter(cmbStatusFilter.getValue()); }

    @FXML private void handleMarkReviewed() {
        actionManager.markAsReviewed(tableClosures.getSelectionModel().getSelectedItem(), this::loadClosures);
    }

    @FXML private void handleEditClosure() {
        actionManager.editClosure(tableClosures.getSelectionModel().getSelectedItem(), this::loadClosures);
    }

    @FXML private void handleMarkAllReviewed() {
        actionManager.markAllAsReviewed(dataManager.getAll(), cmbStatusFilter.getValue(), this::loadClosures);
    }

    @FXML private void handlePrintAudit() {
        actionManager.printAudit(tableClosures.getSelectionModel().getSelectedItem());
    }

    @FXML private void handleCloseDetails() {
        detailsManager.hide();
        tableClosures.getSelectionModel().clearSelection();
    }
}


