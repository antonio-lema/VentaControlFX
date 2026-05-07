package com.mycompany.ventacontrolfx.presentation.theme;

import javafx.scene.paint.Color;
import java.util.Map;

/**
 * Generador especializado de bloques CSS dinámicos para el ThemeManager.
 * Reconstruye fielmente toda la estética dinámica de la aplicación.
 */
public class ThemeCssGenerator {

    public String generate(Map<String, String> settings) {
        StringBuilder sb = new StringBuilder();
        String bg = settings.get("ui.bg_main");
        boolean isDark = bg != null && !isLightColor(bg);

        // 1. Bloque principal .root
        sb.append(".root {\n");
        appendBrandingColors(sb, settings);
        appendStaticShadows(sb);
        appendSecondaryAndTextColors(sb, settings, isDark);
        appendSidebarColors(sb, settings);
        appendFontAndBorderRadii(sb, settings);
        appendCardMetrics(sb, settings);
        sb.append("}\n\n");

        // 2. Global Text Overrides
        appendGlobalTextOverrides(sb, settings, isDark);

        // 3. Grid & Layout Sizing
        appendLayoutSizing(sb, settings);

        // 4. Dynamic Theme Mode (Light/Dark)
        appendThemeMode(sb, settings, isDark, bg);

        // 5. Hard Component Overrides (Phase 3 & 4)
        appendHardComponentOverrides(sb, settings);

        // 6. Sidebar & Search Bar Specifics
        appendSidebarGradient(sb, settings);
        appendSearchBarTints(sb, settings, isDark);

        return sb.toString();
    }

    private void appendBrandingColors(StringBuilder sb, Map<String, String> settings) {
        String primary = settings.get("ui.primary_color");
        if (primary != null) {
            String primaryLight = brighten(primary, 0.3);
            String primaryDark = darken(primary, 0.1);
            String primaryGlow = primary + "99";

            sb.append("  -fx-custom-color-primary: ").append(primary).append(";\n");
            sb.append("  -fx-custom-color-primary-dark: ").append(primaryDark).append(";\n");
            sb.append("  -fx-custom-color-primary-light: ").append(primaryLight).append(";\n");
            sb.append("  -fx-custom-color-primary-bg: ").append(primary).append("1A;\n");
            sb.append("  -fx-custom-color-primary-hover: ").append(primary).append("33;\n");
            sb.append("  -fx-custom-color-primary-alpha40: ").append(primary).append("66;\n");
            sb.append("  -fx-custom-color-primary-alpha60: ").append(primary).append("99;\n");
            sb.append("  -fx-shadow-color-primary: ").append(primaryGlow).append(";\n");
            sb.append("  -fx-shadow-primary: dropshadow(three-pass-box, ").append(primaryGlow).append(", 40, 0.0, 0, 12);\n");
            sb.append("  -fx-shadow-primary-intense: dropshadow(three-pass-box, ").append(primary).append("CC, 20, 0.0, 0, 6);\n");
            sb.append("  -fx-grad-primary: linear-gradient(to bottom right, ").append(primaryLight).append(", ").append(primaryDark).append(");\n");
            sb.append("  -fx-grad-primary-soft: linear-gradient(to bottom right, ").append(primary).append("4D, ").append(primary).append("26);\n");
            sb.append("  -fx-grad-sidebar-active: linear-gradient(to bottom right, ").append(primaryLight).append("E6, ").append(primaryDark).append(");\n");
        } else {
            sb.append("  -fx-grad-sidebar-active: linear-gradient(to bottom right, -fx-custom-color-primary-light, -fx-custom-color-primary-dark);\n");
        }
    }

    private void appendStaticShadows(StringBuilder sb) {
        sb.append("  -fx-shadow-success: dropshadow(three-pass-box, rgba(16, 185, 129, 0.4), 15, 0, 0, 5);\n");
        sb.append("  -fx-shadow-danger: dropshadow(three-pass-box, rgba(239, 68, 68, 0.4), 15, 0, 0, 5);\n");
        sb.append("  -fx-shadow-warning: dropshadow(three-pass-box, rgba(245, 158, 11, 0.4), 15, 0, 0, 5);\n");
        sb.append("  -fx-grad-success: linear-gradient(to bottom, #10b981, #059669);\n");
        sb.append("  -fx-grad-danger: linear-gradient(to bottom, #ef4444, #dc2626);\n");
        sb.append("  -fx-grad-warning: linear-gradient(to bottom, #f59e0b, #d97706);\n");
    }

