package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.application.usecase.CashClosureUseCase;
import com.mycompany.ventacontrolfx.application.usecase.WorkSessionUseCase;
import com.mycompany.ventacontrolfx.application.usecase.ConfigUseCase;
import com.mycompany.ventacontrolfx.domain.model.WorkSession;
import com.mycompany.ventacontrolfx.domain.model.BusinessDay;
import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.application.usecase.UserUseCase;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.SpanishHolidays;
import com.mycompany.ventacontrolfx.domain.model.User;
import java.time.LocalTime;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class OperativeDashboardController implements Injectable {

    @FXML
    private Label lblActiveStaffCount;
    @FXML
    private Label lblCashStatus;
    @FXML
    private Label lblCashAmount;
    @FXML
    private Label lblDailyOpenTime;
    @FXML
    private Label lblBusinessStatus;
    @FXML
    private Label lblServerTime;
    @FXML
    private Label lblScheduleCompliance;
    @FXML
    private VBox vboxTodaySchedule;
    @FXML
    private VBox vboxAlerts;
    @FXML
    private TableView<ActiveStaffViewModel> tableActiveStaff;

    @FXML
    private TableColumn<ActiveStaffViewModel, String> colStaffName;
    @FXML
    private TableColumn<ActiveStaffViewModel, String> colStaffRole;
    @FXML
    private TableColumn<ActiveStaffViewModel, String> colStaffStart;
    @FXML
    private TableColumn<ActiveStaffViewModel, String> colStaffShift;
    @FXML
    private TableColumn<ActiveStaffViewModel, String> colStaffDuration;
    @FXML
    private TableColumn<ActiveStaffViewModel, javafx.scene.Node> colStaffProgress;
    @FXML
    private TableColumn<ActiveStaffViewModel, javafx.scene.Node> colStaffStatus;

    private WorkSessionUseCase workSessionUseCase;
    private CashClosureUseCase cashClosureUseCase;
    private ConfigUseCase configUseCase;
    private UserUseCase userUseCase;
    private ServiceContainer container;
    private List<User> allUsers;
    private Timer refreshTimer;
    private final ObservableList<ActiveStaffViewModel> activeStaffList = FXCollections.observableArrayList();

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.workSessionUseCase = container.getWorkSessionUseCase();
        this.cashClosureUseCase = container.getClosureUseCase();
        this.configUseCase = container.getConfigUseCase();
        this.userUseCase = container.getUserUseCase();
        try {
            this.allUsers = userUseCase.getAllUsers();
        } catch (Exception e) {
            e.printStackTrace();
        }

        setupTable();
        refreshData();
        startAutoRefresh();
    }

    private void setupTable() {
        colStaffName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colStaffRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStaffStart.setCellValueFactory(new PropertyValueFactory<>("start"));
        colStaffShift.setCellValueFactory(new PropertyValueFactory<>("shift"));
        colStaffDuration.setCellValueFactory(new PropertyValueFactory<>("duration"));
        colStaffProgress.setCellValueFactory(new PropertyValueFactory<>("progressNode"));
        colStaffStatus.setCellValueFactory(new PropertyValueFactory<>("statusNode"));
        tableActiveStaff.setItems(activeStaffList);
    }

    @FXML
    public void refreshData() {
        try {
            // 1. Business Schedule & Compliance
            SaleConfig cfg = configUseCase.getConfig();
            int todayIdx = LocalDate.now().getDayOfWeek().getValue();
            final BusinessDay todaySchedule = (cfg != null) ? cfg.getSchedule().stream()
                    .filter(d -> d.getDayOfWeek() == todayIdx)
                    .findFirst()
                    .orElse(null) : null;

            // 2. Staff Status
            List<WorkSession> allSessionsToday = workSessionUseCase.getHistoryByDate(LocalDate.now());
            List<WorkSession> activeSessions = workSessionUseCase.getAllActiveSessions();
            Platform.runLater(() -> {
                lblActiveStaffCount.setText(String.valueOf(activeSessions.size()));
                activeStaffList.setAll(activeSessions.stream()
                        .map(s -> {
                            User u = allUsers != null
                                    ? allUsers.stream().filter(usr -> usr.getUserId() == s.getUserId()).findFirst()
                                            .orElse(null)
                                    : null;

                            List<WorkSession> userSessionsToday = allSessionsToday.stream()
                                    .filter(ws -> ws.getUserId().equals(s.getUserId()))
                                    .collect(Collectors.toList());

                            BusinessDay.TimeRange range = null;
                            if (todaySchedule != null) {
                                range = todaySchedule.getShifts().stream()
                                        .filter(r -> r.getAssignedUserIds().contains(s.getUserId()))
                                        .findFirst().orElse(null);
                            }
                            return new ActiveStaffViewModel(s, u, range, userSessionsToday, container.getBundle());
                        })
                        .collect(Collectors.toList()));
            });

            // 3. Cash Status
            boolean isCashOpen = cashClosureUseCase.hasActiveFund();
            double currentCash = cashClosureUseCase.getCurrentCashInDrawer();
            Platform.runLater(() -> {
                lblCashStatus
                        .setText(container.getBundle().getString("dashboard.cash." + (isCashOpen ? "open" : "closed")));
                lblCashStatus.setStyle(isCashOpen ? "-fx-text-fill: #16a34a;" : "-fx-text-fill: #ef4444;");
                lblCashAmount.setText(String.format("%.2f \u20ac", currentCash));
            });

            // 4. Business Opening Time
            List<WorkSession> todaySessions = workSessionUseCase.getHistoryByDate(LocalDate.now());
            LocalDateTime firstStart = null;
            if (!todaySessions.isEmpty()) {
                firstStart = todaySessions.get(todaySessions.size() - 1).getStartTime();
                final String fStart = firstStart.format(DateTimeFormatter.ofPattern("HH:mm"));
                Platform.runLater(() -> lblDailyOpenTime.setText(fStart));
            } else {
                Platform.runLater(() -> lblDailyOpenTime.setText("--:--"));
            }

            updateScheduleUI(todaySchedule, firstStart);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleShowAudit() {
        if (container != null && container.getNavigationService() != null) {
            container.getNavigationService().navigateTo("/view/punctuality_audit.fxml");
        }
    }

    @FXML
    private void handlePartialClosure() {
        try {
            cashClosureUseCase.performPartialClosure(container.getUserSession().getCurrentUser().getUserId());
            AlertUtil.showInfo(container.getBundle().getString("dashboard.partial.closure.title"),
                    container.getBundle().getString("dashboard.partial.closure.msg"));
        } catch (Exception e) {
            AlertUtil.showError(container.getBundle().getString("alert.error"),
                    container.getBundle().getString("closure.error.generic") + ": " + e.getMessage());
        }
    }

    private void updateScheduleUI(BusinessDay schedule, LocalDateTime actualStart) {
        Platform.runLater(() -> {
            vboxTodaySchedule.getChildren().clear();
            vboxAlerts.getChildren().clear();

            if (schedule == null || schedule.isClosed() || schedule.getShifts().isEmpty()) {
                lblBusinessStatus.setText(container.getBundle().getString(
                        "dashboard.status." + (schedule != null && schedule.isClosed() ? "closed" : "no_schedule")));
                lblBusinessStatus.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569;");
                lblScheduleCompliance.setText("N/A");
                vboxTodaySchedule.getChildren()
                        .add(new Label(container.getBundle().getString("dashboard.shifts.none")));
                return;
            }

            LocalTime now = LocalTime.now();
            boolean isOpen = schedule.getShifts().stream()
                    .anyMatch(s -> now.isAfter(s.getOpen()) && now.isBefore(s.getClose()));

            lblBusinessStatus
                    .setText(container.getBundle().getString("dashboard.status." + (isOpen ? "open" : "out_of_hours")));
            lblBusinessStatus.setStyle(isOpen ? "-fx-background-color: #dcfce7; -fx-text-fill: #16a34a;"
                    : "-fx-background-color: #fef3c7; -fx-text-fill: #d97706;");

            // Compliance check
            if (actualStart != null && !schedule.getShifts().isEmpty()) {
                LocalTime expected = schedule.getShifts().get(0).getOpen();
                if (actualStart.toLocalTime().isAfter(expected.plusMinutes(10))) {
                    lblScheduleCompliance.setText(container.getBundle().getString("dashboard.compliance.delay"));
                    lblScheduleCompliance.setStyle("-fx-text-fill: #ef4444;");
                    addAlert(String.format(container.getBundle().getString("dashboard.alert.late_opening"),
                            actualStart.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))), "warning");
                } else {
                    lblScheduleCompliance.setText(container.getBundle().getString("dashboard.compliance.optimal"));
                    lblScheduleCompliance.setStyle("-fx-text-fill: #16a34a;");
                }
            } else {
                lblScheduleCompliance.setText(container.getBundle().getString("dashboard.compliance.pending"));
                lblScheduleCompliance.setStyle("-fx-text-fill: #94a3b8;");
            }

            for (BusinessDay.TimeRange r : schedule.getShifts()) {
                String staffNames = r.getAssignedUserIds().stream()
                        .map(id -> allUsers.stream().filter(u -> u.getUserId() == id).map(User::getFullName).findFirst()
                                .orElse(String.format(container.getBundle().getString("dashboard.staff.user"), id)))
                        .collect(Collectors.joining(", "));

                VBox shiftBox = new VBox(5);
                shiftBox.setPadding(new Insets(10));
                shiftBox.setStyle(
                        "-fx-background-color: #f8fafc; -fx-background-radius: 8; -fx-border-color: #e2e8f0;");

                HBox timeRow = new HBox(10,
                        new de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView(
                                de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.CLOCK_ALT),
                        new Label(r.getOpen() + " - " + r.getClose()));
                timeRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                Label lblStaff = new Label(
                        staffNames.isEmpty() ? container.getBundle().getString("dashboard.alert.no_staff_assigned")
                                : staffNames);
                lblStaff.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-style: italic;");
                lblStaff.setWrapText(true);

                shiftBox.getChildren().addAll(timeRow, lblStaff);
                vboxTodaySchedule.getChildren().add(shiftBox);

                // Detection: Shift is active but nobody from assigned list is clocked in?
                if (now.isAfter(r.getOpen()) && now.isBefore(r.getClose())) {
                    List<WorkSession> active = workSessionUseCase.getAllActiveSessions();
                    for (Integer id : r.getAssignedUserIds()) {
                        if (active.stream().noneMatch(s -> s.getUserId() == id)) {
                            String name = allUsers.stream().filter(u -> u.getUserId() == id).map(User::getFullName)
                                    .findFirst()
                                    .orElse(String.format(container.getBundle().getString("dashboard.staff.user"), id));
                            addAlert(String.format(container.getBundle().getString("dashboard.alert.absence_detected"),
                                    name), "warning");
                        }
                    }
                }
            }

            // National Holiday Alert
            if (SpanishHolidays.isHoliday(LocalDate.now())) {
                addAlert(
                        String.format(container.getBundle().getString("dashboard.alert.holiday"),
                                container.getBundle().getString(SpanishHolidays.getHolidayName(LocalDate.now()))),
                        "info");
            }

            if (vboxAlerts.getChildren().isEmpty()) {
                addAlert(container.getBundle().getString("dashboard.alert.stable"), "info");
            }
        });
    }

    private void addAlert(String msg, String type) {
        HBox alert = new HBox(10, new de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView(
                type.equals("warning") ? de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.EXCLAMATION_TRIANGLE
                        : de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.INFO_CIRCLE),
                new Label(msg));
        alert.getStyleClass().addAll("alert-row", "alert-" + type);
        alert.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        vboxAlerts.getChildren().add(alert);
    }

    private void startAutoRefresh() {
        refreshTimer = new Timer(true);
        // Task for the clock (every second)
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                String timeNow = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                Platform.runLater(() -> lblServerTime.setText(timeNow));
            }
        }, 0, 1000);

        // Task for heavy data (every 30 seconds)
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                refreshData();
            }
        }, 1000, 30000);
    }

    public void stopRefresh() {
        if (refreshTimer != null)
            refreshTimer.cancel();
    }

    public static class ActiveStaffViewModel {
        private final String name;
        private final String role;
        private final String start;
        private final String shift;
        private final String duration;
        private final javafx.scene.Node progressNode;
        private final javafx.scene.Node statusNode;

        public ActiveStaffViewModel(WorkSession session, User user, BusinessDay.TimeRange scheduledRange,
                List<WorkSession> userSessionsToday, java.util.ResourceBundle bundle) {
            String uname = session.getUserName();
            this.name = (uname != null && !uname.isEmpty()) ? uname
                    : String.format(bundle.getString("dashboard.staff.user"), session.getUserId());
            this.role = (user != null) ? user.getRole() : "N/D";

            // First start time of the day (SHIFTS only)
            LocalDateTime firstStart = userSessionsToday.stream()
                    .filter(s -> s.getType() == WorkSession.SessionType.SHIFT)
                    .map(WorkSession::getStartTime)
                    .min(LocalDateTime::compareTo)
                    .orElse(session.getStartTime());

            this.start = firstStart.format(DateTimeFormatter.ofPattern("HH:mm"));
            this.shift = (scheduledRange != null) ? (scheduledRange.getOpen() + " - " + scheduledRange.getClose())
                    : bundle.getString("dashboard.staff.no_assigned");

            // Total worked seconds (SHIFTS only)
            long totalSeconds = userSessionsToday.stream()
                    .filter(s -> s.getType() == WorkSession.SessionType.SHIFT)
                    .mapToLong(s -> {
                        LocalDateTime end = s.getEndTime() != null ? s.getEndTime() : LocalDateTime.now();
                        return java.time.Duration.between(s.getStartTime(), end).getSeconds();
                    }).sum();

            long h = totalSeconds / 3600;
            long m = (totalSeconds % 3600) / 60;
            this.duration = String.format("%dh %dm", h, m);

            // Calcular progreso basado en la duración real del turno
            double shiftDurationHours = (scheduledRange != null && scheduledRange.getDurationHours() > 0)
                    ? scheduledRange.getDurationHours()
                    : 8.0;
            ProgressBar pb = new ProgressBar(Math.min(1.0, (double) totalSeconds / 3600.0 / shiftDurationHours));
            pb.setPrefWidth(100);

            // Colorear según cercanía al final
            if (pb.getProgress() > 0.9) {
                pb.setStyle("-fx-accent: #10b981;"); // Verde al terminar
            } else if (pb.getProgress() > 0.7) {
                pb.setStyle("-fx-accent: #f59e0b;"); // Ámbar al final
            } else {
                pb.setStyle("-fx-accent: #3b82f6;"); // Azul por defecto
            }
            this.progressNode = pb;

            Label lblStatus = new Label();
            lblStatus.setPadding(new Insets(2, 8, 2, 8));
            lblStatus.setStyle(
                    "-fx-background-radius: 10; -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: white;");

            LocalTime now = LocalTime.now();
            if (session.getType() == WorkSession.SessionType.BREAK) {
                lblStatus.setText(bundle.getString("dashboard.staff.status.break"));
                lblStatus.setStyle(lblStatus.getStyle() + "-fx-background-color: #f59e0b;"); // Amber
            } else if (scheduledRange != null) {
                if (now.isAfter(scheduledRange.getOpen()) && now.isBefore(scheduledRange.getClose())) {
                    lblStatus.setText(bundle.getString("dashboard.staff.status.shift"));
                    lblStatus.setStyle(lblStatus.getStyle() + "-fx-background-color: #10b981;"); // Green
                } else {
                    lblStatus.setText(bundle.getString("dashboard.staff.status.extras"));
                    lblStatus.setStyle(lblStatus.getStyle() + "-fx-background-color: #8b5cf6;"); // Purple
                }
            } else {
                lblStatus.setText(bundle.getString("dashboard.staff.status.out_of_prog"));
                lblStatus.setStyle(lblStatus.getStyle() + "-fx-background-color: #64748b;"); // Gray
            }
            this.statusNode = lblStatus;
        }

        public String getName() {
            return name;
        }

        public String getRole() {
            return role;
        }

        public String getStart() {
            return start;
        }

        public String getShift() {
            return shift;
        }

        public String getDuration() {
            return duration;
        }

        public javafx.scene.Node getProgressNode() {
            return progressNode;
        }

        public javafx.scene.Node getStatusNode() {
            return statusNode;
        }
    }
}
