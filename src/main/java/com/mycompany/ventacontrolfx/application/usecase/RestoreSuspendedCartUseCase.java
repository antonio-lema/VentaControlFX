package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.Client;
import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.domain.model.SuspendedCart;
import com.mycompany.ventacontrolfx.domain.model.SuspendedCartItem;
import com.mycompany.ventacontrolfx.domain.repository.IClientRepository;
import com.mycompany.ventacontrolfx.domain.repository.IProductRepository;
import com.mycompany.ventacontrolfx.domain.repository.ISuspendedCartRepository;

import java.sql.SQLException;

/**
 * Business logic for restoring a suspended cart into the active shopping cart.
 * Following Clean Architecture: This is an application layer use case.
 */
public class RestoreSuspendedCartUseCase {

    private final ISuspendedCartRepository suspendedRepo;
    private final IProductRepository productRepo;
    private final IClientRepository clientRepo;
    private final CartUseCase activeCart;

    public RestoreSuspendedCartUseCase(
            ISuspendedCartRepository suspendedRepo,
            IProductRepository productRepo,
            IClientRepository clientRepo,
            CartUseCase activeCart) {
        this.suspendedRepo = suspendedRepo;
        this.productRepo = productRepo;
        this.clientRepo = clientRepo;
        this.activeCart = activeCart;
    }

    /**
     * Restores a suspended cart.
     * Orchestrates data retrieval from multiple repos to ensure data integrity.
     */
    public void execute(int cartId) throws SQLException {
        // 1. Fetch the suspended structure (shallow)
        SuspendedCart suspended = suspendedRepo.findById(cartId);
        if (suspended == null) {
            throw new SQLException("El carrito aplazado no existe.");
        }

        // 2. Prepare/Reset the active session cart
        activeCart.clear();

        // 3. Restore Client with fresh data (ensuring address/VAT rules are up to date)
        if (suspended.getClientId() != null) {
            Client freshClient = clientRepo.getById(suspended.getClientId());
            if (freshClient != null) {
                activeCart.setSelectedClient(freshClient);
            }
        }

        // 4. Restore Items (Fetching fresh products to ensure current price/VAT)
        if (suspended.getItems() != null) {
            for (SuspendedCartItem item : suspended.getItems()) {
                Product freshProduct = productRepo.getById(item.getProduct().getId());
                if (freshProduct != null) {
                    activeCart.addItem(freshProduct, item.getQuantity());
                } else {
                    // Fail-safe: if product was deleted, we log or warn.
                    // For now, we omit it to avoid total crash.
                    System.err.println("Producto ID " + item.getProduct().getId() + " ya no existe, omitido.");
                }
            }
        }

        // 5. Cleanup: Delete the record once successfully loaded into active state
        suspendedRepo.delete(cartId);
    }
}