    private void appendSecondaryAndTextColors(StringBuilder sb, Map<String, String> settings, boolean isDark) {
        String secondary = settings.get("ui.secondary_color");
        if (secondary != null && !secondary.isEmpty()) {
            sb.append("  -fx-custom-color-secondary: ").append(secondary).append(";\n");
        }
        String text = settings.get("ui.text_main");
        if (text != null && (!isDark || isLightColor(text))) {
            sb.append("  -fx-text-custom-main: ").append(text).append(";\n");
            sb.append("  -fx-text-custom-medium: ").append(text).append(";\n");
            sb.append("  -fx-text-custom-light: ").append(text).append("B3;\n");
            sb.append("  -fx-text-custom-muted: ").append(text).append("80;\n");
            sb.append("  -fx-text-custom-on-primary: #ffffff;\n");
        }
        String textCards = settings.getOrDefault("ui.text_cards", text);
        if (textCards != null && (!isDark || isLightColor(textCards))) {
            sb.append("  -fx-text-custom-cards: ").append(textCards).append(";\n");
            sb.append("  -fx-text-custom-cards-muted: ").append(textCards).append("80;\n");
        }
        String textPrice = settings.getOrDefault("ui.text_price", text);
        if (textPrice != null && (!isDark || isLightColor(textPrice))) {
            sb.append("  -fx-text-custom-price: ").append(textPrice).append(";\n");
        }
    }

    private void appendSidebarColors(StringBuilder sb, Map<String, String> settings) {
        String sidebarTop = settings.getOrDefault("ui.sidebar_bg", "#0f172a");
        sb.append("  -fx-bg-sidebar: ").append(sidebarTop).append(";\n");
        String sidebarText = settings.getOrDefault("ui.sidebar_text_color", "#ffffff");
        sb.append("  -fx-custom-color-sidebar-text: ").append(sidebarText).append(";\n");
        sb.append("  -fx-custom-color-sidebar-text-a70: ").append(sidebarText).append("B3;\n");
        sb.append("  -fx-custom-color-sidebar-text-a60: ").append(sidebarText).append("99;\n");
    }

    private void appendFontAndBorderRadii(StringBuilder sb, Map<String, String> settings) {
        String fontSize = settings.get("ui.font_size");
        if (fontSize != null) {
            try {
                double base = Double.parseDouble(fontSize);
                // Variables de fuente como números puros (JavaFX prefiere esto para fuentes)
                sb.append("  -fx-font-size-base: ").append(base).append(";\n");
                sb.append("  -fx-font-size-large: ").append(base + 4.0).append(";\n");
                sb.append("  -fx-font-size-xl: ").append(base + 12.0).append(";\n");
                sb.append("  -fx-font-size-small: ").append(base - 2.0).append(";\n");
                // Propiedad global con px para asegurar consistencia
                sb.append("  -fx-font-size: ").append(base).append("px;\n");
            } catch (Exception ignored) {}
        }
        String borderRadius = settings.get("ui.border_radius");
        if (borderRadius != null) {
            try {
                double br = Double.parseDouble(borderRadius);
                // Los radios SIEMPRE con px para evitar ClassCastException
                sb.append("  -fx-radius-sm: ").append(br * 0.5).append("px;\n");
                sb.append("  -fx-radius-md: ").append(br).append("px;\n");
                sb.append("  -fx-radius-lg: ").append(br * 1.5).append("px;\n");
                sb.append("  -fx-radius-xl: ").append(br * 2.5).append("px;\n");
                sb.append("  -fx-layout-radius: ").append(br * 2.0).append("px;\n");
            } catch (Exception ignored) {}
        }
    }

