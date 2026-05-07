package com.mycompany.ventacontrolfx.presentation.controller.cart;

import com.mycompany.ventacontrolfx.application.usecase.CartUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import com.mycompany.ventacontrolfx.presentation.navigation.ModalService;
import java.text.MessageFormat;
import java.util.ArrayList;

/**
 * Gestor de suspensión y restauración de carritos (Ventas aparcadas).
 */
public class CartSuspensionManager {

    private final ServiceContainer container;
    private final CartUseCase cartUseCase;
    private final CartUIManager uiManager;

    public CartSuspensionManager(ServiceContainer container, CartUseCase cartUseCase, CartUIManager uiManager) {
        this.container = container;
        this.cartUseCase = cartUseCase;
        this.uiManager = uiManager;
    }

    public void suspendCart() {
        if (!container.getUserSession().hasPermission("venta.aplazar")) {
            AlertUtil.showError(container.getBundle().getString("cart.suspend.denied.title"), container.getBundle().getString("cart.suspend.denied.msg"));
            return;
        }
        if (cartUseCase.getItemCount() == 0) return;

        uiManager.showSuspendDialog(alias -> {
            try {
                container.getSuspendedCartUseCase().suspendCart(
                        alias, 
                        new ArrayList<>(cartUseCase.getCartItems()),
                        cartUseCase.getSelectedClient(), 
                        container.getUserSession().getCurrentUser().getUserId(), 
                        cartUseCase.getGrandTotal()
                );
                cartUseCase.clear();
                AlertUtil.showToast(MessageFormat.format(container.getBundle().getString("cart.suspend.success_toast"), alias));
            } catch (Exception ex) {
                AlertUtil.showError(container.getBundle().getString("alert.error"), container.getBundle().getString("error.save"));
            }
        });
    }

    public void showSuspendedCarts() {
        ModalService.showTransparentModal("/view/dialog/suspended_carts_dialog.fxml", container.getBundle().getString("cart.suspended.title"), container,
                (SuspendedCartsDialogController controller) -> {
                    if (controller == null) return;
                    controller.setOnCartSelected(suspendedCart -> {
                        try {
                            container.getRestoreSuspendedCartUseCase().execute(suspendedCart.getId());
                        } catch (Exception e) {
                            AlertUtil.showError(container.getBundle().getString("alert.error"), e.getMessage());
                        }
                    });
                });
    }
}



