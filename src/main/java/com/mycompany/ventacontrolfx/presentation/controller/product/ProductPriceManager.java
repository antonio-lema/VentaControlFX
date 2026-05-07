package com.mycompany.ventacontrolfx.presentation.controller.product;

import com.mycompany.ventacontrolfx.domain.model.Price;
import com.mycompany.ventacontrolfx.domain.model.PriceList;
import com.mycompany.ventacontrolfx.application.usecase.PriceUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Gestor de la sección de precios y tarifas para el formulario de productos.
 * Maneja la creación dinámica de inputs y el cálculo de diferencias en tiempo real.
 */
public class ProductPriceManager {

    private final ServiceContainer container;
    private final PriceUseCase priceUseCase;
    private final VBox containerVBox;
    private final Map<Integer, TextField> priceFields = new HashMap<>();
    private final Map<Integer, Label> diffLabels = new HashMap<>();
    private PriceList defaultPriceList;

    public ProductPriceManager(ServiceContainer container, VBox containerVBox) {
        this.container = container;
        this.priceUseCase = container.getPriceUseCase();
        this.containerVBox = containerVBox;
    }

    public void renderPriceLists(List<PriceList> lists) {
        containerVBox.getChildren().clear();
        priceFields.clear();
        diffLabels.clear();

        for (PriceList pl : lists) {
            if (pl.isDefault()) defaultPriceList = pl;
            
            HBox row = createPriceRow(pl);
            containerVBox.getChildren().add(row);
        }

        setupListeners();
    }

    private HBox createPriceRow(PriceList pl) {
        HBox row = new HBox();
        row.getStyleClass().add("modern-input-container");
        row.setAlignment(Pos.CENTER_LEFT);

        FontAwesomeIconView icon = new FontAwesomeIconView(de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon.MONEY);
        icon.setSize("18"); icon.getStyleClass().add("sidebar-icon");
        HBox.setMargin(icon, new Insets(0, 10, 0, 0));

        TextField txt = new TextField();
        txt.setPromptText("0.00");
        txt.getStyleClass().add("modern-input-field");
        HBox.setHgrow(txt, Priority.ALWAYS);

        String suffix = container.getBundle().getString("product.price_list.base_suffix");
        Label lblName = new Label(pl.getName() + (pl.isDefault() ? " (" + suffix + "):" : ":"));
        lblName.setMinWidth(140);
        lblName.setStyle("-fx-font-weight: bold; -fx-text-fill: #64748b; -fx-font-size: 13px;");
        HBox.setMargin(lblName, new Insets(0, 10, 0, 0));

        Label lblDiff = new Label("");
        lblDiff.setMinWidth(65); lblDiff.setAlignment(Pos.CENTER_RIGHT);
        lblDiff.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
        HBox.setMargin(lblDiff, new Insets(0, 0, 0, 10));

        row.getChildren().addAll(icon, lblName, txt, lblDiff);
        priceFields.put(pl.getId(), txt);
        if (!pl.isDefault()) diffLabels.put(pl.getId(), lblDiff);
        
        return row;
    }

    private void setupListeners() {
        if (defaultPriceList == null || !priceFields.containsKey(defaultPriceList.getId())) return;
        
        TextField defaultField = priceFields.get(defaultPriceList.getId());
        defaultField.textProperty().addListener((obs, old, nv) -> updateAllDiffs());

        for (Map.Entry<Integer, TextField> entry : priceFields.entrySet()) {
            if (entry.getKey() != defaultPriceList.getId()) {
                entry.getValue().textProperty().addListener((obs, old, nv) -> updateDiff(entry.getKey()));
            }
        }
    }

    public void updateAllDiffs() {
        diffLabels.keySet().forEach(this::updateDiff);
    }

    private void updateDiff(int priceListId) {
        try {
            double base = Double.parseDouble(priceFields.get(defaultPriceList.getId()).getText().replace(",", "."));
            double current = Double.parseDouble(priceFields.get(priceListId).getText().replace(",", "."));
            Label lbl = diffLabels.get(priceListId);

            if (base > 0) {
                double diff = ((current - base) / base) * 100.0;
                if (Math.abs(diff) < 0.01) {
                    lbl.setText(container.getBundle().getString("product.price_diff.equal") + " =");
                    lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #94a3b8;");
                } else if (diff > 0) {
                    lbl.setText(String.format("+%.1f%%", diff));
                    lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #22c55e;");
                } else {
                    lbl.setText(String.format("%.1f%%", diff));
                    lbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #ef4444;");
                }
            } else { lbl.setText(""); }
        } catch (Exception e) { if (diffLabels.get(priceListId) != null) diffLabels.get(priceListId).setText(""); }
    }

    public void loadProductPrices(int productId, double fallbackBasePrice) {
        priceFields.forEach((id, field) -> {
            try {
                Optional<Price> active = priceUseCase.getActivePrice(productId, id);
                if (active.isPresent()) field.setText(String.valueOf(active.get().getValue()));
                else if (defaultPriceList != null && id == defaultPriceList.getId()) field.setText(String.valueOf(fallbackBasePrice));
            } catch (Exception e) {}
        });
        updateAllDiffs();
    }

    public Map<Integer, TextField> getPriceFields() { return priceFields; }
    public PriceList getDefaultPriceList() { return defaultPriceList; }
}

