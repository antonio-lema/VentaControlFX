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

import com.mycompany.ventacontrolfx.domain.model.Price;
import com.mycompany.ventacontrolfx.domain.model.PriceList;
import com.mycompany.ventacontrolfx.domain.repository.IPriceRepository;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;

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
    private final IntegerProperty priceListId = new SimpleIntegerProperty(-1);

    private final ICompanyConfigRepository configRepository;
    private final IPriceRepository priceRepository;

    public CartUseCase(ICompanyConfigRepository configRepository, IPriceRepository priceRepository) {
        this.configRepository = configRepository;
        this.priceRepository = priceRepository;
        this.cartItems = FXCollections
                .observableArrayList((CartItem item) -> new Observable[] { item.quantityProperty() });
        this.cartItems.addListener((ListChangeListener.Change<? extends CartItem> c) -> updateTotals());

        this.selectedClient.addListener((obs, oldClient, newClient) -> {
            if (newClient != null && newClient.getPriceListId() > 0) {
                setPriceListId(newClient.getPriceListId());
            } else if (newClient == null) {
                // When client is removed, if we want to go back to default:
                try {
                    PriceList defaultList = priceRepository.getDefaultPriceList();
                    if (defaultList != null) {
                        setPriceListId(defaultList.getId());
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });

        this.priceListId.addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                updatePricesForList(newVal.intValue());
            }
        });
    }

    private void updatePricesForList(int newListId) {
        // 1. Tomamos una COPIA de los items en el hilo FX (seguro)
        List<CartItem> snapshot = new ArrayList<>(cartItems);
        System.out
                .println("[CartUseCase] updatePricesForList(" + newListId + ") - items en carrito: " + snapshot.size());
        if (snapshot.isEmpty())
            return;

        // 2. Hacemos las consultas a BD en un hilo de fondo
        Thread bgThread = new Thread(() -> {
            List<Runnable> uiUpdates = new ArrayList<>();
            for (CartItem item : snapshot) {
                try {
                    Optional<Price> price = priceRepository.getActivePrice(
                            item.getProduct().getId(), newListId);
                    System.out.println("[CartUseCase]   Producto ID=" + item.getProduct().getId()
                            + " - precio encontrado: " + price.isPresent()
                            + (price.isPresent() ? " = " + price.get().getValue() : ""));
                    if (price.isPresent()) {
                        final double newPrice = price.get().getValue();
                        uiUpdates.add(() -> item.updateUnitPrice(newPrice));
                    }
                } catch (SQLException e) {
                    System.out.println("[CartUseCase]   ERROR SQL: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // 3. Aplicamos SOLO si la tarifa no cambió mientras consultábamos la BD
            // Esto evita el race condition cuando el usuario cambia la tarifa rápidamente
            javafx.application.Platform.runLater(() -> {
                if (priceListId.get() != newListId) {
                    // El usuario cambió la tarifa de nuevo: descartamos esta actualización obsoleta
                    System.out.println("[CartUseCase] Descartando precios obsoletos de tarifa " + newListId
                            + " (tarifa actual: " + priceListId.get() + ")");
                    return;
                }
                System.out.println(
                        "[CartUseCase] Aplicando " + uiUpdates.size() + " actualizaciones de precio en FX thread");
                for (Runnable update : uiUpdates) {
                    update.run();
                }
                updateTotals();
            });
        }, "cart-price-update-" + newListId);
        bgThread.setDaemon(true);
        bgThread.start();
    }

    private void updateTotals() {
        SaleConfig config = configRepository.load();
        double totalInclusive = 0.0;
        double totalBase = 0.0;
        boolean isInclusive = config.isPricesIncludeTax();

        for (CartItem item : cartItems) {
            double lineTotal = Math.round(item.getTotal() * 100.0) / 100.0; // Este es qty * price
            double effectiveRate = item.getProduct().resolveEffectiveIva(config.getTaxRate());

            if (isInclusive) {
                // El precio ya tiene el IVA. Extraemos la base de forma estricta.
                double lineBase = Math.round((lineTotal / (1.0 + (effectiveRate / 100.0))) * 100.0) / 100.0;
                totalBase += lineBase;
                totalInclusive += lineTotal;
            } else {
                // El precio es la base. Calculamos el IVA y lo sumamos.
                double lineTax = Math.round((lineTotal * (effectiveRate / 100.0)) * 100.0) / 100.0;
                totalBase += lineTotal;
                totalInclusive += (lineTotal + lineTax);
            }
        }

        subtotal.set(Math.round(totalBase * 100.0) / 100.0);
        tax.set(Math.round((totalInclusive - totalBase) * 100.0) / 100.0);
        grandTotal.set(Math.round(totalInclusive * 100.0) / 100.0);

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

    public IntegerProperty priceListIdProperty() {
        return priceListId;
    }

    public void setPriceListId(int id) {
        this.priceListId.set(id);
    }

    public int getPriceListId() {
        return priceListId.get();
    }

    public void addItem(Product product) {
        addItem(product, 1);
    }

    public void addItem(Product product, int quantity) {
        Optional<CartItem> existingItem = cartItems.stream()
                .filter(item -> item.getProduct().getId() == product.getId())
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
        } else {
            cartItems.add(new CartItem(product, quantity));
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
