package com.mycompany.ventacontrolfx.presentation.theme;

import com.mycompany.ventacontrolfx.domain.repository.IAppSettingsRepository;
import javafx.scene.Scene;
import javafx.scene.Parent;
import java.sql.SQLException;
import java.util.Map;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
import java.net.URL;
import javafx.scene.paint.Color;

/**
 * Gestor din\u00e1mico de la est\u00e9tica de la aplicaci\u00f3n.
 * Permite cambiar colores y tama\u00f1os en caliente inyectando CSS
 * din\u00e1mico.
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
            "/styles/components/informes.css",
            "/styles/components/vender.css",
            "/styles/components/personal.css",
            "/styles/components/alertas.css",
            "/styles/skeleton.css"
    };

    public ThemeManager(IAppSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    /**
     * Aplica el sistema modular de estilos base y el tema din\u00e1mico a una
     * escena.
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

        // 2. Aplicar personalizaciones din\u00e1micas (desde BD)
        applyTheme(scene);
    }

    /**
     * Aplica el tema guardado en la BD a la escena proporcionada.
     */
    public void applyTheme(Scene scene) {
        try {
            applyTheme(scene, settingsRepository.getAllSettings());
        } catch (SQLException e) {
            System.err.println("Error cargando configuraci\u00f3n est\u00e9tica: " + e.getMessage());
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

        // Limpiar cualquier stylesheet din\u00e1mico previo de ESTA escena
        // espec\u00edfica
        scene.getStylesheets().removeIf(url -> url.startsWith("data:text/css;base64,"));

        // A\u00f1adir el nuevo
        scene.getStylesheets().add(newUrl);
        this.dynamicStylesheetUrl = newUrl;

        // Aplicar clase de modo oscuro autom\u00e1ticamente si es necesario
        String bg = settings.get("ui.bg_main");
        setTheme(scene.getRoot(), bg != null && !isLightColor(bg));
    }

    /**
     * Alterna entre modo claro y oscuro aplicando una clase al root.
     * 
     * @param root   El nodo ra\u00edz
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
        String bg = settings.get("ui.bg_main");
        // Calcular si es oscuro autom\u00e1ticamente seg\u00fan el color de fondo para
        // evitar
        // mezclas
        boolean isDark = bg != null && !isLightColor(bg);

        // 1. Bloque principal .root
        sb.append(".root {\n");

        // Colores de Marca
        String primary = settings.get("ui.primary_color");
        if (primary != null) {
            String primaryLight = brighten(primary, 0.3);
            String primaryDark = darken(primary, 0.1);
            String primaryGlow = primary + "99"; // 60% alpha

            sb.append("  -fx-custom-color-primary: ").append(primary).append(";\n");
            sb.append("  -fx-custom-color-primary-dark: ").append(primaryDark).append(";\n");
            sb.append("  -fx-custom-color-primary-light: ").append(primaryLight).append(";\n");
            sb.append("  -fx-custom-color-primary-bg: ").append(primary).append("1A;\n");
            sb.append("  -fx-custom-color-primary-hover: ").append(primary).append("33;\n");
            sb.append("  -fx-custom-color-primary-alpha40: ").append(primary).append("66;\n");
            sb.append("  -fx-custom-color-primary-alpha60: ").append(primary).append("99;\n");

            // Sombras din\u00e1micas - Look Premium (Glow Effect)
            sb.append("  -fx-shadow-color-primary: ").append(primaryGlow).append(";\n");
            sb.append("  -fx-shadow-primary: dropshadow(three-pass-box, ").append(primaryGlow)
                    .append(", 40, 0.0, 0, 12);\n");
            sb.append("  -fx-shadow-primary-intense: dropshadow(three-pass-box, ").append(primary)
                    .append("CC, 20, 0.0, 0, 6);\n");

            // Gradientes din\u00e1micos - Reales (Light -> Dark)
            sb.append("  -fx-grad-primary: linear-gradient(to bottom right, ").append(primaryLight).append(", ")
                    .append(primaryDark).append(");\n");
            sb.append("  -fx-grad-primary-soft: linear-gradient(to bottom right, ").append(primary).append("4D, ")
                    .append(primary).append("26);\n");
            sb.append("  -fx-grad-sidebar-active: linear-gradient(to bottom right, ").append(primaryLight)
                    .append("E6, ").append(primaryDark).append(");\n");
        } else {
            // Fallback for active sidebar when primary is null
            sb.append(
                    "  -fx-grad-sidebar-active: linear-gradient(to bottom right, -fx-custom-color-primary-light, -fx-custom-color-primary-dark);\n");
        }

        // Variantes de sombras fijas
        sb.append("  -fx-shadow-success: dropshadow(three-pass-box, rgba(16, 185, 129, 0.4), 15, 0, 0, 5);\n");
        sb.append("  -fx-shadow-danger: dropshadow(three-pass-box, rgba(239, 68, 68, 0.4), 15, 0, 0, 5);\n");
        sb.append("  -fx-shadow-warning: dropshadow(three-pass-box, rgba(245, 158, 11, 0.4), 15, 0, 0, 5);\n");

        sb.append("  -fx-grad-success: linear-gradient(to bottom, #10b981, #059669);\n");
        sb.append("  -fx-grad-danger: linear-gradient(to bottom, #ef4444, #dc2626);\n");
        sb.append("  -fx-grad-warning: linear-gradient(to bottom, #f59e0b, #d97706);\n");

        String secondary = settings.get("ui.secondary_color");
        if (secondary != null && !secondary.isEmpty()) {
            sb.append("  -fx-custom-color-secondary: ").append(secondary).append(";\n");
        }

        String text = settings.get("ui.text_main");
        if (text != null && (!isDark || isLightColor(text))) {
            sb.append("  -fx-text-custom-main: ").append(text).append(";\n");
            sb.append("  -fx-text-custom-medium: ").append(text).append(";\n");
            sb.append("  -fx-text-custom-light: ").append(text).append("B3;\n"); // 70% alpha
            sb.append("  -fx-text-custom-muted: ").append(text).append("80;\n"); // 50% alpha
            sb.append("  -fx-text-custom-on-primary: #ffffff;\n");
        }

        String textCards = settings.getOrDefault("ui.text_cards", text);
        if (textCards != null && (!isDark || isLightColor(textCards))) {
            sb.append("  -fx-text-custom-cards: ").append(textCards).append(";\n");
            sb.append("  -fx-text-custom-cards-muted: ").append(textCards).append("80;\n"); // 50% alpha
        }

        String textPrice = settings.getOrDefault("ui.text_price", text);
        if (textPrice != null && (!isDark || isLightColor(textPrice))) {
            sb.append("  -fx-text-custom-price: ").append(textPrice).append(";\n");
        }

        // El color elegido por el usuario ES el color SUPERIOR del gradiente.
        // Gradiente: vibrante (top) \u2192 35% m\u00e1s oscuro (mid) \u2192 65%
        // m\u00e1s oscuro (bot)
        // As\u00ed, con sidebar = #1e88e5 (azul) se ver\u00e1: azul \u2192 azul medio
        // \u2192 azul casi
        // negro
        final String sidebarTop = settings.getOrDefault("ui.sidebar_bg", "#0f172a");
        final String sidebarMid = darken(sidebarTop, 0.35);
        final String sidebarBot = darken(sidebarTop, 0.65);
        sb.append("  -fx-bg-sidebar: ").append(sidebarTop).append(";\n");

        // Nuevo color de texto del sidebar (por defecto blanco) y sus versiones con
        // transparencia
        final String sidebarText = settings.getOrDefault("ui.sidebar_text_color", "#ffffff");
        sb.append("  -fx-custom-color-sidebar-text: ").append(sidebarText).append(";\n");
        sb.append("  -fx-custom-color-sidebar-text-a70: ").append(sidebarText).append("B3;\n"); // 70% alpha
        sb.append("  -fx-custom-color-sidebar-text-a60: ").append(sidebarText).append("99;\n"); // 60% alpha

        // GLOBAL TEXT COLOR OVERRIDES (Phase 4)
        if (text != null) {
            boolean useTextOverride = !isDark || isLightColor(text);
            if (useTextOverride) {
                String themeClass = isDark ? ".dark-theme" : ".light-theme";
                sb.append("}\n\n");
                sb.append(themeClass).append(", ").append(themeClass).append(" .label, ").append(themeClass)
                        .append(" .text, ")
                        .append(themeClass).append(" .button, ").append(themeClass).append(" .toggle-button, ")
                        .append(themeClass).append(" .text-field, ").append(themeClass).append(" .text-area, ")
                        .append(themeClass).append(" .combo-box {\n");
                sb.append("  -fx-text-fill: -fx-text-custom-main;\n");
                sb.append("}\n\n");
                sb.append(".root {\n");
            }
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
                sb.append("  -fx-layout-radius: ").append(br * 2).append("px;\n");
            } catch (Exception ignored) {
            }
        }

        // Card Scale & Shadow (Variables)
        String cardScale = settings.getOrDefault("ui.card_scale", "1.0");
        String cardShadow = settings.getOrDefault("ui.card_shadow", "15");
        String cardBorderWidth = settings.getOrDefault("ui.card_border_width", "1.5");
        String cardHoverLift = settings.getOrDefault("ui.card_hover_lift", "8");
        String cardHoverScale = settings.getOrDefault("ui.card_hover_scale", "1.02");

        double shadowSize = 15;
        try {
            shadowSize = Double.parseDouble(cardShadow);
        } catch (Exception ignored) {
        }

        sb.append("  -fx-card-scale: ").append(cardScale).append(";\n");
        sb.append("  -fx-card-shadow: dropshadow(three-pass-box, -fx-shadow-color-medium, ")
                .append(shadowSize).append(", 0, 0, 3);\n");
        sb.append("  -fx-card-shadow-hover: dropshadow(three-pass-box, -fx-shadow-color-primary, ")
                .append(shadowSize * 1.5).append(", 0, 0, 6);\n");
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

        // 2. Tema Claro/Oscuro din\u00e1mico
        // String bg ya declarada arriba

        if (bg != null) {
            // Solo sobreescribimos si el color encaja con el modo (evita forzar fondos
            // claros en modo oscuro)
            boolean fitsMode = !isDark || !isLightColor(bg);

            if (fitsMode) {
                String themeClass = isDark ? ".dark-theme" : ".light-theme";
                sb.append(themeClass).append(" {\n");
                sb.append("  -fx-bg-main: ").append(bg).append(";\n");
                sb.append("  -fx-bg-sidebar: ").append(bg).append(";\n");
                sb.append("  -fx-grad-sidebar: ").append(bg).append(";\n");

                if (isDark) {
                    String surface = brightenAbsolute(bg, 0.15); // Aumento del 15% para contraste visible
                    String subtle = brightenAbsolute(bg, 0.08);
                    sb.append("  -fx-bg-surface: ").append(surface).append(";\n");
                    sb.append("  -fx-bg-subtle: ").append(subtle).append(";\n");
                } else {
                    sb.append("  -fx-bg-surface: #ffffff;\n");
                }
                sb.append("}\n\n");
            }
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
                sb.append("  -fx-background-radius: ").append((int) (bRadius * 1.5)).append("px;\n");
                sb.append("  -fx-border-radius: ").append((int) (bRadius * 1.5)).append("px;\n");
                sb.append("}\n\n");

                // HARD OVERRIDES FOR CARDS & CLIPS (Phase 3)
                sb.append(
                        ".cart-panel, .product-box, .client-card, .user-card, .stat-card, .filter-card, .detail-panel, .view-header-icon-wrap {\n");
                sb.append("  -fx-background-radius: ").append(bRadius * 2).append("px !important;\n");
                sb.append("  -fx-border-radius: ").append(bRadius * 2).append("px !important;\n");
                sb.append("}\n\n");

                // Headers del carrito (solo el de arriba del todo lleva redondeo superior)
                sb.append(".cart-customer-header {\n");
                sb.append("  -fx-background-radius: ").append(bRadius * 2).append("px ").append(bRadius * 2)
                        .append("px 0 0 !important;\n");
                sb.append("  -fx-border-radius: ").append(bRadius * 2).append("px ").append(bRadius * 2)
                        .append("px 0 0 !important;\n");
                sb.append("}\n\n");

                // Headers intermedios (cuadrados para que no se separen)
                sb.append(".cart-pricelist-header, .cart-items-header {\n");
                sb.append("  -fx-background-radius: 0 !important;\n");
                sb.append("  -fx-border-radius: 0 !important;\n");
                sb.append("}\n\n");

                // Resumen del carrito (parte inferior lleva redondeo inferior)
                sb.append(".checkout-summary {\n");
                sb.append("  -fx-background-radius: 0 0 ").append(bRadius * 2).append("px ").append(bRadius * 2)
                        .append("px !important;\n");
                sb.append("  -fx-border-radius: 0 0 ").append(bRadius * 2).append("px ").append(bRadius * 2)
                        .append("px !important;\n");
                sb.append("}\n\n");

                sb.append(".product-image-container {\n");
                sb.append("  -fx-background-radius: ").append(bRadius * 2).append("px ").append(bRadius * 2)
                        .append("px 0 0 !important;\n");
                sb.append("  -fx-border-radius: ").append(bRadius * 2).append("px ").append(bRadius * 2)
                        .append("px 0 0 !important;\n");
                sb.append("}\n\n");

                sb.append(".product-image-display {\n");
                sb.append("  -fx-background-radius: ").append((int) (bRadius * 1.5)).append("px !important;\n");
                sb.append("  -fx-border-radius: ").append((int) (bRadius * 1.5)).append("px !important;\n");
                sb.append("}\n\n");

                sb.append(".alert-root, .toast-root {\n");
                sb.append("  -fx-background-radius: ").append(bRadius * 3).append("px !important;\n");
                sb.append("  -fx-border-radius: ").append(bRadius * 3).append("px !important;\n");
                sb.append("}\n\n");

                sb.append(".alert-button {\n");
                sb.append("  -fx-background-radius: ").append(bRadius * 1.2).append("px !important;\n");
                sb.append("}\n\n");

                sb.append(".product-price-badge {\n");
                sb.append("  -fx-background-radius: 0px ").append((int) (bRadius * 1.5)).append("px 0px ")
                        .append((int) (bRadius * 1.5))
                        .append("px !important;\n");
                sb.append("}\n\n");

                sb.append(".product-add-btn {\n");
                sb.append("  -fx-background-radius: 0 0 ").append(bRadius * 2).append("px ").append(bRadius * 2)
                        .append("px !important;\n");
                sb.append("  -fx-border-radius: 0 0 ").append(bRadius * 2).append("px ").append(bRadius * 2)
                        .append("px !important;\n");
                sb.append("}\n");
            } catch (Exception ignored) {
            }
        }

        // \u00e2\u201d\u20ac\u00e2\u201d\u20ac BLOQUE .sidebar con gradiente directo
        // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac
        // JavaFX NO puede resolver variables CSS que contienen linear-gradient.
        // Por eso escribimos el valor del gradiente directamente en la regla CSS.
        // El color elegido (sidebarTop) es el vibrante de arriba;
        // sidebarMid y sidebarBot son versiones progresivamente m\u00e1s oscuras.
        sb.append(".sidebar {\n");
        sb.append("  -fx-background-color: linear-gradient(to bottom, ")
                .append(sidebarTop).append(" 0%, ")
                .append(sidebarMid).append(" 55%, ")
                .append(sidebarBot).append(" 100%);\n");
        sb.append("}\n");

        // \u00e2\u201d\u20ac\u00e2\u201d\u20ac BLOQUE .search-bar con tinte del color
        // primario
        // \u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac\u00e2\u201d\u20ac
        // La barra de b\u00fasqueda usa un tinte muy sutil del color primario.
        // En reposo: 5% de tinte. Al hacer foco: borde del color primario.
        if (primary != null) {
            String baseBg = isDark ? "#0f172a" : "#f8fafc";
            String baseBgFocus = isDark ? "#1e293b" : "#ffffff";
            String baseBorder = isDark ? "#1e293b" : "#e2e8f0";

            String searchBg = blendColors(baseBg, primary, 0.06); // tinte muy sutil
            String searchBgFocus = blendColors(baseBgFocus, primary, 0.04); // fondo en foco

            sb.append(".search-bar {\n");
            sb.append("  -fx-background-color: ").append(searchBg).append(";\n");
            sb.append("  -fx-border-color: ").append(blendColors(baseBorder, primary, 0.20)).append(";\n");
            sb.append("}\n");
            sb.append(".search-bar:focus-within {\n");
            sb.append("  -fx-background-color: ").append(searchBgFocus).append(";\n");
            sb.append("  -fx-border-color: ").append(primary).append(";\n");
            sb.append("}\n");
        }

        return sb.toString();
    }

    private void appendVar(StringBuilder sb, String varName, String value) {
        if (value != null && !value.isEmpty()) {
            sb.append("  ").append(varName).append(": ").append(value).append(";\n");
        }
    }

    // --- Helpers de Manipulaci\u00f3n de Color ---

    private String brightenAbsolute(String hex, double amount) {
        try {
            Color c = Color.valueOf(hex);
            double h = c.getHue();
            double s = c.getSaturation();
            double b = c.getBrightness();

            double bNew = Math.min(1.0, b + amount);
            // Desaturamos un poco al elevar para crear superficies m\u00e1s neutras en SaaS
            // style
            double sNew = Math.max(0.0, s - (amount * 0.4));

            return toHex(Color.hsb(h, sNew, bNew));
        } catch (Exception e) {
            return hex;
        }
    }

    private String brighten(String hex, double factor) {
        try {
            Color c = Color.valueOf(hex);
            Color b = c.deriveColor(0, 1.0, 1.0 + factor, 1.0);
            return toHex(b);
        } catch (Exception e) {
            return hex;
        }
    }

    private String darken(String hex, double factor) {
        try {
            Color c = Color.valueOf(hex);
            Color d = c.deriveColor(0, 1.0, 1.0 - factor, 1.0);
            return toHex(d);
        } catch (Exception e) {
            return hex;
        }
    }

    /**
     * Mezcla dos colores hexadecimales.
     * 
     * @param base  Color base (hex)
     * @param blend Color a mezclar (hex)
     * @param ratio Proporci\u00f3n del color blend (0.0 = solo base, 1.0 = solo
     *              blend)
     */
    private String blendColors(String base, String blend, double ratio) {
        try {
            Color c1 = Color.valueOf(base);
            Color c2 = Color.valueOf(blend);
            double r = c1.getRed() + (c2.getRed() - c1.getRed()) * ratio;
            double g = c1.getGreen() + (c2.getGreen() - c1.getGreen()) * ratio;
            double b = c1.getBlue() + (c2.getBlue() - c1.getBlue()) * ratio;
            r = Math.max(0.0, Math.min(1.0, r));
            g = Math.max(0.0, Math.min(1.0, g));
            b = Math.max(0.0, Math.min(1.0, b));
            return String.format("#%02X%02X%02X",
                    (int) (r * 255), (int) (g * 255), (int) (b * 255));
        } catch (Exception e) {
            return base;
        }
    }

    private boolean isLightColor(String hex) {
        try {
            Color c = Color.valueOf(hex);
            double brightness = c.getRed() * 0.299 + c.getGreen() * 0.587 + c.getBlue() * 0.114;
            return brightness > 0.5;
        } catch (Exception e) {
            return false;
        }
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255), (int) (c.getGreen() * 255), (int) (c.getBlue() * 255));
    }

    public void saveAndApply(String key, String value, Scene scene) throws SQLException {
        settingsRepository.saveSetting(key, value);
        applyTheme(scene);
    }
}
