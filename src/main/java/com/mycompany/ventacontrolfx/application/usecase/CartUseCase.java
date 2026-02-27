package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.CartItem;
import com.mycompany.ventacontrolfx.domain.model.Client;
import com.mycompany.ventacontrolfx.domain.model.Product;
import com.mycompany.ventacontrolfx.domain.model.SaleConfig;
import com.mycompany.ventacontrolfx.domain.repository.ICompanyConfigRepository;
import javafx.beans.Observable;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.util.Optional;

/**
 * Use case for managing the shopping cart.
 * It encapsulates the cart state and business logic (like tax calculation based
 * on config).
 */
public class CartUseCase {

    private final ObservableList<CartItem> cartItems;
    private final DoubleProperty subtotal = new SimpleDoubleProperty(0.0);
    private final DoubleProperty tax = new SimpleDoubleProperty(0.0);
    private final DoubleProperty grandTotal = new SimpleDoubleProperty(0.0);
    private final IntegerProperty itemCount = new SimpleIntegerProperty(0);
    private final ObjectProperty<Client> selectedClient = new SimpleObjectProperty<>(null);

    private final ICompanyConfigRepository configRepository;

    public CartUseCase(ICompanyConfigRepository configRepository) {
        this.configRepository = configRepository;
        this.cartItems = FXCollections
                .observableArrayList((CartItem item) -> new Observable[] { item.quantityProperty() });
        this.cartItems.addListener((ListChangeListener.Change<? extends CartItem> c) -> updateTotals());
    }

    private void updateTotals() {
        SaleConfig config = configRepository.load();
        double totalInclusive = cartItems.stream().mapToDouble(CartItem::getTotal).sum();

        double taxDivisor = config.getTaxDivisor();
        double baseAmount = totalInclusive / taxDivisor;
        double taxAmount = totalInclusive - baseAmount;

        subtotal.set(baseAmount);
        tax.set(taxAmount);
        grandTotal.set(totalInclusive);

        itemCount.set(cartItems.stream().mapToInt(CartItem::getQuantity).sum());
    }

    public ObservableList<CartItem> getCartItems() {
        return cartItems;
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

    public ObjectProperty<Client> selectedClientProperty() {
        return selectedClient;
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
        cartItems.stream()
                .filter(item -> item.getProduct().getId() == product.getId())
                .findFirst()
                .ifPresent(item -> {
                    if (item.getQuantity() > 1) {
                        item.setQuantity(item.getQuantity() - 1);
                    } else {
                        removeItem(product);
                    }
                });
    }

    public double getSubtotal() {
        return subtotal.get();
    }

    public double getTax() {
        return tax.get();
    }

    public void updateQuantity(Product product, int quantity) {
        if (quantity < 1) {
            removeItem(product);
            return;
        }
        cartItems.stream()
                .filter(item -> item.getProduct().getId() == product.getId())
                .findFirst()
                .ifPresent(item -> item.setQuantity(quantity));
    }

    public void clear() {
        cartItems.clear();
        selectedClient.set(null);
    }

    public Client getSelectedClient() {
        return selectedClient.get();
    }

    public void setSelectedClient(Client client) {
        selectedClient.set(client);
    }

    public double getGrandTotal() {
        return grandTotal.get();
    }

    public int getItemCount() {
        return itemCount.get();
    }
}
