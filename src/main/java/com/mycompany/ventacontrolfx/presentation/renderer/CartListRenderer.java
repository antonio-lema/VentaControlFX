package com.mycompany.ventacontrolfx.presentation.renderer;

import com.mycompany.ventacontrolfx.component.CartItemRow;
import com.mycompany.ventacontrolfx.domain.model.CartItem;
import com.mycompany.ventacontrolfx.application.usecase.CartUseCase;
import javafx.collections.ListChangeListener;
import javafx.scene.layout.VBox;

public class CartListRenderer {
    private final VBox container;
    private final CartUseCase cartUseCase;

    public CartListRenderer(VBox container, CartUseCase cartUseCase) {
        this.container = container;
        this.cartUseCase = cartUseCase;
        renderCurrentItems();
        initListener();
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
                () -> cartUseCase.incrementQuantity(item.getProduct()),
                () -> cartUseCase.decrementQuantity(item.getProduct()),
                () -> cartUseCase.removeItem(item.getProduct()),
                (newQty) -> cartUseCase.updateQuantity(item.getProduct(), newQty));
        container.getChildren().add(row);
    }

    private void initListener() {
        cartUseCase.getCartItems().addListener((ListChangeListener.Change<? extends CartItem> c) -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (CartItem item : c.getAddedSubList()) {
                        CartItemRow row = new CartItemRow(
                                item,
                                () -> cartUseCase.incrementQuantity(item.getProduct()),
                                () -> cartUseCase.decrementQuantity(item.getProduct()),
                                () -> cartUseCase.removeItem(item.getProduct()),
                                (newQty) -> cartUseCase.updateQuantity(item.getProduct(), newQty));
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
