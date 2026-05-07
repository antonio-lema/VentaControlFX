package com.mycompany.ventacontrolfx.presentation.controller.dashboard;

import com.mycompany.ventacontrolfx.domain.model.BusinessDay;
import com.mycompany.ventacontrolfx.domain.model.User;
import com.mycompany.ventacontrolfx.domain.model.WorkSession;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestor de la tabla de personal activo en el Dashboard.
 * Maneja la creación de ViewModels con lógica de progreso y estado.
 */
public class DashboardStaffManager {

    private final ServiceContainer container;
    private final TableView<ActiveStaffViewModel> table;
    private final ObservableList<ActiveStaffViewModel> dataList = FXCollections.observableArrayList();

    public DashboardStaffManager(ServiceContainer container, TableView<ActiveStaffViewModel> table) {
        this.container = container;
        this.table = table;
    }

    public void setup(TableColumn<ActiveStaffViewModel, String> colName, TableColumn<ActiveStaffViewModel, String> colRole,
                    TableColumn<ActiveStaffViewModel, String> colStart, TableColumn<ActiveStaffViewModel, String> colShift,
                    TableColumn<ActiveStaffViewModel, String> colDuration, TableColumn<ActiveStaffViewModel, Node> colProgress,
                    TableColumn<ActiveStaffViewModel, Node> colStatus) {
        
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colStart.setCellValueFactory(new PropertyValueFactory<>("start"));
        colStaffShift(colShift);
        colDuration.setCellValueFactory(new PropertyValueFactory<>("duration"));
        colProgress.setCellValueFactory(new PropertyValueFactory<>("progressNode"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("statusNode"));
        table.setItems(dataList);
    }

    private void colStaffShift(TableColumn<ActiveStaffViewModel, String> col) {
        col.setCellValueFactory(new PropertyValueFactory<>("shift"));
    }

    public void update(List<WorkSession> activeSessions, List<WorkSession> allSessionsToday, List<User> allUsers, BusinessDay todaySchedule) {
        List<ActiveStaffViewModel> models = activeSessions.stream()
                .map(s -> {
                    User u = allUsers.stream().filter(usr -> usr.getUserId() == s.getUserId()).findFirst().orElse(null);
                    List<WorkSession> userSessions = allSessionsToday.stream().filter(ws -> ws.getUserId().equals(s.getUserId())).toList();
                    
                    BusinessDay.TimeRange range = null;
                    if (todaySchedule != null) {
                        range = todaySchedule.getShifts().stream().filter(r -> r.getAssignedUserIds().contains(s.getUserId())).findFirst().orElse(null);
                    }
                    return new ActiveStaffViewModel(s, u, range, userSessions, container.getBundle());
                })
                .collect(Collectors.toList());
        dataList.setAll(models);
    }

    /**
     * ViewModel interno para representar a un empleado activo.
     */
    public static class ActiveStaffViewModel {
        private final String name, role, start, shift, duration;
        private final Node progressNode, statusNode;

        public ActiveStaffViewModel(WorkSession session, User user, BusinessDay.TimeRange scheduledRange, List<WorkSession> userSessionsToday, java.util.ResourceBundle bundle) {
            String uname = session.getUserName();
            this.name = (uname != null && !uname.isEmpty()) ? uname : String.format(bundle.getString("dashboard.staff.user"), session.getUserId());
            this.role = (user != null) ? user.getRole() : bundle.getString("NA");

            LocalDateTime firstStart = userSessionsToday.stream()
                    .filter(s -> s.getType() == WorkSession.SessionType.SHIFT)
                    .map(WorkSession::getStartTime).min(LocalDateTime::compareTo).orElse(session.getStartTime());

            this.start = firstStart.format(DateTimeFormatter.ofPattern("HH:mm"));
            this.shift = (scheduledRange != null) ? (scheduledRange.getOpen() + " - " + scheduledRange.getClose()) : bundle.getString("dashboard.staff.no_assigned");

            long totalSeconds = userSessionsToday.stream()
                    .filter(s -> s.getType() == WorkSession.SessionType.SHIFT)
                    .mapToLong(s -> Duration.between(s.getStartTime(), s.getEndTime() != null ? s.getEndTime() : LocalDateTime.now()).getSeconds()).sum();

            this.duration = String.format(bundle.getString("dashboard.staff.duration.format"), totalSeconds / 3600, (totalSeconds % 3600) / 60);

            double shiftHours = (scheduledRange != null && scheduledRange.getDurationHours() > 0) ? scheduledRange.getDurationHours() : 8.0;
            ProgressBar pb = new ProgressBar(Math.min(1.0, (double) totalSeconds / 3600.0 / shiftHours));
            pb.setPrefWidth(100);
            if (pb.getProgress() > 0.9) pb.setStyle("-fx-accent: #10b981;");
            else if (pb.getProgress() > 0.7) pb.setStyle("-fx-accent: #f59e0b;");
            else pb.setStyle("-fx-accent: #3b82f6;");
            this.progressNode = pb;

            Label lbl = new Label(); lbl.setPadding(new Insets(2, 8, 2, 8));
            lbl.setStyle("-fx-background-radius: 10; -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: white;");
            LocalTime now = LocalTime.now();
            if (session.getType() == WorkSession.SessionType.BREAK) {
                lbl.setText(bundle.getString("dashboard.staff.status.break")); lbl.setStyle(lbl.getStyle() + "; -fx-background-color: #f59e0b;");
            } else if (scheduledRange != null) {
                if (now.isAfter(scheduledRange.getOpen()) && now.isBefore(scheduledRange.getClose())) {
                    lbl.setText(bundle.getString("dashboard.staff.status.shift")); lbl.setStyle(lbl.getStyle() + "; -fx-background-color: #10b981;");
                } else {
                    lbl.setText(bundle.getString("dashboard.staff.status.extras")); lbl.setStyle(lbl.getStyle() + "; -fx-background-color: #8b5cf6;");
                }
            } else {
                lbl.setText(bundle.getString("dashboard.staff.status.out_of_prog")); lbl.setStyle(lbl.getStyle() + "; -fx-background-color: #64748b;");
            }
            this.statusNode = lbl;
        }

        public String getName() { return name; }
        public String getRole() { return role; }
        public String getStart() { return start; }
        public String getShift() { return shift; }
        public String getDuration() { return duration; }
        public Node getProgressNode() { return progressNode; }
        public Node getStatusNode() { return statusNode; }
    }
}

