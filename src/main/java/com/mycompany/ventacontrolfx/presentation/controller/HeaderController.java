package com.mycompany.ventacontrolfx.presentation.controller;

import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.infrastructure.navigation.NavigationService;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.util.ModalService;
import com.mycompany.ventacontrolfx.util.SceneNavigator;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class HeaderController implements Injectable {

    @FXML
    private TextField searchField;
    @FXML
    private MenuButton userMenuButton;
    @FXML
    private HBox searchBarContainer, btnStockAlerts;
    @FXML
    private Label lblHeaderUsername, lblStockAlertCount;
    @FXML
    private MenuItem menuItemCashClosure;

    private ServiceContainer container;
    private NavigationService navigationService;
    private com.mycompany.ventacontrolfx.shared.bus.GlobalEventBus.DataChangeListener stockRefreshListener;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.navigationService = container.getNavigationService();
        setupUserMenu();
        setupSearch();
        checkStockAlerts();

        // Suscribirse a cambios globales para refrescar alertas (ej: después de una
        // venta)
        if (container.getEventBus() != null) {
            this.stockRefreshListener = this::refreshStockAlerts;
            container.getEventBus().subscribe(this.stockRefreshListener);
        }
    }

    public void refreshStockAlerts() {
        checkStockAlerts();
    }

    private void checkStockAlerts() {
        if (container == null || container.getDashboardUseCase() == null)
            return;

        com.mycompany.ventacontrolfx.shared.async.AsyncManager.execute(
                container.getDashboardUseCase().getLowStockProductsTask(),
                (java.util.List<com.mycompany.ventacontrolfx.domain.model.Product> lowStock) -> {
                    if (lowStock != null && !lowStock.isEmpty()) {
                        btnStockAlerts.setVisible(true);
                        btnStockAlerts.setManaged(true);
                        lblStockAlertCount.setText(String.valueOf(lowStock.size()));
                    } else {
                        btnStockAlerts.setVisible(false);
                        btnStockAlerts.setManaged(false);
                    }
                });
    }

    private void setupUserMenu() {
        // Mostrar nombre del usuario en sesión
        if (lblHeaderUsername != null && container.getUserSession() != null) {
            var currentUser = container.getUserSession().getCurrentUser();
            if (currentUser != null) {
                String name = currentUser.getFullName() != null && !currentUser.getFullName().isBlank()
                        ? currentUser.getFullName()
                        : currentUser.getUsername();
                lblHeaderUsername.setText(name != null ? name : "Usuario");
            }
        }
        if (userMenuButton != null) {
            userMenuButton.setOnMouseEntered(e -> {
                if (!userMenuButton.isShowing())
                    userMenuButton.show();
            });
        }

        // Restringir Cierre de Caja por permisos
        if (menuItemCashClosure != null && container.getUserSession() != null) {
            boolean canClose = container.getUserSession().hasPermission("caja.cerrar")
                    || container.getUserSession().hasPermission("CIERRES");
            menuItemCashClosure.setVisible(canClose);
        }
    }

    private void setupSearch() {
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (navigationService != null) {
                    navigationService.search(newVal);
                }
            });
        }

        // Evitar que el campo de búsqueda robe el foco al arrancar, pero asegurar que
        // el foco esté en el campo de texto real
        Platform.runLater(() -> {
            if (searchField != null) {
                searchField.requestFocus();
            }
        });
    }

    public void setSearchBarVisible(boolean visible) {
        if (searchBarContainer != null) {
            searchBarContainer.setVisible(visible);
            searchBarContainer.setManaged(visible);
        }
    }

    @FXML
    private void handleCashClosure() {
        ModalService.showFullScreenModal("/view/cash_closure.fxml", "Cierre de Caja", container, null);
    }

    @FXML
    private void handleLogout() {
        if (AlertUtil.showConfirmation("Cerrar Sesión", "¿Estás seguro?", "Se cerrará la sesión actual.")) {
            container.getUserSession().logout();
            Stage stage = (Stage) userMenuButton.getScene().getWindow();
            SceneNavigator.loadScene(stage, "/view/login.fxml", "Login", 900, 600, false, container);
        }
    }

    @FXML
    private void handleShowStockAlerts() {
        ModalService.showStandardModal("/view/low_stock_dialog.fxml", "Alertas de Stock Bajo", container, null);
    }
}
