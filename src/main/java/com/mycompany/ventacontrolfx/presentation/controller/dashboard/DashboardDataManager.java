package com.mycompany.ventacontrolfx.presentation.controller.dashboard;

import com.mycompany.ventacontrolfx.domain.model.BusinessDay;
import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
import com.mycompany.ventacontrolfx.domain.model.WorkSession;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.application.Platform;
import javafx.scene.control.Label;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

/**
 * Gestor de datos y temporizadores para el Dashboard.
 * Maneja la carga asíncrona y el refresco automático.
 */
public class DashboardDataManager {

    private final ServiceContainer container;
    private Timer refreshTimer;

    public DashboardDataManager(ServiceContainer container) {
        this.container = container;
    }

    public void startAutoRefresh(Label lblTime, Runnable onDataRefresh) {
        refreshTimer = new Timer(true);
        // Reloj (1s)
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() {
                String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
                Platform.runLater(() -> lblTime.setText(time));
            }
        }, 0, 1000);

        // Datos (30s)
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override public void run() { onDataRefresh.run(); }
        }, 1000, 30000);
    }

    public void stop() {
        if (refreshTimer != null) refreshTimer.cancel();
    }

    public void fetchData(Consumer<DashboardDataResult> onSuccess, Consumer<Throwable> onError) {
        container.getAsyncManager().runAsyncTask(() -> {
            try {
                SaleConfig cfg = container.getConfigUseCase().getConfig();
                int todayIdx = LocalDate.now().getDayOfWeek().getValue();
                BusinessDay schedule = (cfg != null) ? cfg.getSchedule().stream().filter(d -> d.getDayOfWeek() == todayIdx).findFirst().orElse(null) : null;
                
                List<WorkSession> allToday = container.getWorkSessionUseCase().getHistoryByDate(LocalDate.now());
                List<WorkSession> active = container.getWorkSessionUseCase().getAllActiveSessions();
                boolean isCashOpen = container.getClosureUseCase().hasActiveFund();
                double cashAmount = container.getClosureUseCase().getCurrentCashInDrawer();

                return new DashboardDataResult(schedule, allToday, active, isCashOpen, cashAmount);
            } catch (Exception e) { throw new RuntimeException(e); }
        }, onSuccess, onError);
    }

    public static record DashboardDataResult(
        BusinessDay schedule,
        List<WorkSession> allSessionsToday,
        List<WorkSession> activeSessions,
        boolean isCashOpen,
        double currentCash
    ) {}
}

