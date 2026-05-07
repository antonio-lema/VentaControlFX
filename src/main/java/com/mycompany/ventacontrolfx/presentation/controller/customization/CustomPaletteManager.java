package com.mycompany.ventacontrolfx.presentation.controller.customization;

import javafx.scene.paint.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Catálogo de paletas de colores predefinidas para la aplicación.
 */
public class CustomPaletteManager {

    public static record Palette(
        String primary, String secondary, String bg, String text, 
        String textCards, String textPrice, String sidebar, String sidebarText
    ) {}

    private final Map<String, Palette> palettes = new HashMap<>();

    public CustomPaletteManager() {
        palettes.put("CLASSIC", new Palette("#1e88e5", "#64748b", "#fafbfc", "#2c3e50", "#2c3e50", "#2c3e50", "#1e88e5", "#ffffff"));
        palettes.put("DARK", new Palette("#6366f1", "#94a3b8", "#0f172a", "#f8fafc", "#f8fafc", "#818cf8", "#0f172a", "#ffffff"));
        palettes.put("EMERALD", new Palette("#2e7d32", "#455a64", "#f1f8e9", "#1b5e20", "#1b5e20", "#10b981", "#2e7d32", "#ffffff"));
        palettes.put("PURPLE", new Palette("#6a1b9a", "#37474f", "#f3e5f5", "#4a148c", "#4a148c", "#4a148c", "#6a1b9a", "#ffffff"));
        palettes.put("AMBER", new Palette("#ef6c00", "#4e342e", "#fff8e1", "#3e2723", "#3e2723", "#e65100", "#ef6c00", "#ffffff"));
        palettes.put("ROSE", new Palette("#d81b60", "#4a148c", "#fce4ec", "#880e4f", "#880e4f", "#880e4f", "#d81b60", "#ffffff"));
        palettes.put("TEAL", new Palette("#0d9488", "#1e293b", "#f0fdfa", "#134e4a", "#134e4a", "#134e4a", "#0d9488", "#ffffff"));
    }

    public Palette getPalette(String key) {
        return palettes.getOrDefault(key, palettes.get("CLASSIC"));
    }
}

