package com.mycompany.ventacontrolfx.presentation.controller.receipt;

import com.mycompany.ventacontrolfx.domain.model.CartItem;
import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Gestor de impuestos y cálculos de ahorro para el recibo.
 */
public class ReceiptTaxManager {

    private final ServiceContainer container;

    public ReceiptTaxManager(ServiceContainer container) {
        this.container = container;
    }

    public double calculateTotalSavings(List<CartItem> items, SaleConfig cfg) {
        double savings = 0;
        for (CartItem item : items) {
            double disc = item.getDiscountAmount();
            if (!cfg.isPricesIncludeTax()) {
                double rate = item.getProduct().resolveEffectiveIva(cfg.getTaxRate());
                disc *= (1 + (rate / 100.0));
            }
            savings += disc;
        }
        return savings;
    }

    public void renderVatBreakdown(VBox containerVat, List<CartItem> items, SaleConfig cfg, String fmt) {
        containerVat.getChildren().clear();
        Map<Double, Double[]> vatMap = new TreeMap<>(); // Rate -> [Subtotal, Vat]

        for (CartItem item : items) {
            double lineTotal = item.getTotal();
            double rate = item.getProduct().resolveEffectiveIva(cfg.getTaxRate());
            double subtotal, vat;

            if (cfg.isPricesIncludeTax()) {
                subtotal = lineTotal / (1.0 + (rate / 100.0));
                vat = lineTotal - subtotal;
            } else {
                subtotal = lineTotal;
                vat = lineTotal * (rate / 100.0);
            }

            Double[] vals = vatMap.getOrDefault(rate, new Double[] { 0.0, 0.0 });
            vals[0] += subtotal; vals[1] += vat;
            vatMap.put(rate, vals);
        }

        for (Map.Entry<Double, Double[]> entry : vatMap.entrySet()) {
            HBox row = new HBox();
            Label lblRate = new Label(container.getBundle().getString("receipt.vat_label") + String.format(" %.0f%%", entry.getKey()));
            lblRate.setMaxWidth(Double.MAX_VALUE); HBox.setHgrow(lblRate, Priority.ALWAYS);
            lblRate.setStyle("-fx-font-size: 10; -fx-text-fill: black;");

            Label lblAmount = new Label(String.format(fmt, entry.getValue()[1]));
            lblAmount.setStyle("-fx-font-size: 10; -fx-text-fill: black;");

            row.getChildren().addAll(lblRate, lblAmount);
            containerVat.getChildren().add(row);
        }
    }
}

