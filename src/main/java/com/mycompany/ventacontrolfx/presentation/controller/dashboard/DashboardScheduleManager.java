package com.mycompany.ventacontrolfx.presentation.controller.dashboard;

import com.mycompany.ventacontrolfx.domain.model.BusinessDay;
import com.mycompany.ventacontrolfx.domain.model.User;
import com.mycompany.ventacontrolfx.domain.model.WorkSession;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.shared.util.SpanishHolidays;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gestor de horarios, cumplimiento y alertas operativas del Dashboard.
 */
public class DashboardScheduleManager {

    private final ServiceContainer container;
    private final VBox scheduleContainer;
    private final VBox alertsContainer;
    private final Label lblStatus;
    private final Label lblCompliance;

    public DashboardScheduleManager(ServiceContainer container, VBox scheduleContainer, VBox alertsContainer, Label lblStatus, Label lblCompliance) {
        this.container = container;
        this.scheduleContainer = scheduleContainer;
        this.alertsContainer = alertsContainer;
        this.lblStatus = lblStatus;
        this.lblCompliance = lblCompliance;
    }

    public void update(BusinessDay schedule, LocalDateTime actualStart, List<User> allUsers, List<WorkSession> activeSessions) {
        Platform.runLater(() -> {
            scheduleContainer.getChildren().clear();
            alertsContainer.getChildren().clear();

            if (schedule == null || schedule.isClosed() || schedule.getShifts().isEmpty()) {
                lblStatus.setText(container.getBundle().getString("dashboard.status." + (schedule != null && schedule.isClosed() ? "closed" : "no_schedule")));
                lblStatus.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569;");
                lblCompliance.setText(container.getBundle().getString("NA"));
                scheduleContainer.getChildren().add(new Label(container.getBundle().getString("dashboard.shifts.none")));
                return;
            }

            LocalTime now = LocalTime.now();
            boolean isOpen = schedule.getShifts().stream().anyMatch(s -> now.isAfter(s.getOpen()) && now.isBefore(s.getClose()));
            lblStatus.setText(container.getBundle().getString("dashboard.status." + (isOpen ? "open" : "out_of_hours")));
            lblStatus.setStyle(isOpen ? "-fx-background-color: #dcfce7; -fx-text-fill: #16a34a;" : "-fx-background-color: #fef3c7; -fx-text-fill: #d97706;");

            checkCompliance(schedule, actualStart);
            renderShifts(schedule, allUsers, activeSessions, now);
            checkHolidays();

            if (alertsContainer.getChildren().isEmpty()) addAlert(container.getBundle().getString("dashboard.alert.stable"), "info");
        });
    }

    private void checkCompliance(BusinessDay schedule, LocalDateTime actualStart) {
        if (actualStart != null && !schedule.getShifts().isEmpty()) {
            LocalTime expected = schedule.getShifts().get(0).getOpen();
            if (actualStart.toLocalTime().isAfter(expected.plusMinutes(10))) {
                lblCompliance.setText(container.getBundle().getString("dashboard.compliance.delay"));
                lblCompliance.setStyle("-fx-text-fill: #ef4444;");
                addAlert(String.format(container.getBundle().getString("dashboard.alert.late_opening"), actualStart.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))), "warning");
            } else {
                lblCompliance.setText(container.getBundle().getString("dashboard.compliance.optimal"));
                lblCompliance.setStyle("-fx-text-fill: #16a34a;");
            }
        } else {
            lblCompliance.setText(container.getBundle().getString("dashboard.compliance.pending"));
            lblCompliance.setStyle("-fx-text-fill: #94a3b8;");
        }
    }

    private void renderShifts(BusinessDay schedule, List<User> allUsers, List<WorkSession> activeSessions, LocalTime now) {
        for (BusinessDay.TimeRange r : schedule.getShifts()) {
            String staff = r.getAssignedUserIds().stream()
                    .map(id -> allUsers.stream().filter(u -> u.getUserId() == id).map(User::getFullName).findFirst()
                    .orElse(String.format(container.getBundle().getString("dashboard.staff.user"), id)))
                    .collect(Collectors.joining(", "));

            VBox box = new VBox(5); box.setPadding(new Insets(10));
            box.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8; -fx-border-color: #e2e8f0;");
            
            HBox timeRow = new HBox(10, new FontAwesomeIconView(FontAwesomeIcon.CLOCK_ALT), new Label(r.getOpen() + " - " + r.getClose()));
            timeRow.setAlignment(Pos.CENTER_LEFT);

            Label lblStaff = new Label(staff.isEmpty() ? container.getBundle().getString("dashboard.alert.no_staff_assigned") : staff);
            lblStaff.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-style: italic;");
            lblStaff.setWrapText(true);

            box.getChildren().addAll(timeRow, lblStaff);
            scheduleContainer.getChildren().add(box);

            if (now.isAfter(r.getOpen()) && now.isBefore(r.getClose())) {
                for (Integer id : r.getAssignedUserIds()) {
                    if (activeSessions.stream().noneMatch(s -> s.getUserId() == id)) {
                        String name = allUsers.stream().filter(u -> u.getUserId() == id).map(User::getFullName).findFirst()
                                .orElse(String.format(container.getBundle().getString("dashboard.staff.user"), id));
                        addAlert(String.format(container.getBundle().getString("dashboard.alert.absence_detected"), name), "warning");
                    }
                }
            }
        }
    }

    private void checkHolidays() {
        if (SpanishHolidays.isHoliday(LocalDate.now())) {
            addAlert(String.format(container.getBundle().getString("dashboard.alert.holiday"), container.getBundle().getString(SpanishHolidays.getHolidayName(LocalDate.now()))), "info");
        }
    }

    private void addAlert(String msg, String type) {
        HBox alert = new HBox(10, new FontAwesomeIconView(type.equals("warning") ? FontAwesomeIcon.EXCLAMATION_TRIANGLE : FontAwesomeIcon.INFO_CIRCLE), new Label(msg));
        alert.getStyleClass().addAll("alert-row", "alert-" + type);
        alert.setAlignment(Pos.CENTER_LEFT);
        alertsContainer.getChildren().add(alert);
    }
}


