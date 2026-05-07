package com.mycompany.ventacontrolfx.presentation.controller.user;

import com.mycompany.ventacontrolfx.domain.model.BusinessDay;
import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
import com.mycompany.ventacontrolfx.domain.model.User;
import com.mycompany.ventacontrolfx.domain.model.WorkSession;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.controller.dialog.ClosureReminderController;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Gestor de monitoreo de turnos, puntualidad y bloqueos de seguridad.
 * Encapsula toda la lógica de tiempo y horarios del MainController.
 */
public class ShiftMonitorManager {

    private final ServiceContainer container;
    private final Set<Integer> notifiedLateUserIds = new HashSet<>();
    private LocalDate lastLateCheckDate = LocalDate.now();
    private LocalDateTime snoozeUntil = null;
    private boolean closureDialogVisible = false;

    private static final int LATE_CLOCK_IN_GRACE_MINUTES = 15;
    private static final int SALES_BLOCK_GRACE_MINUTES = 60;

    public ShiftMonitorManager(ServiceContainer container) {
        this.container = container;
    }

    public void start() {
        Platform.runLater(this::runAllChecks);

        Timeline timeline = new Timeline(new KeyFrame(Duration.minutes(1), event -> runAllChecks()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void runAllChecks() {
        checkShiftEnd();
        checkLateClockIns();
        checkSalesBlock();
    }

    private void checkShiftEnd() {
        if (closureDialogVisible || (snoozeUntil != null && LocalDateTime.now().isBefore(snoozeUntil))) return;

        User user = container.getUserSession().getCurrentUser();
        if (user == null || !user.hasPermission("CIERRES")) return;

        try {
            if (!container.getClosureUseCase().hasActiveFund() && container.getClosureUseCase().getTodayTransactionCount() == 0) return;
        } catch (Exception e) { return; }

        SaleConfig cfg = container.getConfigUseCase().getConfig();
        if (cfg == null) return;

        BusinessDay today = getTodayBusinessDay(cfg);
        if (today == null || today.isClosed()) return;

        LocalTime now = LocalTime.now();
        boolean shiftEnded = false;

        Optional<LocalTime> myEnd = today.getShifts().stream()
                .filter(r -> r.getAssignedUserIds().contains(user.getUserId()))
                .map(BusinessDay.TimeRange::getClose).max(LocalTime::compareTo);

        if (myEnd.isPresent()) {
            if (now.isAfter(myEnd.get())) shiftEnded = true;
        } else if (user.isAdmin()) {
            Optional<LocalTime> absoluteLatest = today.getShifts().stream().map(BusinessDay.TimeRange::getClose).max(LocalTime::compareTo);
            if (absoluteLatest.isPresent() && now.isAfter(absoluteLatest.get())) shiftEnded = true;
        }

        if (shiftEnded) showClosureReminder();
    }

    private void checkLateClockIns() {
        if (!LocalDate.now().equals(lastLateCheckDate)) {
            notifiedLateUserIds.clear();
            lastLateCheckDate = LocalDate.now();
        }

        User currentUser = container.getUserSession().getCurrentUser();
        if (currentUser == null || !currentUser.isAdmin()) return;

        SaleConfig cfg = container.getConfigUseCase().getConfig();
        BusinessDay today = getTodayBusinessDay(cfg);
        if (today == null || today.isClosed()) return;

        LocalTime now = LocalTime.now();
        List<String> lateUsers = new java.util.ArrayList<>();
        List<WorkSession> activeSessions = container.getWorkSessionUseCase().getAllActiveSessions();

        for (BusinessDay.TimeRange shift : today.getShifts()) {
            if (now.isAfter(shift.getOpen().plusMinutes(LATE_CLOCK_IN_GRACE_MINUTES)) && now.isBefore(shift.getClose())) {
                for (Integer userId : shift.getAssignedUserIds()) {
                    boolean isIn = activeSessions.stream().anyMatch(s -> s.getUserId() == userId && s.getType() == WorkSession.SessionType.SHIFT);
                    if (!isIn && !notifiedLateUserIds.contains(userId)) {
                        try {
                            User u = container.getUserUseCase().getUserById(userId);
                            if (u != null) { lateUsers.add(u.getFullName()); notifiedLateUserIds.add(userId); }
                        } catch (Exception e) {}
                    }
                }
            }
        }

        if (!lateUsers.isEmpty()) {
            String msg = container.getBundle().getString("main.attendance.late_warning") + "\n" + String.join(", ", lateUsers);
            Platform.runLater(() -> com.mycompany.ventacontrolfx.presentation.util.AlertUtil.showWarning(container.getBundle().getString("main.attendance.title"), msg));
        }
    }

    private void checkSalesBlock() {
        User user = container.getUserSession().getCurrentUser();
        if (user == null || user.isAdmin()) { unlockCart(false); return; }

        try {
            if (!container.getClosureUseCase().hasActiveFund()) { unlockCart(false); return; }
        } catch (Exception e) { unlockCart(false); return; }

        SaleConfig cfg = container.getConfigUseCase().getConfig();
        BusinessDay today = getTodayBusinessDay(cfg);
        if (today == null || today.isClosed()) { unlockCart(false); return; }

        LocalTime now = LocalTime.now();
        Optional<LocalTime> latestEnd = today.getShifts().stream()
                .filter(r -> r.getAssignedUserIds().contains(user.getUserId()))
                .map(BusinessDay.TimeRange::getClose).max(LocalTime::compareTo);

        if (latestEnd.isPresent() && now.isAfter(latestEnd.get().plusMinutes(SALES_BLOCK_GRACE_MINUTES))) {
            unlockCart(true);
        } else {
            unlockCart(false);
        }
    }

    private void unlockCart(boolean locked) {
        Platform.runLater(() -> {
            if (container.getCartUseCase() != null) container.getCartUseCase().setLocked(locked);
        });
    }

    private void showClosureReminder() {
        closureDialogVisible = true;
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/dialog/closure_reminder.fxml"), container.getBundle());
                Parent root = loader.load();
                ClosureReminderController controller = loader.getController();

                Stage stage = new Stage(StageStyle.TRANSPARENT);
                stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                stage.setTitle(container.getBundle().getString("main.shift_end.title"));

                Scene scene = new Scene(root); scene.setFill(null);
                container.getThemeManager().applyFullTheme(scene);
                stage.setScene(scene);

                controller.setStage(stage);
                controller.setOnAction(action -> {
                    switch (action) {
                        case "CLOSE_NOW": container.getNavigationService().navigateTo("/view/closure/cash_closure.fxml"); break;
                        case "SNOOZE_5": snoozeUntil = LocalDateTime.now().plusMinutes(5); break;
                        case "SNOOZE_15": snoozeUntil = LocalDateTime.now().plusMinutes(15); break;
                        case "SNOOZE_60": snoozeUntil = LocalDateTime.now().plusHours(1); break;
                    }
                });
                stage.showAndWait();
            } catch (Exception e) { e.printStackTrace(); }
            finally { closureDialogVisible = false; }
        });
    }

    private BusinessDay getTodayBusinessDay(SaleConfig cfg) {
        if (cfg == null) return null;
        int idx = LocalDate.now().getDayOfWeek().getValue();
        return cfg.getSchedule().stream().filter(d -> d.getDayOfWeek() == idx).findFirst().orElse(null);
    }
}



