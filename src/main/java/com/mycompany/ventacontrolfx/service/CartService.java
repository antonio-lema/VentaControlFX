package com.mycompany.ventacontrolfx.service;

import com.mycompany.ventacontrolfx.model.CartItem;
import com.mycompany.ventacontrolfx.model.Product;
import java.util.Optional;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ListChangeListener;
import java.util.ArrayList;
import java.util.List;

public class CartService {

    private final ObservableList<CartItem> cartItems;
    private final DoubleProperty subtotal = new SimpleDoubleProperty(0.0);
    private final DoubleProperty tax = new SimpleDoubleProperty(0.0);
    private final DoubleProperty grandTotal = new SimpleDoubleProperty(0.0);
    private final IntegerProperty itemCount = new SimpleIntegerProperty(0);

    private static final double TAX_RATE = 0.21;

    public CartService() {
        this.cartItems = FXCollections
                .observableArrayList((CartItem item) -> new Observable[] { item.quantityProperty() });
        this.cartItems.addListener((ListChangeListener.Change<? extends CartItem> c) -> updateTotals());
    }

    private void updateTotals() {
        double totalInclusive = cartItems.stream().mapToDouble(CartItem::getTotal).sum();
        double baseAmount = totalInclusive / (1 + TAX_RATE);
        double taxAmount = totalInclusive - baseAmount;

        subtotal.set(baseAmount);
        tax.set(taxAmount);
        grandTotal.set(totalInclusive);

        itemCount.set(cartItems.stream().mapToInt(CartItem::getQuantity).sum());
    }

    public DoubleProperty subtotalProperty() {
        return subtotal;
    }

    public DoubleProperty taxProperty() {
        return tax;
    }

    public DoubleProperty grandTotalProperty() {
        return grandTotal;
    }

    public IntegerProperty itemCountProperty() {
        return itemCount;
    }

    public ObservableList<CartItem> getCartItems() {
        return cartItems;
    }

    public void addItem(Product product) {
        Optional<CartItem> existingItem = cartItems.stream()
                .filter(item -> item.getProduct().getId() == product.getId())
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + 1);
        } else {
            cartItems.add(new CartItem(product, 1));
        }
    }

    public void removeItem(Product product) {
        cartItems.removeIf(item -> item.getProduct().getId() == product.getId());
    }

    public void incrementQuantity(Product product) {
        addItem(product);
    }

    public void decrementQuantity(Product product) {
        Optional<CartItem> existingItem = cartItems.stream()
                .filter(item -> item.getProduct().getId() == product.getId())
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            if (item.getQuantity() > 1) {
                item.setQuantity(item.getQuantity() - 1);
            } else {
                // Should we remove if 0? Usually yes, or keep at 1.
                // Implementation implies min 1.
            }
        }
    }

    public double getSubtotal() {
        return subtotal.get();
    }

    public double getTax() {
        return tax.get();
    }

    public double getGrandTotal() {
        return grandTotal.get();
    }

    public int getItemCount() {
        return cartItems.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }

    public void setQuantity(Product product, int quantity) {
        if (quantity < 1) {
            return;
        }
        Optional<CartItem> existingItem = cartItems.stream()
                .filter(item -> item.getProduct().getId() == product.getId())
                .findFirst();

        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(quantity);
        } else {
            cartItems.add(new CartItem(product, quantity));
        }
    }

    public void clear() {
        cartItems.clear();
    }
}
