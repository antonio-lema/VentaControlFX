package com.mycompany.ventacontrolfx.util;

import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.Parent;
import java.net.URL;

/**
 * Gestor de temas para la aplicación.
 * Permite cargar el sistema modular de CSS y alternar entre modo claro y
 * oscuro.
 */
public class ThemeManager {

    private static final String[] STYLE_FILES = {
            "/styles/variables.css",
            "/styles/layout/sidebar.css",
            "/styles/layout/topbar.css",
            "/styles/layout/status_bar.css",
            "/styles/components/botones.css",
            "/styles/components/tarjetas.css",
            "/styles/components/tablas.css",
            "/styles/components/formularios.css",
            "/styles/components/login.css",
            "/styles/components/carrito.css",
            "/styles/components/dialogos.css",
            "/styles/components/config.css",
            "/styles/components/historial.css",
            "/styles/components/pago.css"
    };

    /**
     * Aplica el sistema modular de estilos a una escena.
     */
    public static void applyStyles(Scene scene) {
        applyStyles(scene.getStylesheets());
        // Aplicar tema claro por defecto
        setTheme(scene.getRoot(), true);
    }

    /**
     * Aplica el sistema modular de estilos a una lista de hojas de estilo.
     */
    public static void applyStyles(ObservableList<String> stylesheets) {
        stylesheets.clear();

        for (String styleFile : STYLE_FILES) {
            java.net.URL resource = ThemeManager.class.getResource(styleFile);
            if (resource != null) {
                stylesheets.add(resource.toExternalForm());
            } else {
                System.err.println("Warning: Style file not found: " + styleFile);
            }
        }
    }

    /**
     * Cambia el tema de la aplicación.
     * 
     * @param root    El nodo raíz de la escena (Parent)
     * @param isLight true para modo claro, false para modo oscuro
     */
    public static void setTheme(Parent root, boolean isLight) {
        if (isLight) {
            root.getStyleClass().remove("dark-theme");
            if (!root.getStyleClass().contains("light-theme")) {
                root.getStyleClass().add("light-theme");
            }
        } else {
            root.getStyleClass().remove("light-theme");
            if (!root.getStyleClass().contains("dark-theme")) {
                root.getStyleClass().add("dark-theme");
            }
        }
    }

    /**
     * Alterna entre los dos temas.
     */
    public static void toggleTheme(Parent root) {
        if (root.getStyleClass().contains("dark-theme")) {
            setTheme(root, true);
        } else {
            setTheme(root, false);
        }
    }
}
