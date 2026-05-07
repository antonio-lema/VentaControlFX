package com.mycompany.ventacontrolfx.presentation.controller.dashboard;

import com.mycompany.ventacontrolfx.domain.model.User;
import com.mycompany.ventacontrolfx.domain.model.WorkSession;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OperativeDashboardController implements Injectable {

    @FXML private Label lblActiveStaffCount, lblCashStatus, lblCashAmount, lblDailyOpenTime, lblBusinessStatus, lblServerTime, lblScheduleCompliance;
    @FXML private VBox vboxTodaySchedule, vboxAlerts, skeletonStaffContainer;
    @FXML private TableView<DashboardStaffManager.ActiveStaffViewModel> tableActiveStaff;
    @FXML private TableColumn<DashboardStaffManager.ActiveStaffViewModel, String> colStaffName, colStaffRole, colStaffStart, colStaffShift, colStaffDuration;
    @FXML private TableColumn<DashboardStaffManager.ActiveStaffViewModel, javafx.scene.Node> colStaffProgress, colStaffStatus;

    private ServiceContainer container;
    private DashboardStaffManager staffManager;
    private DashboardScheduleManager scheduleManager;
    private DashboardUIManager uiManager;
    private DashboardDataManager dataManager;
    private List<User> allUsers;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        try { this.allUsers = container.getUserUseCase().getAllUsers(); } catch (Exception e) { e.printStackTrace(); }

        // 1. Inicializar Managers
        this.staffManager = new DashboardStaffManager(container, tableActiveStaff);
        this.scheduleManager = new DashboardScheduleManager(container, vboxTodaySchedule, vboxAlerts, lblBusinessStatus, lblScheduleCompliance);
        this.uiManager = new DashboardUIManager(container, lblActiveStaffCount, lblCashStatus, lblCashAmount, lblDailyOpenTime, lblScheduleCompliance, skeletonStaffContainer, vboxTodaySchedule, tableActiveStaff);
        this.dataManager = new DashboardDataManager(container);

        // 2. Setup
        staffManager.setup(colStaffName, colStaffRole, colStaffStart, colStaffShift, colStaffDuration, colStaffProgress, colStaffStatus);
        
        refreshData();
        dataManager.startAutoRefresh(lblServerTime, this::refreshData);
    }

    @FXML
    public void refreshData() {
        uiManager.showSkeletons(true);
        dataManager.fetchData(result -> {
            uiManager.showSkeletons(false);
            
            String firstStartStr = "--:--";
            if (!result.allSessionsToday().isEmpty()) {
                firstStartStr = result.allSessionsToday().get(result.allSessionsToday().size() - 1)
                                      .getStartTime().format(DateTimeFormatter.ofPattern("HH:mm"));
            }

            uiManager.updateKPIs(result.activeSessions().size(), result.isCashOpen(), result.currentCash(), firstStartStr);
            staffManager.update(result.activeSessions(), result.allSessionsToday(), allUsers, result.schedule());
            scheduleManager.update(result.schedule(), result.allSessionsToday().isEmpty() ? null : result.allSessionsToday().get(result.allSessionsToday().size() - 1).getStartTime(), allUsers, result.activeSessions());
            
        }, e -> { uiManager.showSkeletons(false); e.printStackTrace(); });
    }

    @FXML
    private void handlePartialClosure() {
        if (!container.getUserSession().hasPermission("caja.cerrar")) {
            AlertUtil.showError(container.getBundle().getString("alert.error"), container.getBundle().getString("auth.error.no_permission"));
            return;
        }
        try {
            container.getClosureUseCase().performPartialClosure(container.getUserSession().getCurrentUser().getUserId());
            AlertUtil.showInfo(container.getBundle().getString("dashboard.partial.closure.title"), container.getBundle().getString("dashboard.partial.closure.msg"));
            refreshData();
        } catch (Exception e) {
            AlertUtil.showError(container.getBundle().getString("alert.error"), container.getBundle().getString("closure.error.generic") + ": " + e.getMessage());
        }
    }

    @FXML
    private void handleShowAudit() {
        if (container.getNavigationService() != null) container.getNavigationService().navigateTo("/view/reports/punctuality_audit.fxml");
    }

    public void stopRefresh() { dataManager.stop(); }
}


