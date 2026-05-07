package com.mycompany.ventacontrolfx.presentation.controller.sidebar;

import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.io.File;

/**
 * Gestor de identidad visual y branding para el Sidebar.
 */
public class SidebarBrandingManager {

    private final ServiceContainer container;

    public SidebarBrandingManager(ServiceContainer container) {
        this.container = container;
    }

    public void applyBranding(Label lblAppName, ImageView brandLogo) {
        try {
            SaleConfig cfg = container.getICompanyConfigRepository().load();
            
            // 1. Nombre de la App
            String name = cfg.getAppName();
            if (name == null || name.isBlank()) name = cfg.getCompanyName();
            if (name == null || name.isBlank()) name = container.getBundle().getString("app.name.default");
            lblAppName.setText(name);

            // 2. Logo
            String logoPath = cfg.getLogoPath();
            if (logoPath == null || logoPath.isBlank()) logoPath = cfg.getAppIconPath();
            
            if (logoPath != null && !logoPath.isBlank()) {
                File f = new File(logoPath);
                if (f.exists()) {
                    brandLogo.setImage(new Image(f.toURI().toString()));
                    brandLogo.setVisible(true);
                    brandLogo.setManaged(true);
                }
            }
        } catch (Exception e) {
            lblAppName.setText(container.getBundle().getString("app.name.default"));
        }
    }
}

