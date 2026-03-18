package com.mycompany.ventacontrolfx.presentation.renderer;

import com.mycompany.ventacontrolfx.component.CartItemRow;
import com.mycompany.ventacontrolfx.domain.model.CartItem;
import com.mycompany.ventacontrolfx.application.usecase.CartUseCase;
import javafx.collections.ListChangeListener;
import javafx.scene.layout.VBox;

public class CartListRenderer {
    private final VBox container;
    private final CartUseCase cartUseCase;
    private final double globalTaxRate;
    private final boolean pricesIncludeTax;

    public CartListRenderer(VBox container, CartUseCase cartUseCase, double globalTaxRate, boolean pricesIncludeTax) {
        this.container = container;
        this.cartUseCase = cartUseCase;
        this.globalTaxRate = globalTaxRate;
        this.pricesIncludeTax = pricesIncludeTax;
        renderCurrentItems();
        initListener();
    }

    public void refreshAllPrices() {
        renderCurrentItems();
    }

    private void renderCurrentItems() {
        container.getChildren().clear();
        for (CartItem item : cartUseCase.getCartItems()) {
            addRow(item);
        }
    }

    private void addRow(CartItem item) {
        CartItemRow row = new CartItemRow(
                item,
                globalTaxRate,
                pricesIncludeTax,
                () -> {
                    try {
                        cartUseCase.incrementQuantity(item.getProduct());
                    } catch (IllegalArgumentException ex) {
                        com.mycompany.ventacontrolfx.util.AlertUtil.showWarning("Stock Insuficiente", ex.getMessage());
                    }
                },
                () -> cartUseCase.decrementQuantity(item.getProduct()),
                () -> cartUseCase.removeItem(item.getProduct()),
                (newQty) -> {
                    try {
                        cartUseCase.updateQuantity(item.getProduct(), newQty);
                    } catch (IllegalArgumentException ex) {
                        com.mycompany.ventacontrolfx.util.AlertUtil.showWarning("Stock Insuficiente", ex.getMessage());
                    }
                });
        container.getChildren().add(row);
    }

    private void initListener() {
        cartUseCase.getCartItems().addListener((ListChangeListener.Change<? extends CartItem> c) -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (CartItem item : c.getAddedSubList()) {
                        CartItemRow row = new CartItemRow(
                                item,
                                globalTaxRate,
                                pricesIncludeTax,
                                () -> {
                                    try {
                                        cartUseCase.incrementQuantity(item.getProduct());
                                    } catch (IllegalArgumentException ex) {
                                        com.mycompany.ventacontrolfx.util.AlertUtil.showWarning("Stock Insuficiente",
                                                ex.getMessage());
                                    }
                                },
                                () -> cartUseCase.decrementQuantity(item.getProduct()),
                                () -> cartUseCase.removeItem(item.getProduct()),
                                (newQty) -> {
                                    try {
                                        cartUseCase.updateQuantity(item.getProduct(), newQty);
                                    } catch (IllegalArgumentException ex) {
                                        com.mycompany.ventacontrolfx.util.AlertUtil.showWarning("Stock Insuficiente",
                                                ex.getMessage());
                                    }
                                });
                        container.getChildren().add(row);
                    }
                }
                if (c.wasRemoved()) {
                    for (CartItem item : c.getRemoved()) {
                        container.getChildren().removeIf(node -> node instanceof CartItemRow
                                && ((CartItemRow) node).getCartItem().getProduct().getId() == item.getProduct()
                                        .getId());
                    }
                }
            }
        });
    }
}
