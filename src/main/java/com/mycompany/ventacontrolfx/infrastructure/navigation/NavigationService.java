package com.mycompany.ventacontrolfx.infrastructure.navigation;

import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.util.Searchable;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import com.mycompany.ventacontrolfx.util.AuthorizationService;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import com.mycompany.ventacontrolfx.domain.repository.IAuditRepository;
import java.io.IOException;
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

    private Object activeView;
    private final List<Searchable> searchHandlers = new ArrayList<>();

    // Listener opcional para visibilidad del carrito
    private CartVisibilityListener cartVisibilityListener;
    private SearchBarVisibilityListener searchBarVisibilityListener;

    // Control de Accesos
    private static final Map<String, String> ACCESS_RULES = new HashMap<>();
    static {
        ACCESS_RULES.put("/view/products.fxml", "PRODUCTOS");
        ACCESS_RULES.put("/view/categories.fxml", "PRODUCTOS");
        ACCESS_RULES.put("/view/sales.fxml", "HISTORIAL");
        ACCESS_RULES.put("/view/closure_history.fxml", "CIERRES");
        ACCESS_RULES.put("/view/clients.fxml", "CLIENTES");
        ACCESS_RULES.put("/view/manage_users.fxml", "usuario.crear");
        ACCESS_RULES.put("/view/manage_roles.fxml", "rol.editar");
        ACCESS_RULES.put("/view/sale_config.fxml", "CONFIGURACION");
        ACCESS_RULES.put("/view/customization_panel.fxml", "CONFIGURACION");
        ACCESS_RULES.put("/view/seller_report.fxml", "reporte.vendedores");
        ACCESS_RULES.put("/view/client_report.fxml", "reporte.clientes");
        ACCESS_RULES.put("/view/return_list.fxml", "venta.devolucion");
        ACCESS_RULES.put("/view/vat_management.fxml", "admin.iva");
        ACCESS_RULES.put("/view/price_lists.fxml", "PRODUCTOS");
        ACCESS_RULES.put("/view/promotions.fxml", "PRODUCTOS");
        ACCESS_RULES.put("/view/fiscal_documents.fxml", "HISTORIAL");
    }

    /** Vista que debe mostrar el carrito */
    private static final String SELL_VIEW = "/view/sell_view.fxml";

    public NavigationService(ScrollPane mainContent, VBox loadingOverlay, ServiceContainer container) {
        this.mainContent = mainContent;
        this.loadingOverlay = loadingOverlay;
        this.container = container;
    }

    /**
     * Registra el listener que MainController usará para mostrar/ocultar el
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

            AlertUtil.showError("Acceso Denegado", "No tienes permiso para acceder a esta sección.");
            return;
        }

        // Permitido: Registrar acceso exitoso (Audit Trail)
        try {
            audit.logAccess(userId, "NAVIGATE", fxmlPath, "Acceso a módulo");
        } catch (SQLException ignored) {
        }

        if (loadingOverlay != null)
            loadingOverlay.setVisible(true);
        searchHandlers.clear(); // Clear search handlers for the new view

        // Notificar visibilidad del carrito y búsqueda ANTES de cargar la vista
        boolean isSellView = SELL_VIEW.equals(fxmlPath);

        if (cartVisibilityListener != null) {
            cartVisibilityListener.onCartVisibilityChanged(isSellView);
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node viewNode = loader.load();
            activeView = loader.getController();

            // Automatic Injection
            if (activeView instanceof Injectable) {
                ((Injectable) activeView).inject(container);
            }

            // Initialization if provided
            if (initializer != null) {
                @SuppressWarnings("unchecked")
                T controller = (T) activeView;
                initializer.accept(controller);
            }

            // Automatic Search Registration and Visibility
            boolean isSearchable = activeView instanceof Searchable;
            if (isSearchable) {
                registerSearchHandler((Searchable) activeView);
            }
            if (searchBarVisibilityListener != null) {
                searchBarVisibilityListener.onSearchBarVisibilityChanged(isSellView);
            }

            mainContent.setContent(viewNode);
        } catch (Exception e) {
            e.printStackTrace();
            AlertUtil.showError("Error de Navegación",
                    "No se pudo cargar la vista: " + fxmlPath + "\nError: " + e.getMessage());
        } finally {
            if (loadingOverlay != null)
                loadingOverlay.setVisible(false);
        }
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
}
