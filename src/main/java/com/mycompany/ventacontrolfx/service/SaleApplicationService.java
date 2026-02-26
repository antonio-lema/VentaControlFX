package com.mycompany.ventacontrolfx.service;

import com.mycompany.ventacontrolfx.model.CartItem;
import com.mycompany.ventacontrolfx.model.Client;
import com.mycompany.ventacontrolfx.util.AlertUtil;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * High-level service that orchestrates sales operations.
 */
public class SaleApplicationService {
    private final SaleService saleService;
    private final CartService cartService;
    private final GlobalEventBus eventBus;

    public SaleApplicationService(SaleService saleService, CartService cartService, GlobalEventBus eventBus) {
        this.saleService = saleService;
        this.cartService = cartService;
        this.eventBus = eventBus;
    }

    public Integer processSale(double paid, double change, String method, ResultCallback callback) {
        List<CartItem> items = new ArrayList<>(cartService.getCartItems());
        double total = cartService.getGrandTotal();
        Client client = cartService.getSelectedClient();
        Integer clientId = client != null ? client.getId() : null;

        try {
            int saleId = saleService.saveSale(items, total, method, clientId);
            cartService.clear();

            // Notify other components that data (sales/stock/counts) has changed
            eventBus.publishDataChange();

            if (callback != null) {
                callback.onSuccess(saleId, items, total, paid, change, method, client);
            }
            return saleId;
        } catch (SQLException e) {
            AlertUtil.showError("Error al guardar venta", e.getMessage());
            return null;
        }
    }

    public interface ResultCallback {
        void onSuccess(int saleId, List<CartItem> items, double total, double paid, double change, String method,
                Client client);
    }
}
