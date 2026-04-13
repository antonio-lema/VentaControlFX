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
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.stage.Window;
import javafx.stage.PopupWindow;
import javafx.scene.input.MouseEvent;

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
    private PauseTransition menuClosePause;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.navigationService = container.getNavigationService();
        setupUserMenu();
        setupSearch();
        checkStockAlerts();

        // Suscribirse a cambios globales para refrescar alertas (ej: despu\u00e9s de
        // una
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
        // Mostrar nombre del usuario en sesi\u00f3n
        if (lblHeaderUsername != null && container.getUserSession() != null) {
            var currentUser = container.getUserSession().getCurrentUser();
            if (currentUser != null) {
                String name = currentUser.getFullName() != null && !currentUser.getFullName().isBlank()
                        ? currentUser.getFullName()
                        : currentUser.getUsername();
                lblHeaderUsername.setText(name != null ? name : container.getBundle().getString("main.session.user"));
            }
        }

        if (userMenuButton != null) {
            menuClosePause = new PauseTransition(Duration.millis(300));
            menuClosePause.setOnFinished(e -> userMenuButton.hide());

            userMenuButton.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    newScene.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
                        if (userMenuButton.getScene() == null)
                            return;

                        double mouseX = e.getScreenX();
                        double mouseY = e.getScreenY();

                        boolean overButton = false;
                        javafx.geometry.Bounds btnBounds = userMenuButton
                                .localToScreen(userMenuButton.getBoundsInLocal());
                        if (btnBounds != null && btnBounds.contains(mouseX, mouseY)) {
                            overButton = true;
                        }

                        boolean overPopup = false;
                        if (userMenuButton.isShowing()) {
                            for (Window w : Window.getWindows()) {
                                if (w instanceof PopupWindow && w.isShowing()) {
                                    if (w.getX() <= mouseX && mouseX <= w.getX() + w.getWidth() &&
                                            w.getY() <= mouseY && mouseY <= w.getY() + w.getHeight()) {
                                        overPopup = true;
                                    }
                                }
                            }
                        }

                        if (overButton || overPopup) {
                            menuClosePause.stop();
                            if (!userMenuButton.isShowing() && overButton) {
                                userMenuButton.show();
                            }
                        } else {
                            if (userMenuButton.isShowing()) {
                                if (menuClosePause.getStatus() != javafx.animation.Animation.Status.RUNNING) {
                                    menuClosePause.playFromStart();
                                }
                            }
                        }
                    });
                }
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
            // Debounce logic: wait 300ms after last key stroke before searching
            PauseTransition debounce = new PauseTransition(Duration.millis(300));
            debounce.setOnFinished(e -> {
                if (navigationService != null) {
                    navigationService.search(searchField.getText());
                }
            });

            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                debounce.playFromStart();
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
        ModalService.showFullScreenModal("/view/cash_closure.fxml",
                container.getBundle().getString("main.shift_end.title"), container, null);
    }

    @FXML
    private void handleLogout() {
        try {
            var user = container.getUserSession().getCurrentUser();
            if (user == null) {
                performLogout();
                return;
            }

            // 1. Confirmaci\u00f3n de Cierre de Sesi\u00f3n
            if (AlertUtil.showConfirmation(container.getBundle().getString("header.logout.title"),
                    container.getBundle().getString("header.logout.confirm"),
                    container.getBundle().getString("header.logout.msg"))) {

                /*
                 * Finalizar turno automáticamente si hay uno activo
                 * try {
                 * container.getWorkSessionUseCase().endSession(user.getUserId());
                 * } catch (Exception e) {
                 * System.err.println("Error finalizando turno al cerrar sesión: " +
                 * e.getMessage());
                 * }
                 */

                performLogout();
            }
        } catch (Exception e) {
            AlertUtil.showError(container.getBundle().getString("header.logout.error"),
                    container.getBundle().getString("cart.payment.error.unexpected") + ": " + e.getMessage());
        }
    }

    private void performLogout() {
        container.getUserSession().logout();
        Stage stage = (Stage) userMenuButton.getScene().getWindow();
        SceneNavigator.loadScene(stage, "/view/login.fxml", "Login", 900, 600, false, container);
    }

    @FXML
    private void handleShowStockAlerts() {
        ModalService.showStandardModal("/view/low_stock_dialog.fxml",
                container.getBundle().getString("header.stock_alerts.title"), container, null);
    }
}
