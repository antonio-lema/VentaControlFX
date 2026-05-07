package com.mycompany.ventacontrolfx.presentation.controller.customization;

import com.mycompany.ventacontrolfx.domain.repository.IAppSettingsRepository;
import com.mycompany.ventacontrolfx.presentation.theme.ThemeManager;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Slider;
import javafx.scene.paint.Color;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestor de persistencia y aplicación de temas visuales.
 */
public class CustomThemeManager {

    private final IAppSettingsRepository repository;
    private final ThemeManager themeManager;

    public CustomThemeManager(IAppSettingsRepository repository, ThemeManager themeManager) {
        this.repository = repository;
        this.themeManager = themeManager;
    }

    public Map<String, String> buildSettingsMap(
            ColorPicker cpPrimary, ColorPicker cpSecondary, ColorPicker cpBackground, 
            ColorPicker cpText, ColorPicker cpTextCards, ColorPicker cpTextPrice,
            ColorPicker cpSidebar, ColorPicker cpSidebarText,
            Slider sldFontSize, Slider sldBorderRadius, Slider sldCardScale, 
            Slider sldCardShadow, Slider sldCardHoverLift, boolean isDarkMode) {

        Map<String, String> t = new HashMap<>();
        t.put("ui.primary_color", toHex(cpPrimary.getValue()));
        t.put("ui.secondary_color", toHex(cpSecondary.getValue()));
        t.put("ui.bg_main", toHex(cpBackground.getValue()));
        t.put("ui.text_main", toHex(cpText.getValue()));
        t.put("ui.text_cards", toHex(cpTextCards.getValue()));
        t.put("ui.text_price", toHex(cpTextPrice.getValue()));
        t.put("ui.sidebar_bg", toHex(cpSidebar.getValue()));
        t.put("ui.sidebar_text_color", toHex(cpSidebarText.getValue()));
        t.put("ui.font_size", String.valueOf((int) sldFontSize.getValue()));
        t.put("ui.border_radius", String.valueOf((int) sldBorderRadius.getValue()));
        t.put("ui.card_scale", String.valueOf(sldCardScale.getValue()));
        t.put("ui.card_shadow", String.valueOf((int) sldCardShadow.getValue()));
        t.put("ui.card_hover_lift", String.valueOf((int) sldCardHoverLift.getValue()));
        t.put("ui.theme_mode", isDarkMode ? "DARK" : "LIGHT");
        return t;
    }

    public void save(Map<String, String> settings) throws SQLException {
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            repository.saveSetting(entry.getKey(), entry.getValue());
        }
    }

    public Map<String, String> loadAll() throws SQLException {
        return repository.getAllSettings();
    }

    public String toHex(Color c) {
        return String.format("#%02X%02X%02X", (int)(c.getRed() * 255), (int)(c.getGreen() * 255), (int)(c.getBlue() * 255));
    }
}

