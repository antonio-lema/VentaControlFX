package com.mycompany.ventacontrolfx.presentation.renderer;

import com.mycompany.ventacontrolfx.presentation.component.CartItemRow;
import com.mycompany.ventacontrolfx.domain.model.CartItem;
import com.mycompany.ventacontrolfx.application.usecase.CartUseCase;
import com.mycompany.ventacontrolfx.presentation.controller.dialog.EditCartItemController;
import javafx.collections.ListChangeListener;
import javafx.scene.layout.VBox;

public class CartListRenderer {
    private final com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer container;
    private final VBox containerBox;
    private final CartUseCase cartUseCase;
    private final double globalTaxRate;
    private final boolean pricesIncludeTax;

    public CartListRenderer(VBox containerBox, CartUseCase cartUseCase, double globalTaxRate, boolean pricesIncludeTax,
            com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer container) {
        this.containerBox = containerBox;
        this.cartUseCase = cartUseCase;
        this.globalTaxRate = globalTaxRate;
        this.pricesIncludeTax = pricesIncludeTax;
        this.container = container;
        renderCurrentItems();
        initListener();
    }

    public void refreshAllPrices() {
        renderCurrentItems();
    }

    private void renderCurrentItems() {
        containerBox.getChildren().clear();
        for (CartItem item : cartUseCase.getCartItems()) {
            addRow(item);
        }
    }

    private void addRow(CartItem item) {
        CartItemRow row = new CartItemRow(
                item,
                globalTaxRate,
                pricesIncludeTax,
                container.getBundle(),
                () -> {
                    try {
                        cartUseCase.incrementQuantity(item.getProduct());
                    } catch (IllegalArgumentException ex) {
                        com.mycompany.ventacontrolfx.presentation.util.AlertUtil.showWarning(container.getBundle().getString("cart.error.insufficient_stock"), ex.getMessage());
                    }
                },
                () -> cartUseCase.decrementQuantity(item.getProduct()),
                () -> cartUseCase.removeItem(item.getProduct()),
                (newQty) -> {
                    try {
                        cartUseCase.updateQuantity(item.getProduct(), newQty);
                    } catch (IllegalArgumentException ex) {
                        com.mycompany.ventacontrolfx.presentation.util.AlertUtil.showWarning(container.getBundle().getString("cart.error.insufficient_stock"), ex.getMessage());
                    }
                },
                () -> {
                    com.mycompany.ventacontrolfx.presentation.navigation.ModalService.showTransparentModal(
                            "/view/cart/edit_cart_item.fxml",
                            container.getBundle().getString("cart.edit.title"),
                            container,
                            (EditCartItemController controller) -> {
                                controller.setData(item, () -> {
                                    cartUseCase.updateTotals();
                                });
                            });
                });
        containerBox.getChildren().add(row);
    }

    private void initListener() {
        cartUseCase.getCartItems().addListener((ListChangeListener.Change<? extends CartItem> c) -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (CartItem item : c.getAddedSubList()) {
                        addRow(item);
                    }
                }
                if (c.wasRemoved()) {
                    for (CartItem item : c.getRemoved()) {
                        containerBox.getChildren().removeIf(node -> node instanceof CartItemRow
                                && ((CartItemRow) node).getCartItem().getProduct().getId() == item.getProduct()
                                        .getId());
                    }
                }
            }
        });
    }
}