    private void appendCardMetrics(StringBuilder sb, Map<String, String> settings) {
        String cardScale = settings.getOrDefault("ui.card_scale", "1.0");
        String cardShadow = settings.getOrDefault("ui.card_shadow", "15");
        double shadowSize = 15;
        try { shadowSize = Double.parseDouble(cardShadow); } catch (Exception ignored) {}

        sb.append("  -fx-card-scale: ").append(cardScale).append(";\n");
        sb.append("  -fx-card-shadow: dropshadow(three-pass-box, -fx-shadow-color-medium, ").append(shadowSize).append(", 0, 0, 3);\n");
        sb.append("  -fx-card-shadow-hover: dropshadow(three-pass-box, -fx-shadow-color-primary, ").append(shadowSize * 1.5).append(", 0, 0, 6);\n");
        sb.append("  -fx-card-border-width: ").append(settings.getOrDefault("ui.card_border_width", "1.5")).append("px;\n");
        sb.append("  -fx-card-hover-lift: -").append(settings.getOrDefault("ui.card_hover_lift", "8")).append("px;\n");
        sb.append("  -fx-card-hover-scale: ").append(settings.getOrDefault("ui.card_hover_scale", "1.02")).append(";\n");
    }

    private void appendGlobalTextOverrides(StringBuilder sb, Map<String, String> settings, boolean isDark) {
        String text = settings.get("ui.text_main");
        if (text != null && (!isDark || isLightColor(text))) {
            String themeClass = isDark ? ".dark-theme" : ".light-theme";
            sb.append(themeClass).append(", ").append(themeClass).append(" .label, ").append(themeClass).append(" .text, ")
              .append(themeClass).append(" .button, ").append(themeClass).append(" .toggle-button, ")
              .append(themeClass).append(" .text-field, ").append(themeClass).append(" .text-area, ")
              .append(themeClass).append(" .combo-box {\n");
            sb.append("  -fx-text-fill: -fx-text-custom-main;\n");
            sb.append("}\n\n");
        }
    }

    private void appendLayoutSizing(StringBuilder sb, Map<String, String> settings) {
        try {
            double scale = Double.parseDouble(settings.getOrDefault("ui.card_scale", "1.0"));
            sb.append(".products-grid {\n");
            sb.append("  -fx-tile-width: ").append((int) (200 * scale) + 15).append("px;\n");
            sb.append("  -fx-tile-height: ").append((int) (280 * scale) + 15).append("px;\n");
            sb.append("}\n\n");
            sb.append(".user-card {\n");
            sb.append("  -fx-pref-width: ").append((int) (280 * scale)).append("px;\n");
            sb.append("}\n\n");
        } catch (Exception ignored) {}
    }

    private void appendThemeMode(StringBuilder sb, Map<String, String> settings, boolean isDark, String bg) {
        if (bg != null && (!isDark || !isLightColor(bg))) {
            String themeClass = isDark ? ".dark-theme" : ".light-theme";
            sb.append(themeClass).append(" {\n");
            sb.append("  -fx-bg-main: ").append(bg).append(";\n");
            sb.append("  -fx-bg-sidebar: ").append(bg).append(";\n");
            sb.append("  -fx-grad-sidebar: ").append(bg).append(";\n");
            if (isDark) {
                sb.append("  -fx-bg-surface: ").append(brightenAbsolute(bg, 0.15)).append(";\n");
                sb.append("  -fx-bg-subtle: ").append(brightenAbsolute(bg, 0.08)).append(";\n");
            } else {
                sb.append("  -fx-bg-surface: #ffffff;\n");
            }
            sb.append("}\n\n");
        }
    }

