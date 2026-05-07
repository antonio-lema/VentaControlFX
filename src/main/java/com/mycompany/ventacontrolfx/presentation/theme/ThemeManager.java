package com.mycompany.ventacontrolfx.presentation.theme;

import com.mycompany.ventacontrolfx.domain.repository.IAppSettingsRepository;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.paint.Color;
import java.sql.SQLException;
import java.util.Map;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.net.URL;

/**
 * Gestor dinámico de la estética de la aplicación.
 * Orquesta la inyección de CSS base y dinámico en las escenas.
 */
public class ThemeManager {

    private final IAppSettingsRepository settingsRepository;
    private final ThemeCssGenerator cssGenerator;
    private String dynamicStylesheetUrl = null;

    private static final String[] STYLE_FILES = {
            "/styles/variables.css", "/styles/main.css", "/styles/layout/sidebar.css",
            "/styles/layout/topbar.css", "/styles/layout/status_bar.css", "/styles/components/botones.css",
            "/styles/components/tarjetas.css", "/styles/components/tablas.css", "/styles/components/formularios.css",
            "/styles/components/login.css", "/styles/components/carrito.css", "/styles/components/dialogos.css",
            "/styles/components/config.css", "/styles/components/estetica.css", "/styles/components/historial.css",
            "/styles/components/pago.css", "/styles/components/devoluciones.css", "/styles/components/informes.css",
            "/styles/components/vender.css", "/styles/components/personal.css", "/styles/components/alertas.css",
            "/styles/skeleton.css"
    };

    public ThemeManager(IAppSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
        this.cssGenerator = new ThemeCssGenerator();
    }

    /**
     * Aplica el sistema modular de estilos base y el tema dinámico a una escena.
     */
    public void applyFullTheme(Scene scene) {
        for (String styleFile : STYLE_FILES) {
            URL resource = getClass().getResource(styleFile);
            if (resource != null) {
                String externalForm = resource.toExternalForm();
                if (!scene.getStylesheets().contains(externalForm)) {
                    scene.getStylesheets().add(externalForm);
                }
            }
        }
        applyTheme(scene);
    }

    public void applyTheme(Scene scene) {
        try {
            applyTheme(scene, settingsRepository.getAllSettings());
        } catch (SQLException e) {
            System.err.println("Error cargando configuración estética: " + e.getMessage());
        }
    }

    /**
     * Aplica un conjunto de ajustes (pueden ser temporales para preview) a la escena.
     */
    public void applyTheme(Scene scene, Map<String, String> settings) {
        if (scene == null) return;

        String css = cssGenerator.generate(settings);
        String encodedCss = Base64.getEncoder().encodeToString(css.getBytes(StandardCharsets.UTF_8));
        String newUrl = "data:text/css;base64," + encodedCss;

        scene.getStylesheets().removeIf(url -> url.startsWith("data:text/css;base64,"));
        scene.getStylesheets().add(newUrl);
        this.dynamicStylesheetUrl = newUrl;

        String bg = settings.get("ui.bg_main");
        setTheme(scene.getRoot(), bg != null && !isLightColor(bg));
    }

    /**
     * Alterna entre modo claro y oscuro aplicando una clase al root.
     */
    public void setTheme(Parent root, boolean isDark) {
        root.getStyleClass().removeAll("light-theme", "dark-theme");
        root.getStyleClass().add(isDark ? "dark-theme" : "light-theme");
    }

    private boolean isLightColor(String hex) {
        try {
            Color c = Color.valueOf(hex);
            return (c.getRed() * 0.299 + c.getGreen() * 0.587 + c.getBlue() * 0.114) > 0.5;
        } catch (Exception e) { return false; }
    }

    public void saveAndApply(String key, String value, Scene scene) throws SQLException {
        settingsRepository.saveSetting(key, value);
        applyTheme(scene);
    }

    public String getDynamicStylesheetUrl() {
        return dynamicStylesheetUrl;
    }
}

