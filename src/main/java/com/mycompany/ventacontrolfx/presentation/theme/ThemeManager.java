package com.mycompany.ventacontrolfx.presentation.theme;

import com.mycompany.ventacontrolfx.domain.repository.IAppSettingsRepository;
import javafx.scene.Scene;
import javafx.scene.Parent;
import java.sql.SQLException;
import java.util.Map;
import java.util.List;
import java.lang.String;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.net.URL;

/**
 * Gestor dinámico de la estética de la aplicación.
 * Permite cambiar colores y tamaños en caliente inyectando CSS dinámico.
 */
public class ThemeManager {

    private final IAppSettingsRepository settingsRepository;
    private String dynamicStylesheetUrl = null;

    private static final String[] STYLE_FILES = {
            "/styles/variables.css",
            "/styles/main.css",
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
            "/styles/components/estetica.css",
            "/styles/components/historial.css",
            "/styles/components/pago.css",
            "/styles/components/devoluciones.css",
            "/styles/components/informes.css"
    };

    public ThemeManager(IAppSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    /**
     * Aplica el sistema modular de estilos base y el tema dinámico a una escena.
     */
    public void applyFullTheme(Scene scene) {
        // 1. Aplicar estilos base (archivos CSS)
        for (String styleFile : STYLE_FILES) {
            URL resource = getClass().getResource(styleFile);
            if (resource != null) {
                if (!scene.getStylesheets().contains(resource.toExternalForm())) {
                    scene.getStylesheets().add(resource.toExternalForm());
                }
            }
        }

        // 2. Aplicar personalizaciones dinámicas (desde BD)
        applyTheme(scene);
    }

    /**
     * Aplica el tema guardado en la BD a la escena proporcionada.
     */
    public void applyTheme(Scene scene) {
        try {
            applyTheme(scene, settingsRepository.getAllSettings());
        } catch (SQLException e) {
            System.err.println("Error cargando configuración estética: " + e.getMessage());
        }
    }

    /**
     * Aplica un conjunto de ajustes (pueden ser temporales para preview) a la
     * escena.
     */
    public void applyTheme(Scene scene, Map<String, String> settings) {
        if (scene == null)
            return;

        String css = generateCss(settings);
        String encodedCss = Base64.getEncoder().encodeToString(css.getBytes(StandardCharsets.UTF_8));
        String newUrl = "data:text/css;base64," + encodedCss;

        // Limpiar cualquier stylesheet dinámico previo de ESTA escena específica
        scene.getStylesheets().removeIf(url -> url.startsWith("data:text/css;base64,"));

        // Añadir el nuevo
        scene.getStylesheets().add(newUrl);
        this.dynamicStylesheetUrl = newUrl;

        // Aplicar clase de modo oscuro si es necesario
        String mode = settings.getOrDefault("ui.theme_mode", "LIGHT");
        setTheme(scene.getRoot(), "DARK".equals(mode));
    }

    /**
     * Alterna entre modo claro y oscuro aplicando una clase al root.
     * 
     * @param root   El nodo raíz
     * @param isDark true para modo oscuro
     */
    public void setTheme(Parent root, boolean isDark) {
        if (!isDark) {
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
     * Genera un bloque CSS (.root) basado en los ajustes.
     */
    private String generateCss(Map<String, String> settings) {
        StringBuilder sb = new StringBuilder();

        // 1. Bloque principal .root
        sb.append(".root {\n");

        // Colores de Marca
        java.lang.String primary = settings.get("ui.primary_color");
        if (primary != null) {
            sb.append("  -fx-custom-color-primary: ").append(primary).append(";\n");
            sb.append("  -fx-custom-color-primary-dark: ").append(primary).append(";\n");
            sb.append("  -fx-custom-color-primary-light: ").append(primary).append(";\n");
            sb.append("  -fx-custom-color-primary-bg: ").append(primary).append("1A;\n");
            sb.append("  -fx-custom-color-primary-hover: ").append(primary).append("33;\n");
            sb.append("  -fx-custom-color-primary-alpha40: ").append(primary).append("66;\n");
            sb.append("  -fx-custom-color-primary-alpha60: ").append(primary).append("99;\n");

            // Sombras dinámicas - Look Premium
            String shadowColor = primary + "66"; // 40% alpha
            sb.append("  -fx-shadow-color-primary: ").append(shadowColor).append(";\n");
            sb.append("  -fx-shadow-primary: dropshadow(three-pass-box, ").append(shadowColor)
                    .append(", 25, 0, 0, 10);\n");

            // Variantes de sombras
            sb.append("  -fx-shadow-success: dropshadow(three-pass-box, rgba(16, 185, 129, 0.4), 15, 0, 0, 5);\n");
            sb.append("  -fx-shadow-danger: dropshadow(three-pass-box, rgba(239, 68, 68, 0.4), 15, 0, 0, 5);\n");
            sb.append("  -fx-shadow-warning: dropshadow(three-pass-box, rgba(245, 158, 11, 0.4), 15, 0, 0, 5);\n");

            // Gradientes dinámicos
            sb.append("  -fx-grad-primary: linear-gradient(to bottom, ").append(primary).append(", ").append(primary)
                    .append("CC);\n");
            sb.append("  -fx-grad-primary-soft: linear-gradient(to bottom right, ").append(primary).append("4D, ")
                    .append(primary).append("26);\n");

            sb.append("  -fx-grad-success: linear-gradient(to bottom, #10b981, #059669);\n");
            sb.append("  -fx-grad-danger: linear-gradient(to bottom, #ef4444, #dc2626);\n");
            sb.append("  -fx-grad-warning: linear-gradient(to bottom, #f59e0b, #d97706);\n");
        }

        String secondary = settings.get("ui.secondary_color");
        if (secondary != null && !secondary.isEmpty()) {
            sb.append("  -fx-custom-color-secondary: ").append(secondary).append(";\n");
        }

        String text = settings.get("ui.text_main");
        if (text != null) {
            sb.append("  -fx-text-custom-main: ").append(text).append(";\n");
            sb.append("  -fx-text-custom-medium: ").append(text).append(";\n");
            sb.append("  -fx-text-custom-muted: ").append(text).append("B3;\n");
            sb.append("  -fx-text-custom-on-primary: #ffffff;\n");
        }

        // Font Sizes
        String fontSize = settings.get("ui.font_size");
        if (fontSize != null) {
            try {
                int base = (int) Double.parseDouble(fontSize);
                sb.append("  -fx-font-size-base: ").append(base).append("px;\n");
                sb.append("  -fx-font-size-large: ").append(base + 4).append("px;\n");
                sb.append("  -fx-font-size-xl: ").append(base + 12).append("px;\n");
                sb.append("  -fx-font-size-small: ").append(base - 2).append("px;\n");
            } catch (Exception ignored) {
            }
        }

        // Border Radii
        String borderRadius = settings.get("ui.border_radius");
        if (borderRadius != null) {
            try {
                int br = (int) Double.parseDouble(borderRadius);
                sb.append("  -fx-radius-sm: ").append(br / 2).append("px;\n");
                sb.append("  -fx-radius-md: ").append(br).append("px;\n");
                sb.append("  -fx-radius-lg: ").append(br * 2).append("px;\n");
                sb.append("  -fx-radius-xl: ").append(br * 3).append("px;\n");
            } catch (Exception ignored) {
            }
        }

        // Card Scale & Shadow (Variables)
        String cardScale = settings.getOrDefault("ui.card_scale", "1.0");
        String cardShadow = settings.getOrDefault("ui.card_shadow", "15");
        String cardBorderWidth = settings.getOrDefault("ui.card_border_width", "1.5");
        String cardHoverLift = settings.getOrDefault("ui.card_hover_lift", "8");
        String cardHoverScale = settings.getOrDefault("ui.card_hover_scale", "1.02");

        sb.append("  -fx-card-scale: ").append(cardScale).append(";\n");
        sb.append("  -fx-card-shadow-size: ").append(cardShadow).append("px;\n");
        sb.append("  -fx-card-border-width: ").append(cardBorderWidth).append("px;\n");
        sb.append("  -fx-card-hover-lift: -").append(cardHoverLift).append("px;\n");
        sb.append("  -fx-card-hover-scale: ").append(cardHoverScale).append(";\n");
        sb.append("}\n\n");

        // Dynamic Grid Sizing
        try {
            double scale = java.lang.Double.parseDouble(cardScale);
            int tileW = (int) (200 * scale) + 15;
            int tileH = (int) (280 * scale) + 15;
            sb.append(".products-grid {\n");
            sb.append("  -fx-tile-width: ").append(tileW).append("px;\n");
            sb.append("  -fx-tile-height: ").append(tileH).append("px;\n");
            sb.append("}\n\n");

            sb.append(".user-card {\n");
            sb.append("  -fx-pref-width: ").append((int) (280 * scale)).append("px;\n");
            sb.append("}\n\n");
        } catch (java.lang.Exception ignored) {
        }

        // 2. Tema Claro/Oscuro dinámico
        String bg = settings.get("ui.bg_main");
        if (bg != null) {
            // Aplicar fondos al .root para que afecte fuera de clases de tema si es
            // necesario
            sb.append(".root {\n");
            sb.append("  -fx-bg-main: ").append(bg).append(";\n");
            sb.append("  -fx-bg-surface: #ffffff;\n");
            sb.append("  -fx-bg-sidebar: ").append(bg).append(";\n");
            sb.append("  -fx-bg-topbar: #ffffff;\n");
            sb.append("  -fx-grad-sidebar: ").append(bg).append(";\n");
            sb.append("}\n\n");

            sb.append(".light-theme {\n");
            sb.append("  -fx-bg-main: ").append(bg).append(";\n");
            sb.append("  -fx-bg-surface: #ffffff;\n");
            sb.append("  -fx-bg-sidebar: ").append(bg).append(";\n");
            sb.append("  -fx-bg-topbar: #ffffff;\n");
            sb.append("  -fx-grad-sidebar: ").append(bg).append(";\n");
            sb.append("}\n\n");
        }

        // 3. Overrides de componentes para asegurar consistencia
        if (fontSize != null || borderRadius != null) {
            try {
                int fBase = fontSize != null ? (int) Double.parseDouble(fontSize) : 14;
                int bRadius = borderRadius != null ? (int) Double.parseDouble(borderRadius) : 8;

                sb.append(".button, .text-field, .combo-box {\n");
                sb.append("  -fx-font-size: ").append(fBase).append("px;\n");
                sb.append("  -fx-background-radius: ").append(bRadius).append("px;\n");
                sb.append("  -fx-border-radius: ").append(bRadius).append("px;\n");
                sb.append("}\n\n");

                sb.append(".modern-btn-primary, .modern-btn-secondary {\n");
                sb.append("  -fx-background-radius: ").append(bRadius * 1.5).append("px;\n");
                sb.append("  -fx-border-radius: ").append(bRadius * 1.5).append("px;\n");
                sb.append("}\n");
            } catch (Exception ignored) {
            }
        }

        return sb.toString();
    }

    private void appendVar(StringBuilder sb, String varName, String value) {
        if (value != null && !value.isEmpty()) {
            sb.append("  ").append(varName).append(": ").append(value).append(";\n");
        }
    }

    public void saveAndApply(String key, String value, Scene scene) throws SQLException {
        settingsRepository.saveSetting(key, value);
        applyTheme(scene);
    }
}
