package com.mycompany.ventacontrolfx.service;

import com.mycompany.ventacontrolfx.util.Injectable;
import com.mycompany.ventacontrolfx.util.Searchable;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    private final ScrollPane mainContent;
    private final VBox loadingOverlay;
    private final ServiceContainer container;

    private Object activeView;
    private final List<Searchable> searchHandlers = new ArrayList<>();

    // Listener opcional para visibilidad del carrito
    private CartVisibilityListener cartVisibilityListener;

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

    public void navigateTo(String fxmlPath) {
        if (loadingOverlay != null)
            loadingOverlay.setVisible(true);
        searchHandlers.clear(); // Clear search handlers for the new view

        // Notificar visibilidad del carrito ANTES de cargar la vista
        if (cartVisibilityListener != null) {
            boolean showCart = SELL_VIEW.equals(fxmlPath);
            cartVisibilityListener.onCartVisibilityChanged(showCart);
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node viewNode = loader.load();
            activeView = loader.getController();

            // Automatic Injection
            if (activeView instanceof Injectable) {
                ((Injectable) activeView).inject(container);
            }

            // Automatic Search Registration if the main controller is Searchable
            if (activeView instanceof Searchable) {
                registerSearchHandler((Searchable) activeView);
            }

            mainContent.setContent(viewNode);
        } catch (IOException e) {
            e.printStackTrace();
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
