package com.mycompany.ventacontrolfx.presentation.controller.customization;

import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import java.sql.SQLException;
import java.util.Map;

public class CustomizationController implements Injectable {

    @FXML private ColorPicker cpPrimary, cpSecondary, cpBackground, cpText, cpTextCards, cpTextPrice, cpSidebar, cpSidebarText;
    @FXML private Slider sldFontSize, sldBorderRadius, sldCardScale, sldCardShadow, sldCardHoverLift;
    @FXML private Label lblFontSizeVal, lblBorderRadiusVal, lblCardScaleVal, lblCardShadowVal, lblCardHoverLiftVal;
    
    @FXML private VBox paneColors, paneEffects, paneInteraction;
    @FXML private javafx.scene.control.Button btnTabColors, btnTabEffects, btnTabInteraction;

    private ServiceContainer container;
    private CustomThemeManager themeManager;
    private CustomPaletteManager paletteManager;
    private CustomUIManager uiManager;
    private boolean isDarkMode = false;

    @FXML
    public void initialize() {
        this.paletteManager = new CustomPaletteManager();
        this.uiManager = new CustomUIManager();
        setupSliderLabels();
        setupListeners();
        showColors(); // Default tab
    }

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.themeManager = new CustomThemeManager(container.getAppSettingsRepository(), container.getThemeManager());
        loadCurrentSettings();
    }

    private void setupSliderLabels() {
        uiManager.bindSliderLabel(sldFontSize, lblFontSizeVal, v -> (int) v.doubleValue() + "px");
        uiManager.bindSliderLabel(sldBorderRadius, lblBorderRadiusVal, v -> (int) v.doubleValue() + "px");
        uiManager.bindSliderLabel(sldCardScale, lblCardScaleVal, v -> String.format("%.2f\u00d7", v));
        uiManager.bindSliderLabel(sldCardShadow, lblCardShadowVal, v -> (int) v.doubleValue() + "");
        uiManager.bindSliderLabel(sldCardHoverLift, lblCardHoverLiftVal, v -> (int) v.doubleValue() + "px");
    }

    private void setupListeners() {
        ColorPicker[] pickers = {cpPrimary, cpSecondary, cpBackground, cpText, cpTextCards, cpTextPrice, cpSidebar, cpSidebarText};
        for (ColorPicker cp : pickers) if (cp != null) cp.valueProperty().addListener((obs, o, n) -> updatePreview());
        
        Slider[] sliders = {sldFontSize, sldBorderRadius, sldCardScale, sldCardShadow, sldCardHoverLift};
        for (Slider s : sliders) if (s != null) s.valueProperty().addListener((obs, o, n) -> updatePreview());
    }

    private void loadCurrentSettings() {
        try {
            Map<String, String> s = themeManager.loadAll();
            uiManager.trySetColor(cpPrimary, s.get("ui.primary_color"));
            uiManager.trySetColor(cpSecondary, s.get("ui.secondary_color"));
            uiManager.trySetColor(cpBackground, s.get("ui.bg_main"));
            uiManager.trySetColor(cpText, s.get("ui.text_main"));
            uiManager.trySetColor(cpTextCards, s.getOrDefault("ui.text_cards", s.get("ui.text_main")));
            uiManager.trySetColor(cpTextPrice, s.getOrDefault("ui.text_price", s.get("ui.text_cards")));
            uiManager.trySetColor(cpSidebar, s.getOrDefault("ui.sidebar_bg", "#0f172a"));
            uiManager.trySetColor(cpSidebarText, s.getOrDefault("ui.sidebar_text_color", "#ffffff"));

            uiManager.trySetSlider(sldFontSize, s.getOrDefault("ui.font_size", "14"));
            uiManager.trySetSlider(sldBorderRadius, s.getOrDefault("ui.border_radius", "8"));
            uiManager.trySetSlider(sldCardScale, s.getOrDefault("ui.card_scale", "1.0"));
            uiManager.trySetSlider(sldCardShadow, s.getOrDefault("ui.card_shadow", "15"));
            uiManager.trySetSlider(sldCardHoverLift, s.getOrDefault("ui.card_hover_lift", "5"));
            isDarkMode = "DARK".equals(s.getOrDefault("ui.theme_mode", "LIGHT"));
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void updatePreview() {
        if (container == null) return;
        
        // Usar cualquier componente que no sea nulo para obtener la escena
        Scene scene = null;
        if (cpPrimary != null && cpPrimary.getScene() != null) scene = cpPrimary.getScene();
        else if (btnTabColors != null && btnTabColors.getScene() != null) scene = btnTabColors.getScene();
        
        if (scene != null) {
            container.getThemeManager().applyTheme(scene, buildMap());
        }
    }

    private Map<String, String> buildMap() {
        // Asegurar que no enviamos nulos al themeManager
        return themeManager.buildSettingsMap(
            cpPrimary, cpSecondary, cpBackground, cpText, cpTextCards, cpTextPrice, cpSidebar, cpSidebarText, 
            sldFontSize, sldBorderRadius, sldCardScale, sldCardShadow, sldCardHoverLift, 
            isDarkMode
        );
    }

    @FXML private void handleSave() {
        try {
            Map<String, String> settings = buildMap();
            themeManager.save(settings);
            if (cpPrimary.getScene() != null) container.getThemeManager().applyTheme(cpPrimary.getScene());
            AlertUtil.showInfo("\u2705 Guardado", "La configuraci\u00f3n est\u00e9tica se ha guardado correctamente.");
        } catch (SQLException e) { AlertUtil.showError("Error", e.getMessage()); }
    }

    @FXML private void handleReset() { applyPalette(paletteManager.getPalette("CLASSIC")); }
    @FXML private void applyPremiumDark() { applyPalette(paletteManager.getPalette("DARK")); }
    @FXML private void applyClassicBlue() { applyPalette(paletteManager.getPalette("CLASSIC")); }
    @FXML private void applyEmeraldGreen() { applyPalette(paletteManager.getPalette("EMERALD")); }
    @FXML private void applyMidnightPurple() { applyPalette(paletteManager.getPalette("PURPLE")); }
    @FXML private void applySunsetAmber() { applyPalette(paletteManager.getPalette("AMBER")); }
    @FXML private void applySoftRose() { applyPalette(paletteManager.getPalette("ROSE")); }
    @FXML private void applyOceanTeal() { applyPalette(paletteManager.getPalette("TEAL")); }

    @FXML
    private void showColors() {
        switchTab(paneColors, btnTabColors);
    }

    @FXML
    private void showEffects() {
        switchTab(paneEffects, btnTabEffects);
    }

    @FXML
    private void showInteraction() {
        switchTab(paneInteraction, btnTabInteraction);
    }

    private void switchTab(VBox activePane, javafx.scene.control.Button activeBtn) {
        if (paneColors == null) return;
        
        // Hide all
        paneColors.setVisible(false); paneColors.setManaged(false);
        paneEffects.setVisible(false); paneEffects.setManaged(false);
        paneInteraction.setVisible(false); paneInteraction.setManaged(false);
        
        // Show active
        activePane.setVisible(true); activePane.setManaged(true);
        
        // Reset styles (removing both old and new possible class names)
        btnTabColors.getStyleClass().removeAll("active-tab", "estetica-tab-btn-active");
        btnTabEffects.getStyleClass().removeAll("active-tab", "estetica-tab-btn-active");
        btnTabInteraction.getStyleClass().removeAll("active-tab", "estetica-tab-btn-active");
        
        // Set active style
        activeBtn.getStyleClass().add("estetica-tab-btn-active");
    }

    private void applyPalette(CustomPaletteManager.Palette p) {
        uiManager.trySetColor(cpPrimary, p.primary()); uiManager.trySetColor(cpSecondary, p.secondary());
        uiManager.trySetColor(cpBackground, p.bg()); uiManager.trySetColor(cpText, p.text());
        uiManager.trySetColor(cpTextCards, p.textCards()); uiManager.trySetColor(cpTextPrice, p.textPrice());
        uiManager.trySetColor(cpSidebar, p.sidebar()); uiManager.trySetColor(cpSidebarText, p.sidebarText());
        updatePreview();
        uiManager.showThemePreviewPopup(p.primary(), p.bg(), p.text(), cpPrimary);
    }
}

