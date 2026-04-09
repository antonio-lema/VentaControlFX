package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.domain.model.BusinessDay;
import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
import com.mycompany.ventacontrolfx.domain.model.User;
import com.mycompany.ventacontrolfx.domain.model.WorkSession;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PunctualityAuditController implements Injectable {

    @FXML
    private TableView<AuditRecord> tableAudit;
    @FXML
    private TableColumn<AuditRecord, String> colUser, colShift, colRealStart, colDelay, colStatus, colEndPunctuality;
    @FXML
    private DatePicker datePicker;
    @FXML
    private Label lblGeneralPunctuality, lblLateCount, lblNoShowCount;

    private ServiceContainer container;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        setupTable();
        datePicker.setValue(LocalDate.now());
        refresh();
    }

    private void setupTable() {
        colUser.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().user));
        colShift.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().shiftRange));
        colRealStart.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().realStart));
        colDelay.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().delay));
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().status));
        colEndPunctuality.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().closingStatus));
    }

    @FXML
    public void handleDateChange() {
        refresh();
    }

    @FXML
    public void refresh() {
        LocalDate date = datePicker.getValue();
        if (date == null)
            return;

        SaleConfig config = container.getConfigUseCase().getConfig();
        int dayIdx = date.getDayOfWeek().getValue();
        BusinessDay businessDay = config.getSchedule().stream()
                .filter(d -> d.getDayOfWeek() == dayIdx)
                .findFirst().orElse(null);

        if (businessDay == null || businessDay.isClosed()) {
            tableAudit.setItems(FXCollections.emptyObservableList());
            lblGeneralPunctuality.setText("--");
            lblLateCount.setText("0");
            lblNoShowCount.setText("0");
            return;
        }

        List<WorkSession> sessions = container.getWorkSessionUseCase().getHistoryByDate(date);
        Map<Integer, List<WorkSession>> userSessions = sessions.stream()
                .collect(Collectors.groupingBy(WorkSession::getUserId));

        List<AuditRecord> records = new java.util.ArrayList<>();
        int totalLates = 0;
        int totalNoShows = 0;

        for (BusinessDay.TimeRange range : businessDay.getShifts()) {
            for (Integer userId : range.getAssignedUserIds()) {
                AuditRecord record = new AuditRecord();
                try {
                    User u = container.getUserUseCase().getUserById(userId);
                    record.user = (u != null) ? u.getFullName() : "User #" + userId;
                } catch (Exception e) {
                    record.user = "Error #" + userId;
                }

                record.shiftRange = range.getOpen().format(timeFormatter) + " - "
                        + range.getClose().format(timeFormatter);

                List<WorkSession> mySessions = userSessions.getOrDefault(userId, new java.util.ArrayList<>());
                WorkSession shiftSession = mySessions.stream()
                        .filter(s -> s.getType() == WorkSession.SessionType.SHIFT)
                        .findFirst().orElse(null);

                if (shiftSession == null) {
                    record.realStart = "N/A";
                    record.delay = "N/A";
                    record.status = "FALTA";
                    totalNoShows++;
                } else {
                    LocalTime realStart = shiftSession.getStartTime().toLocalTime();
                    record.realStart = realStart.format(timeFormatter);

                    long delayMinutes = java.time.Duration.between(range.getOpen(), realStart).toMinutes();
                    if (delayMinutes > 5) {
                        record.delay = delayMinutes + " min";
                        record.status = "RETRASO";
                        totalLates++;
                    } else {
                        record.delay = "Puntual";
                        record.status = "OK";
                    }

                    // Closing check
                    if (shiftSession.getEndTime() != null) {
                        LocalTime realEnd = shiftSession.getEndTime().toLocalTime();
                        long endDiff = java.time.Duration.between(range.getClose(), realEnd).toMinutes();
                        if (endDiff > 10)
                            record.closingStatus = "Tarde (" + endDiff + "m)";
                        else if (endDiff < -10)
                            record.closingStatus = "Pronto (" + Math.abs(endDiff) + "m)";
                        else
                            record.closingStatus = "Correcto";
                    } else {
                        record.closingStatus = "Sesi\u00f3n activa";
                    }
                }
                records.add(record);
            }
        }

        tableAudit.setItems(FXCollections.observableArrayList(records));
        lblLateCount.setText(String.valueOf(totalLates));
        lblNoShowCount.setText(String.valueOf(totalNoShows));

        if (records.isEmpty())
            lblGeneralPunctuality.setText("N/A");
        else {
            double punctuality = ((double) (records.size() - totalLates - totalNoShows) / records.size()) * 100;
            lblGeneralPunctuality.setText(String.format("%.0f%%", punctuality));
        }
    }

    public static class AuditRecord {
        public String user;
        public String shiftRange;
        public String realStart;
        public String delay;
        public String status;
        public String closingStatus;
    }
}
