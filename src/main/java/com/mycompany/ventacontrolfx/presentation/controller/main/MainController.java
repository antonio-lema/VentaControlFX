package com.mycompany.ventacontrolfx.presentation.controller.main;

import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.infrastructure.navigation.NavigationService;
import com.mycompany.ventacontrolfx.presentation.controller.main.AppShellManager;
import com.mycompany.ventacontrolfx.presentation.controller.user.ShiftMonitorManager;
import com.mycompany.ventacontrolfx.presentation.controller.receipt.VerifactuIncidentHandler;

import com.mycompany.ventacontrolfx.shared.bus.GlobalEventBus;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import java.util.List;

/**
 * Orquestador principal de la aplicación (Main Shell).
 * Delega la lógica especializada a gestores (Managers).
 */
public class MainController implements Injectable, 
        GlobalEventBus.LocaleChangeListener, 
        GlobalEventBus.VerifactuIncidentListener {

    @FXML private StackPane headerContainer, sidebarContainer, cartContainer;
    @FXML private ScrollPane mainContent;
    @FXML private VBox loadingOverlay;

    private ServiceContainer container;
    private AppShellManager shellManager;
    private ShiftMonitorManager shiftMonitor;
    private VerifactuIncidentHandler verifactuHandler;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        
        // 1. Inicializar Navegación
        NavigationService navService = new NavigationService(mainContent, loadingOverlay, container);
        container.setNavigationService(navService);

        // 2. Inicializar Managers
        this.shellManager = new AppShellManager(container, navService);
        this.shiftMonitor = new ShiftMonitorManager(container);
        this.verifactuHandler = new VerifactuIncidentHandler(container);

        // 3. Construir Interfaz y Suscribir Eventos
        shellManager.build(headerContainer, sidebarContainer, cartContainer);
        
        container.getEventBus().subscribeLocale(this);
        container.getEventBus().subscribeVerifactu(this);
        
        shiftMonitor.start();
        checkInitialShiftStatus();

        // 4. Navegación Inicial (Solo al arrancar)
        navService.navigateTo("/view/cart/sell_view.fxml");
    }

    private void checkInitialShiftStatus() {
        if (container == null || container.getUserSession().getCurrentUser() == null) return;
        
        int userId = container.getUserSession().getCurrentUser().getUserId();
        container.getAsyncManager().runAsyncTask(() -> {
            return container.getWorkSessionUseCase().getActiveSession(userId).isPresent();
        }, (Boolean hasActiveSession) -> {
            if (!hasActiveSession) {
                Platform.runLater(() -> {
                    boolean startNow = com.mycompany.ventacontrolfx.presentation.util.AlertUtil.showConfirmation(
                        container.getBundle().getString("main.attendance.title"),
                        "Inicio de Jornada Detectado",
                        "No tienes una sesi\u00f3n de trabajo activa. \u00bfDeseas iniciar tu turno ahora?"
                    );
                    
                    if (startNow) {
                        com.mycompany.ventacontrolfx.presentation.navigation.ModalService.showTransparentModal(
                            "/view/user/shift_compact.fxml", 
                            "Gesti\u00f3n de Jornada", container, null);
                    }
                });
            }
        }, null);
    }

    @Override
    public void onVerifactuIncidentDetected(List<Integer> affectedSales, List<Integer> affectedReturns) {
        verifactuHandler.handleIncident(affectedSales, affectedReturns);
    }

    @Override
    public void onLocaleChanged() {
        Platform.runLater(() -> {
            shellManager.build(headerContainer, sidebarContainer, cartContainer);
            container.getNavigationService().reloadCurrent();
        });
    }
}

