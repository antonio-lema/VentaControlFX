package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.infrastructure.navigation.NavigationService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import com.mycompany.ventacontrolfx.domain.model.User;
import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
import com.mycompany.ventacontrolfx.domain.model.BusinessDay;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.application.Platform;
import javafx.stage.Stage;

public class MainController implements Injectable, com.mycompany.ventacontrolfx.shared.bus.GlobalEventBus.LocaleChangeListener {

    @FXML
    private StackPane headerContainer, sidebarContainer, cartContainer;
    @FXML
    private ScrollPane mainContent;
    @FXML
    private VBox loadingOverlay;

    private ServiceContainer container;
    private NavigationService navigationService;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.navigationService = new NavigationService(mainContent, loadingOverlay, container);
        container.setNavigationService(navigationService);

        navigationService.setCartVisibilityListener(shouldShow -> {
            if (cartContainer != null) {
                cartContainer.setVisible(shouldShow);
                cartContainer.setManaged(shouldShow);
            }
        });

        loadStaticComponents();
        container.getEventBus().subscribeLocale(this);
        showInitialView();
        startShiftMonitor();
    }

    private LocalDateTime snoozeUntil = null;
    private boolean closureDialogVisible = false;
    private static final int LATE_CLOCK_IN_GRACE_MINUTES = 15;
    private static final int SALES_BLOCK_GRACE_MINUTES = 60;

    private final Set<Integer> notifiedLateUserIds = new HashSet<>();
    private LocalDate lastLateCheckDate = LocalDate.now();

    private void startShiftMonitor() {
        // First check
        Platform.runLater(() -> {
            checkShiftEnd();
            checkLateClockIns();
            checkSalesBlock();
        });

        Timeline timeline = new Timeline(new KeyFrame(Duration.minutes(1), event -> {
            checkShiftEnd();
            checkLateClockIns();
            checkSalesBlock();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void checkShiftEnd() {
        if (closureDialogVisible)
            return;
        if (snoozeUntil != null && LocalDateTime.now().isBefore(snoozeUntil))
            return;

        User user = container.getUserSession().getCurrentUser();
        if (user == null || !user.hasPermission("CIERRES"))
            return;

        // Check if cash is actually open OR has pending transactions from a previous
        // shift
        try {
            boolean hasActiveFund = container.getClosureUseCase().hasActiveFund();
            boolean hasPendingSales = container.getClosureUseCase().getTodayTransactionCount() > 0;

            if (!hasActiveFund && !hasPendingSales) {
                return;
            }
        } catch (Exception e) {
            return;
        }

        SaleConfig cfg = container.getConfigUseCase().getConfig();
        if (cfg == null)
            return;

        int todayIdx = LocalDate.now().getDayOfWeek().getValue();
        BusinessDay today = cfg.getSchedule().stream()
                .filter(d -> d.getDayOfWeek() == todayIdx)
                .findFirst().orElse(null);

        if (today == null || today.isClosed())
            return;

        LocalTime now = LocalTime.now();
        boolean shiftEnded = false;

        // 1. Check if user has explicit shifts and the latest has ended
        Optional<LocalTime> myLatestEnd = today.getShifts().stream()
                .filter(r -> r.getAssignedUserIds().contains(user.getUserId()))
                .map(BusinessDay.TimeRange::getClose)
                .max(LocalTime::compareTo);

        if (myLatestEnd.isPresent()) {
            if (now.isAfter(myLatestEnd.get())) {
                shiftEnded = true;
            }
        }
        // 2. Fallback for Administrators: if not assigned to specific shift, show if
        // LAST shift of the day has passed
        else if (user.getRole().equalsIgnoreCase("Administrador") || user.getRole().equalsIgnoreCase("admin")) {
            Optional<LocalTime> absoluteLatest = today.getShifts().stream()
                    .map(BusinessDay.TimeRange::getClose)
                    .max(LocalTime::compareTo);

            if (absoluteLatest.isPresent() && now.isAfter(absoluteLatest.get())) {
                shiftEnded = true;
            }
        }

        if (shiftEnded) {
            showClosureReminder();
        }
    }

    private void checkLateClockIns() {
        if (!LocalDate.now().equals(lastLateCheckDate)) {
            notifiedLateUserIds.clear();
            lastLateCheckDate = LocalDate.now();
        }

        User currentUser = container.getUserSession().getCurrentUser();
        if (currentUser == null || (!currentUser.getRole().equalsIgnoreCase("Administrador")
                && !currentUser.getRole().equalsIgnoreCase("Admin")))
            return;

        SaleConfig cfg = container.getConfigUseCase().getConfig();
        if (cfg == null)
            return;

        int todayIdx = LocalDate.now().getDayOfWeek().getValue();
        BusinessDay today = cfg.getSchedule().stream()
                .filter(d -> d.getDayOfWeek() == todayIdx)
                .findFirst().orElse(null);

        if (today == null || today.isClosed())
            return;

        LocalTime now = LocalTime.now();
        List<String> lateUsers = new java.util.ArrayList<>();
        Set<Integer> newlyNotified = new java.util.HashSet<>();
        List<com.mycompany.ventacontrolfx.domain.model.WorkSession> activeSessions = container.getWorkSessionUseCase()
                .getAllActiveSessions();

        for (BusinessDay.TimeRange shift : today.getShifts()) {
            // If shift started more than X mins ago
            if (now.isAfter(shift.getOpen().plusMinutes(LATE_CLOCK_IN_GRACE_MINUTES))
                    && now.isBefore(shift.getClose())) {
                for (Integer userId : shift.getAssignedUserIds()) {
                    boolean isClockedIn = activeSessions.stream()
                            .anyMatch(s -> s.getUserId() == userId
                                    && s.getType() == com.mycompany.ventacontrolfx.domain.model.WorkSession.SessionType.SHIFT);
                    if (!isClockedIn && !notifiedLateUserIds.contains(userId)) {
                        try {
                            User u = container.getUserUseCase().getUserById(userId);
                            if (u != null) {
                                lateUsers.add(u.getFullName());
                                newlyNotified.add(userId);
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }

        if (!lateUsers.isEmpty()) {
            notifiedLateUserIds.addAll(newlyNotified);
            final String message = container.getBundle().getString("main.attendance.late_warning") + "\n"
                    + String.join(", ", lateUsers);
            Platform.runLater(() -> {
                // We use AlertUtil for a quick toast/warning to the Admin
                com.mycompany.ventacontrolfx.util.AlertUtil.showWarning(
                        container.getBundle().getString("main.attendance.title"), message);
            });
        }
    }

    private void checkSalesBlock() {
        User user = container.getUserSession().getCurrentUser();
        if (user == null || user.getRole().equalsIgnoreCase("Administrador")) {
            unlockCart();
            return;
        }

        // If cash is not open, we might want to block too, but let's stick to the shift
        // end logic
        try {
            if (!container.getClosureUseCase().hasActiveFund()) {
                unlockCart();
                return;
            }
        } catch (Exception e) {
            unlockCart();
            return;
        }

        SaleConfig cfg = container.getConfigUseCase().getConfig();
        if (cfg == null) {
            unlockCart();
            return;
        }

        // Check if user is currently clocked in for a SHIFT
        boolean isClockedIn = container.getWorkSessionUseCase().getAllActiveSessions().stream()
                .anyMatch(s -> s.getUserId() == user.getUserId()
                        && s.getType() == com.mycompany.ventacontrolfx.domain.model.WorkSession.SessionType.SHIFT);

        if (isClockedIn) {
            unlockCart();
            return;
        }

        int todayIdx = LocalDate.now().getDayOfWeek().getValue();
        BusinessDay today = cfg.getSchedule().stream()
                .filter(d -> d.getDayOfWeek() == todayIdx)
                .findFirst().orElse(null);

        if (today == null || today.isClosed()) {
            unlockCart();
            return;
        }

        LocalTime now = LocalTime.now();
        Optional<LocalTime> latestEnd = today.getShifts().stream()
                .filter(r -> r.getAssignedUserIds().contains(user.getUserId()))
                .map(BusinessDay.TimeRange::getClose)
                .max(LocalTime::compareTo);

        if (latestEnd.isPresent() && now.isAfter(latestEnd.get().plusMinutes(SALES_BLOCK_GRACE_MINUTES))) {
            Platform.runLater(() -> {
                if (container.getCartUseCase() != null) {
                    container.getCartUseCase().setLocked(true);
                }
            });
        } else {
            unlockCart();
        }
    }

    private void unlockCart() {
        Platform.runLater(() -> {
            if (container.getCartUseCase() != null) {
                container.getCartUseCase().setLocked(false);
            }
        });
    }

    private void showClosureReminder() {
        closureDialogVisible = true;
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/dialog/closure_reminder.fxml"));
                Parent root = loader.load();
                ClosureReminderController controller = loader.getController();

                Stage stage = new Stage();
                stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                stage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
                stage.setTitle(container.getBundle().getString("main.shift_end.title"));

                Scene scene = new Scene(root);
                scene.setFill(null);
                container.getThemeManager().applyFullTheme(scene);
                stage.setScene(scene);

                controller.setStage(stage);
                controller.setOnAction(action -> {
                    switch (action) {
                        case "CLOSE_NOW":
                            navigationService.navigateTo("/view/cash_closure.fxml");
                            break;
                        case "SNOOZE_5":
                            snoozeUntil = LocalDateTime.now().plusMinutes(5);
                            break;
                        case "SNOOZE_15":
                            snoozeUntil = LocalDateTime.now().plusMinutes(15);
                            break;
                        case "SNOOZE_60":
                            snoozeUntil = LocalDateTime.now().plusHours(1);
                            break;
                    }
                });

                stage.showAndWait();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closureDialogVisible = false;
            }
        });
    }

    // HeaderController reference para controlar visibilidad del search bar
    private HeaderController headerController;

    private void loadStaticComponents() {
        headerController = loadComponentWithController("/view/main_header.fxml", headerContainer);
        loadComponent("/view/sidebar.fxml", sidebarContainer);
        loadComponent("/view/cart_panel.fxml", cartContainer);

        // Conectar la visibilidad de la barra de bÃºsqueda con el header
        if (headerController != null) {
            navigationService.setSearchBarVisibilityListener(shouldShow -> {
                headerController.setSearchBarVisible(shouldShow);
            });
        }
    }

    private void showInitialView() {
        navigationService.navigateTo("/view/sell_view.fxml");
    }

    private void loadComponent(String fxml, StackPane target) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml), container.getBundle());
            Node node = loader.load();
            Object controller = loader.getController();
            if (controller instanceof Injectable) {
                ((Injectable) controller).inject(container);
            }
            target.getChildren().setAll(node);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T loadComponentWithController(String fxml, StackPane target) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml), container.getBundle());
            Node node = loader.load();
            Object controller = loader.getController();
            if (controller instanceof Injectable) {
                ((Injectable) controller).inject(container);
            }
            target.getChildren().setAll(node);
            return (T) controller;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onLocaleChanged() {
        Platform.runLater(() -> {
            loadStaticComponents();
            showInitialView(); // This will refresh the center view too
        });
    }
}
