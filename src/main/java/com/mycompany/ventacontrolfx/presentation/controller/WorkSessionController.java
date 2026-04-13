package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.application.usecase.WorkSessionUseCase;
import com.mycompany.ventacontrolfx.domain.model.WorkSession;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.UserSession;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class WorkSessionController implements Injectable {

    @FXML
    private StackPane statusIconContainer;
    @FXML
    private FontAwesomeIconView statusIcon;
    @FXML
    private Label lblCurrentStatus;
    @FXML
    private Label lblSessionTimer;
    @FXML
    private Label lblShiftStart;
    @FXML
    private Label lblTotalWorked;
    @FXML
    private Label lblTotalBreak;

    @FXML
    private Button btnStartShift;
    @FXML
    private Button btnStartBreak;
    @FXML
    private Button btnEndSession;

    @FXML
    private TableView<WorkSessionViewModel> historyTable;
    @FXML
    private TableColumn<WorkSessionViewModel, String> colType;
    @FXML
    private TableColumn<WorkSessionViewModel, String> colStart;
    @FXML
    private TableColumn<WorkSessionViewModel, String> colEnd;
    @FXML
    private TableColumn<WorkSessionViewModel, String> colDuration;
    @FXML
    private TableColumn<WorkSessionViewModel, String> colStatus;

    private WorkSessionUseCase useCase;
    private UserSession userSession;
    private Timeline timer;
    private WorkSession activeSession;

    private final ObservableList<WorkSessionViewModel> historyList = FXCollections.observableArrayList();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public void inject(ServiceContainer container) {
        this.useCase = container.getWorkSessionUseCase();
        this.userSession = container.getUserSession();

        setupTable();
        refreshUI();
        startGlobalTimer();
    }

    private void setupTable() {
        colType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colStart.setCellValueFactory(new PropertyValueFactory<>("start"));
        colEnd.setCellValueFactory(new PropertyValueFactory<>("end"));
        colDuration.setCellValueFactory(new PropertyValueFactory<>("duration"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        historyTable.setItems(historyList);
    }

    private void refreshUI() {
        if (userSession.getCurrentUser() == null)
            return;

        Optional<WorkSession> sessionOpt = useCase.getActiveSession(userSession.getCurrentUser().getUserId());
        if (sessionOpt.isPresent()) {
            this.activeSession = sessionOpt.get();
            updateActiveStatusUI();
        } else {
            this.activeSession = null;
            updateInactiveStatusUI();
        }
        loadHistory();
    }

    private void updateActiveStatusUI() {
        boolean isShift = activeSession.getType() == WorkSession.SessionType.SHIFT;

        statusIconContainer.getStyleClass().removeAll("status-active", "status-break", "status-inactive");
        if (isShift) {
            lblCurrentStatus.setText("EN TURNO");
            statusIconContainer.getStyleClass().add("status-active");
            statusIcon.setGlyphName("USER");
            btnStartShift.setDisable(true);
            btnStartBreak.setDisable(false);
            btnEndSession.setText("FINALIZAR TURNO");
        } else {
            lblCurrentStatus.setText("EN DESCANSO");
            statusIconContainer.getStyleClass().add("status-break");
            statusIcon.setGlyphName("COFFEE");
            btnStartShift.setDisable(false); // Permite "Volver al turno"
            btnStartShift.setText("VOLVER AL TURNO");
            btnStartBreak.setDisable(true);
            btnEndSession.setText("FINALIZAR TODO");
        }

        btnEndSession.setDisable(false);
    }

    private void updateInactiveStatusUI() {
        lblCurrentStatus.setText("FUERA DE TURNO");
        lblSessionTimer.setText("00:00:00");
        statusIconContainer.getStyleClass().removeAll("status-active", "status-break", "status-inactive");
        statusIconContainer.getStyleClass().add("status-inactive");
        statusIcon.setGlyphName("USER_TIMES");

        btnStartShift.setDisable(false);
        btnStartShift.setText("INICIAR TURNO");
        btnStartBreak.setDisable(true);
        btnEndSession.setDisable(true);
        btnEndSession.setText("FINALIZAR");
    }

    private void startGlobalTimer() {
        if (timer != null)
            timer.stop();

        timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> updateTimerLabel()));
        timer.setCycleCount(Timeline.INDEFINITE);
        timer.play();
    }

    private void updateTimerLabel() {
        if (activeSession != null) {
            // Update main cumulative timer (User expects work time to continue after break)
            long totalSeconds = calculateCumulativeTimedSessionToday(activeSession.getType());
            lblSessionTimer.setText(formatLongDuration(totalSeconds));

            // Periodically refresh the whole history/summary to keep it in sync
            loadHistory();
        } else {
            lblSessionTimer.setText("00:00:00");
        }
    }

    private long calculateCumulativeTimedSessionToday(WorkSession.SessionType type) {
        if (userSession.getCurrentUser() == null)
            return 0;

        LocalDate today = LocalDate.now();
        List<WorkSession> history = useCase.getHistory(userSession.getCurrentUser().getUserId());

        long completedSeconds = history.stream()
                .filter(s -> s.getStartTime().toLocalDate().isEqual(today))
                .filter(s -> s.getType() == type)
                .filter(s -> s.getStatus() == WorkSession.SessionStatus.COMPLETED)
                .mapToLong(s -> {
                    if (s.getEndTime() != null) {
                        return ChronoUnit.SECONDS.between(s.getStartTime(), s.getEndTime());
                    }
                    return 0;
                })
                .sum();

        long activeSeconds = 0;
        if (activeSession != null && activeSession.getType() == type) {
            activeSeconds = java.time.temporal.ChronoUnit.SECONDS.between(activeSession.getStartTime(),
                    LocalDateTime.now());
        }

        return completedSeconds + activeSeconds;
    }

    @FXML
    private void handleStartShift() {
        try {
            Integer userId = userSession.getCurrentUser().getUserId();
            if (activeSession != null && activeSession.getType() == WorkSession.SessionType.BREAK) {
                useCase.endSession(userId);
            }
            useCase.startShift(userId);
            refreshUI();
        } catch (Exception e) {
            showAlert("Error", e.getMessage());
        }
    }

    @FXML
    private void handleStartBreak() {
        try {
            useCase.startBreak(userSession.getCurrentUser().getUserId());
            refreshUI();
        } catch (Exception e) {
            showAlert("Error", e.getMessage());
        }
    }

    @FXML
    private void handleEndSession() {
        try {
            useCase.endSession(userSession.getCurrentUser().getUserId());
            refreshUI();
        } catch (Exception e) {
            showAlert("Error", e.getMessage());
        }
    }

    @FXML
    private void loadHistory() {
        if (userSession.getCurrentUser() == null)
            return;

        List<WorkSession> history = useCase.getHistory(userSession.getCurrentUser().getUserId());
        historyList.setAll(history.stream()
                .map(s -> new WorkSessionViewModel(s, dateTimeFormatter))
                .collect(Collectors.toList()));

        updateDailySummaries(history);
    }

    private void updateDailySummaries(List<WorkSession> history) {
        LocalDate today = LocalDate.now();

        List<WorkSession> todaySessions = history.stream()
                .filter(s -> s.getStartTime().toLocalDate().isEqual(today))
                .collect(Collectors.toList());

        Optional<WorkSession> firstShift = todaySessions.stream()
                .filter(s -> s.getType() == WorkSession.SessionType.SHIFT)
                .min((a, b) -> a.getStartTime().compareTo(b.getStartTime()));

        lblShiftStart.setText(firstShift.map(s -> s.getStartTime().format(timeFormatter)).orElse("--"));

        long totalWorkedSeconds = calculateTotalSeconds(todaySessions, WorkSession.SessionType.SHIFT);
        long totalBreakSeconds = calculateTotalSeconds(todaySessions, WorkSession.SessionType.BREAK);

        lblTotalWorked.setText(formatShortDuration(totalWorkedSeconds));
        lblTotalBreak.setText(formatShortDuration(totalBreakSeconds));
    }

    private long calculateTotalSeconds(List<WorkSession> sessions, WorkSession.SessionType type) {
        return sessions.stream()
                .filter(s -> s.getType() == type)
                .mapToLong(s -> {
                    LocalDateTime end = s.getEndTime() != null ? s.getEndTime() : LocalDateTime.now();
                    return ChronoUnit.SECONDS.between(s.getStartTime(), end);
                }).sum();
    }

    private String formatShortDuration(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        return String.format("%dh %dm", h, m);
    }

    private String formatLongDuration(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static class WorkSessionViewModel {
        private String type;
        private String start;
        private String end;
        private String duration;
        private String status;

        public WorkSessionViewModel(WorkSession session, DateTimeFormatter formatter) {
            this.type = session.getType() == WorkSession.SessionType.SHIFT ? "TURNO" : "DESCANSO";
            this.start = session.getStartTime().format(formatter);
            this.end = session.getEndTime() != null ? session.getEndTime().format(formatter) : "-";

            LocalDateTime endDt = session.getEndTime() != null ? session.getEndTime() : LocalDateTime.now();
            long seconds = ChronoUnit.SECONDS.between(session.getStartTime(), endDt);
            long h = seconds / 3600;
            long m = (seconds % 3600) / 60;
            this.duration = String.format("%dh %dm", h, m);

            this.status = session.getStatus().name();
        }

        public String getType() {
            return type;
        }

        public String getStart() {
            return start;
        }

        public String getEnd() {
            return end;
        }

        public String getDuration() {
            return duration;
        }

        public String getStatus() {
            return status;
        }
    }
}
