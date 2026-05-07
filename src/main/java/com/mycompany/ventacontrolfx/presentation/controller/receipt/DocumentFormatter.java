package com.mycompany.ventacontrolfx.presentation.controller.receipt;

import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import java.io.File;

public class DocumentFormatter {
    private final ServiceContainer container;
    private final SaleConfig cfg;

    public DocumentFormatter(ServiceContainer container, SaleConfig cfg) {
        this.container = container;
        this.cfg = cfg;
    }

    public void applyCompanyHeader(Label brand, Label name, Label address, Label phone, Label cif, ImageView logo, Label icon) {
        if (cfg == null) return;
        brand.setText(cfg.getCompanyName());
        if (name != null) name.setText(cfg.getCompanyName());
        if (address != null) address.setText(cfg.getAddress());
        if (phone != null) phone.setText(cfg.getPhone());
        if (cif != null) cif.setText(cfg.getCif());
        
        if (cfg.getLogoPath() != null && logo != null) {
            File f = new File(cfg.getLogoPath());
            if (f.exists()) {
                logo.setImage(new Image(f.toURI().toString()));
                logo.setVisible(true); logo.setManaged(true);
                if (icon != null) { icon.setVisible(false); icon.setManaged(false); }
            }
        }
    }

    public void applyPaperLayout(VBox sheet, boolean isClient) {
        double width = (cfg != null && cfg.getTicketFormat().contains("A4")) || isClient ? 700 : 300;
        sheet.setPrefWidth(width);
        sheet.setMinWidth(width);
        sheet.setMaxWidth(width);
    }
}

