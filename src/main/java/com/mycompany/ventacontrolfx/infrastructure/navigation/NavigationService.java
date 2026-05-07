package com.mycompany.ventacontrolfx.infrastructure.navigation;

import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.util.Searchable;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import com.mycompany.ventacontrolfx.infrastructure.security.AuthorizationService;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import com.mycompany.ventacontrolfx.domain.repository.IAuditRepository;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Enterprise Navigation and Event Delegation Service.
 *
 * Notifica a CartVisibilityListener en cada cambio de vista para que
 * el carrito solo sea visible en sell_view.fxml.
 */
public class NavigationService {
    /**
     * Interfaz funcional para reaccionar a cambios de visibilidad del carrito.
     * Implementada por MainController para mostrar/ocultar cartContainer.
     */
    @FunctionalInterface
    public interface CartVisibilityListener {
        void onCartVisibilityChanged(boolean shouldShowCart);
    }

    @FunctionalInterface
    public interface SearchBarVisibilityListener {
        void onSearchBarVisibilityChanged(boolean shouldShowSearch);
    }

    private final ScrollPane mainContent;
    private final VBox loadingOverlay;
    private final ServiceContainer container;

    private String currentFxmlPath;
    private Object activeView;
    private final List<Searchable> searchHandlers = new ArrayList<>();

    // Listener opcional para visibilidad del carrito
    private CartVisibilityListener cartVisibilityListener;
    private SearchBarVisibilityListener searchBarVisibilityListener;

    // Control de Accesos
    private static final Map<String, String> ACCESS_RULES = new HashMap<>();
    static {
        ACCESS_RULES.put("/view/product/products.fxml", "PRODUCTOS");
        ACCESS_RULES.put("/view/product/categories.fxml", "PRODUCTOS");
        ACCESS_RULES.put("/view/receipt/sales.fxml", "HISTORIAL");
        ACCESS_RULES.put("/view/closure/closure_history.fxml", "CIERRES");
        ACCESS_RULES.put("/view/dialog/clients.fxml", "CLIENTES");
        ACCESS_RULES.put("/view/user/manage_users.fxml", "usuario.crear");
        ACCESS_RULES.put("/view/user/manage_roles.fxml", "rol.editar");
        ACCESS_RULES.put("/view/config/sale_config.fxml", "CONFIGURACION");
        ACCESS_RULES.put("/view/customization/customization_panel.fxml", "CONFIGURACION");
        ACCESS_RULES.put("/view/reports/seller_report.fxml", "reporte.venta|reporte.vendedores");
        ACCESS_RULES.put("/view/reports/client_report.fxml", "reporte.cliente|HISTORIAL");
        ACCESS_RULES.put("/view/receipt/return_list.fxml", "venta.devolucion");
        ACCESS_RULES.put("/view/vat/vat_management.fxml", "admin.iva");
        ACCESS_RULES.put("/view/dialog/price_lists.fxml", "admin.precios_masivo|PRODUCTOS");
        ACCESS_RULES.put("/view/dialog/promotions.fxml", "admin.promociones|PRODUCTOS");
        ACCESS_RULES.put("/view/receipt/fiscal_documents.fxml", "fiscal.reenviar|fiscal.config|HISTORIAL");
    }

    /** Vista que debe mostrar el carrito */
    private static final String SELL_VIEW = "/view/cart/sell_view.fxml";

    public NavigationService(ScrollPane mainContent, VBox loadingOverlay, ServiceContainer container) {
        this.mainContent = mainContent;
        this.loadingOverlay = loadingOverlay;
        this.container = container;
    }

    /**
     * Registra el listener que MainController usar\u00e1 para mostrar/ocultar el
     * carrito.
     */
    public void setCartVisibilityListener(CartVisibilityListener listener) {
        this.cartVisibilityListener = listener;
    }

    public void setSearchBarVisibilityListener(SearchBarVisibilityListener listener) {
        this.searchBarVisibilityListener = listener;
    }

    public void navigateTo(String fxmlPath) {
        navigateTo(fxmlPath, null);
    }

    public <T> void navigateTo(String fxmlPath, Consumer<T> initializer) {
        this.currentFxmlPath = fxmlPath;
        // 1. Verificar Permisos (Control de Acceso)
        String requiredPermission = ACCESS_RULES.get(fxmlPath);
        AuthorizationService auth = container.getAuthService();
        IAuditRepository audit = container.getAuditRepository();
        int userId = container.getUserSession().getCurrentUser() != null
                ? container.getUserSession().getCurrentUser().getUserId()
                : -1;

        if (requiredPermission != null && !auth.hasPermission(requiredPermission)) {
            // Bloqueado: Registrar intento fallido
            try {
                audit.logAccess(userId, "ACCESS_DENIED", fxmlPath,
                        "Intento de acceso sin permiso: " + requiredPermission);
            } catch (SQLException ignored) {
            }

            AlertUtil.showError(container.getBundle().getString("alert.access_denied"),
                    container.getBundle().getString("error.no_permission"));
            return;
        }

        if (loadingOverlay != null)
            loadingOverlay.setVisible(true);
        searchHandlers.clear();

        // Notificar visibilidad del carrito y búsqueda ANTES de cargar
        boolean isSellView = SELL_VIEW.equals(fxmlPath);
        if (cartVisibilityListener != null) {
            cartVisibilityListener.onCartVisibilityChanged(isSellView);
        }

        // --- OPTIMIZACIÓN: Auditoría asíncrona para no bloquear el hilo UI ---
        new Thread(() -> {
            try {
                audit.logAccess(userId, "NAVIGATE", fxmlPath, "Acceso a m\u00f3dulo");
            } catch (Exception ignored) {
            }
        }, "AsyncAudit").start();

        // --- OPTIMIZACIÓN: Carga diferida ---
        // Usamos runLater para dar tiempo a que el 'loadingOverlay' se dibuje
        // y que el botón muestre su efecto de clic antes del bloqueo por FXML.
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath), container.getBundle());
                Node viewNode = loader.load();
                activeView = loader.getController();

                mainContent.setContent(viewNode);

                // Inyección y otros procesos secundarios
                if (activeView instanceof Injectable) {
                    ((Injectable) activeView).inject(container);
                }

                if (initializer != null) {
                    @SuppressWarnings("unchecked")
                    T controller = (T) activeView;
                    initializer.accept(controller);
                }

                if (activeView instanceof Searchable) {
                    registerSearchHandler((Searchable) activeView);
                }
                if (searchBarVisibilityListener != null) {
                    searchBarVisibilityListener.onSearchBarVisibilityChanged(isSellView);
                }
            } catch (Exception e) {
                e.printStackTrace();
                AlertUtil.showError(container.getBundle().getString("alert.error"),
                        container.getBundle().getString("error.navigation") + ": " + fxmlPath);
            } finally {
                if (loadingOverlay != null)
                    loadingOverlay.setVisible(false);
            }
        });
    }

    /**
     * Allows components (controllers or fragments) to register for global search.
     */
    public void registerSearchHandler(Searchable handler) {
        if (!searchHandlers.contains(handler)) {
            searchHandlers.add(handler);
        }
    }

    /**
     * Delegates global search to all registered handlers for the current view.
     */
    public void search(String text) {
        for (Searchable handler : searchHandlers) {
            handler.handleSearch(text);
        }
    }

    public void reloadCurrent() {
        if (currentFxmlPath != null) {
            navigateTo(currentFxmlPath);
        }
    }
}





