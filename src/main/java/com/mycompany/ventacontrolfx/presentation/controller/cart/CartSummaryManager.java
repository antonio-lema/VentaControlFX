package com.mycompany.ventacontrolfx.presentation.controller.cart;

import com.mycompany.ventacontrolfx.application.usecase.CartUseCase;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

/**
 * Gestor del resumen financiero del carrito (Totales, Impuestos, Ahorros).
 * Maneja los bindings automáticos entre el dominio y la UI.
 */
public class CartSummaryManager {

    private final ServiceContainer container;
    private final CartUseCase cartUseCase;

    public CartSummaryManager(ServiceContainer container, CartUseCase cartUseCase) {
        this.container = container;
        this.cartUseCase = cartUseCase;
    }

    public void bind(Label subtotal, Label tax, Label savings, Label itemsCount, Label totalButton, HBox savingsBox, TextField promoCode) {
        subtotal.textProperty().bind(Bindings.createStringBinding(
                () -> String.format(container.getBundle().getString("cart.summary.subtotal_format"), cartUseCase.getSubtotal()),
                cartUseCase.subtotalProperty()));

        tax.textProperty().bind(Bindings.createStringBinding(
                () -> String.format(container.getBundle().getString("cart.summary.tax_format"), cartUseCase.getTax()),
                cartUseCase.taxProperty()));

        savings.textProperty().bind(Bindings.createStringBinding(
                () -> String.format(container.getBundle().getString("cart.summary.savings_format"), cartUseCase.getTotalSavings()),
                cartUseCase.totalSavingsProperty()));

        savingsBox.visibleProperty().bind(cartUseCase.totalSavingsProperty().greaterThan(0));
        savingsBox.managedProperty().bind(savingsBox.visibleProperty());

        totalButton.textProperty().bind(Bindings.createStringBinding(
                () -> String.format("%.2f \u20ac", cartUseCase.getGrandTotal()),
                cartUseCase.grandTotalProperty()));

        itemsCount.textProperty().bind(Bindings.createStringBinding(
                () -> String.format(container.getBundle().getString("cart.summary.items_count_format"),
                        cartUseCase.getItemCount(),
                        cartUseCase.getItemCount() != 1 
                            ? container.getBundle().getString("cart.summary.items_suffix_plural") 
                            : container.getBundle().getString("cart.summary.items_suffix_singular")),
                cartUseCase.itemCountProperty()));

        promoCode.textProperty().bindBidirectional(cartUseCase.appliedPromoCodeProperty());
    }
}