    private void appendHardComponentOverrides(StringBuilder sb, Map<String, String> settings) {
        String borderRadius = settings.get("ui.border_radius");
        if (borderRadius != null) {
            try {
                int br = (int) Double.parseDouble(borderRadius);
                sb.append(".text-field, .combo-box { -fx-background-radius: ").append(br).append("px; -fx-border-radius: ").append(br).append("px; }\n\n");
                sb.append(".modern-btn-primary, .modern-btn-secondary { -fx-background-radius: ").append((int)(br*1.5)).append("px; -fx-border-radius: ").append((int)(br*1.5)).append("px; }\n\n");
                
                sb.append(".cart-panel, .product-box, .client-card, .user-card, .stat-card, .filter-card, .detail-panel, .view-header-icon-wrap {\n");
                sb.append("  -fx-background-radius: ").append(br * 2).append("px !important;\n");
                sb.append("  -fx-border-radius: ").append(br * 2).append("px !important;\n");
                sb.append("}\n\n");

                sb.append(".cart-customer-header { -fx-background-radius: ").append(br*2).append("px ").append(br*2).append("px 0 0 !important; }\n");
                sb.append(".checkout-summary { -fx-background-radius: 0 0 ").append(br*2).append("px ").append(br*2).append("px !important; }\n");
                sb.append(".product-image-container { -fx-background-radius: ").append(br*2).append("px ").append(br*2).append("px 0 0 !important; }\n");
                sb.append(".product-price-badge { -fx-background-radius: 0px ").append((int)(br*1.5)).append("px 0px ").append((int)(br*1.5)).append("px !important; }\n");
                sb.append(".product-add-btn { -fx-background-radius: 0 0 ").append(br*2).append("px ").append(br*2).append("px !important; }\n");
                sb.append(".alert-root, .toast-root { -fx-background-radius: ").append(br*3).append("px !important; }\n");
            } catch (Exception ignored) {}
        }
    }

    private void appendSidebarGradient(StringBuilder sb, Map<String, String> settings) {
        String top = settings.getOrDefault("ui.sidebar_bg", "#0f172a");
        String mid = darken(top, 0.35);
        String bot = darken(top, 0.65);
        sb.append(".sidebar {\n");
        sb.append("  -fx-background-color: linear-gradient(to bottom, ").append(top).append(" 0%, ").append(mid).append(" 55%, ").append(bot).append(" 100%);\n");
        sb.append("}\n");
    }

    private void appendSearchBarTints(StringBuilder sb, Map<String, String> settings, boolean isDark) {
        String primary = settings.get("ui.primary_color");
        if (primary != null) {
            String baseBg = isDark ? "#0f172a" : "#f8fafc";
            String baseBorder = isDark ? "#1e293b" : "#e2e8f0";
            sb.append(".search-bar {\n");
            sb.append("  -fx-background-color: ").append(blendColors(baseBg, primary, 0.06)).append(";\n");
            sb.append("  -fx-border-color: ").append(blendColors(baseBorder, primary, 0.20)).append(";\n");
            sb.append("}\n");
            sb.append(".search-bar:focus-within { -fx-border-color: ").append(primary).append("; }\n");
        }
    }

    // --- Color Helpers (Same as original ThemeManager) ---

    private String brightenAbsolute(String hex, double amount) {
        try {
            Color c = Color.valueOf(hex);
            return toHex(Color.hsb(c.getHue(), Math.max(0.0, c.getSaturation() - (amount * 0.4)), Math.min(1.0, c.getBrightness() + amount)));
        } catch (Exception e) { return hex; }
    }

    private String brighten(String hex, double factor) {
        try { return toHex(Color.valueOf(hex).deriveColor(0, 1.0, 1.0 + factor, 1.0)); } catch (Exception e) { return hex; }
    }

    private String darken(String hex, double factor) {
        try { return toHex(Color.valueOf(hex).deriveColor(0, 1.0, 1.0 - factor, 1.0)); } catch (Exception e) { return hex; }
    }

    private String blendColors(String base, String blend, double ratio) {
        try {
            Color c1 = Color.valueOf(base); Color c2 = Color.valueOf(blend);
            return String.format("#%02X%02X%02X", (int)((c1.getRed() + (c2.getRed()-c1.getRed())*ratio)*255), (int)((c1.getGreen() + (c2.getGreen()-c1.getGreen())*ratio)*255), (int)((c1.getBlue() + (c2.getBlue()-c1.getBlue())*ratio)*255));
        } catch (Exception e) { return base; }
    }

    private boolean isLightColor(String hex) {
        try { Color c = Color.valueOf(hex); return (c.getRed()*0.299 + c.getGreen()*0.587 + c.getBlue()*0.114) > 0.5; } catch (Exception e) { return false; }
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X", (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255));
    }
}

