package com.mycompany.ventacontrolfx.controller;

import com.mycompany.ventacontrolfx.component.CartItemRow;
import com.mycompany.ventacontrolfx.model.CartItem;
import com.mycompany.ventacontrolfx.service.CartService;
import javafx.collections.ListChangeListener;
import javafx.scene.layout.VBox;

public class CartListRenderer {
    private final VBox container;
    private final CartService cartService;

    public CartListRenderer(VBox container, CartService cartService) {
        this.container = container;
        this.cartService = cartService;
        initListener();
    }

    private void initListener() {
        cartService.getCartItems().addListener((ListChangeListener.Change<? extends CartItem> c) -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (CartItem item : c.getAddedSubList()) {
                        CartItemRow row = new CartItemRow(
                                item,
                                () -> cartService.incrementQuantity(item.getProduct()),
                                () -> cartService.decrementQuantity(item.getProduct()),
                                () -> cartService.removeItem(item.getProduct()),
                                (newQty) -> cartService.setQuantity(item.getProduct(), newQty));
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
