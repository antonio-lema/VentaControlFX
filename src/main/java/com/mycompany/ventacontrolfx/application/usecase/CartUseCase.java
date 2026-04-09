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

import com.mycompany.ventacontrolfx.application.service.PromotionService;
import com.mycompany.ventacontrolfx.domain.model.Price;
import com.mycompany.ventacontrolfx.domain.model.PriceList;
import com.mycompany.ventacontrolfx.domain.repository.IPriceRepository;
import com.mycompany.ventacontrolfx.domain.service.TaxEngineService;
import com.mycompany.ventacontrolfx.domain.service.PriceResolutionService;
import com.mycompany.ventacontrolfx.domain.repository.IProductRepository;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
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
    private final DoubleProperty totalSavings = new SimpleDoubleProperty(0.0);
    private final IntegerProperty itemCount = new SimpleIntegerProperty(0);
    private final ObjectProperty<Client> selectedClient = new SimpleObjectProperty<>(null);
    private final IntegerProperty priceListId = new SimpleIntegerProperty(-1);
    private final IntegerProperty selectedCategoryId = new SimpleIntegerProperty(-2);
    private final StringProperty generalObservation = new SimpleStringProperty("");
    private final BooleanProperty locked = new SimpleBooleanProperty(false);

    private final ICompanyConfigRepository configRepository;
    private final com.mycompany.ventacontrolfx.domain.service.PriceResolutionService priceResolutionService;
    private final TaxEngineService taxEngineService;
    private final com.mycompany.ventacontrolfx.application.service.PromotionEngine promotionEngine;
    private final IProductRepository productRepository;
    private int systemObservationProductId = -1;

    public CartUseCase(ICompanyConfigRepository configRepository,
            PriceResolutionService priceResolutionService,
            TaxEngineService taxEngineService,
            PromotionService promotionService,
            com.mycompany.ventacontrolfx.application.service.PromotionEngine promotionEngine,
            IPriceRepository priceRepository,
            IProductRepository productRepository) {
        this.configRepository = configRepository;
        this.taxEngineService = taxEngineService;
        this.promotionEngine = promotionEngine;
        this.priceResolutionService = priceResolutionService;
        this.productRepository = productRepository;
        this.cartItems = FXCollections
                .observableArrayList((CartItem item) -> new Observable[] { item.quantityProperty() });
        this.cartItems.addListener((ListChangeListener.Change<? extends CartItem> c) -> updateTotals());

        this.selectedClient.addListener((obs, oldClient, newClient) -> {
            try {
                if (newClient != null && newClient.getPriceListId() > 0) {
                    setPriceListId(newClient.getPriceListId());
                } else {
                    // Si el cliente no tiene tarifa o es nulo, volvemos a la por defecto
                    PriceList defaultList = priceRepository.getDefaultPriceList();
                    if (defaultList != null) {
                        setPriceListId(defaultList.getId());
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
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
                    Optional<Price> price = priceResolutionService.resolvePrice(
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

            // 3. Aplicamos SOLO si la tarifa no cambi\u00c3\u00b3 mientras consult\u00c3\u00a1bamos la BD
            // Esto evita el race condition cuando el usuario cambia la tarifa r\u00c3\u00a1pidamente
            javafx.application.Platform.runLater(() -> {
                if (priceListId.get() != newListId) {
                    // El usuario cambi\u00c3\u00b3 la tarifa de nuevo: descartamos esta actualizaci\u00c3\u00b3n obsoleta
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

    public void updateTotals() {
        SaleConfig config = configRepository.load();
        double totalInclusive = 0.0;
        double totalBase = 0.0;
        boolean isInclusive = config.isPricesIncludeTax();
        Client client = selectedClient.get();

        // APLICAR MOTOR DE PROMOCIONES (PIPELINE)
        com.mycompany.ventacontrolfx.application.service.PromotionResult promoResult = promotionEngine
                .process(new ArrayList<>(cartItems));

        double grossSavingsTotal = 0.0;

        for (CartItem item : cartItems) {
            Product product = item.getProduct();
            try {
                // Obtener descuento espec\u00c3\u00adfico para este producto desde el motor
                double autoDiscount = promoResult.getItemDiscounts().getOrDefault(product.getId(), 0.0);
                double totalLineDiscount = Math.min(autoDiscount + item.getManualDiscountAmount(),
                        item.getUnitPrice() * item.getQuantity());
                item.setDiscountAmount(totalLineDiscount);

                // Acumular ahorros para la etiqueta (gross-up si es necesario)
                double taxRate = product.resolveEffectiveIva(config.getTaxRate());
                double taxMultiplier = isInclusive ? 1.0 : (1.0 + (taxRate / 100.0));
                grossSavingsTotal += totalLineDiscount * taxMultiplier;

                // Precio unitario original (con tarifa de cliente ya aplicada)
                double unitPrice = item.getUnitPrice();

                // Calculamos el total de la l\u00c3\u00adnea YA con descuento para que el TaxEngine
                // recalcule la base e IVA
                double grossLineTotal = (unitPrice * item.getQuantity()) - totalLineDiscount;

                // Simulamos un precio unitario efectivo para el motor de impuestos
                // (grossLineTotal / quantity) nos da el precio unitario real pagado
                double effectiveUnitPrice = item.getQuantity() > 0 ? (grossLineTotal / item.getQuantity()) : 0.0;

                com.mycompany.ventacontrolfx.domain.model.TaxCalculationResult result = taxEngineService.calculateLine(
                        product, client, effectiveUnitPrice, item.getQuantity(), isInclusive);

                totalBase += result.getNetTotal();
                totalInclusive += result.getGrossTotal();
            } catch (SQLException e) {
                // Fallback en caso de error en el motor de impuestos
                double autoDiscount = promoResult.getItemDiscounts().getOrDefault(product.getId(), 0.0);
                double totalLineDiscount = Math.min(autoDiscount + item.getManualDiscountAmount(),
                        item.getUnitPrice() * item.getQuantity());

                double taxRate = product.resolveEffectiveIva(config.getTaxRate());
                double taxMultiplier = isInclusive ? 1.0 : (1.0 + (taxRate / 100.0));
                grossSavingsTotal += totalLineDiscount * taxMultiplier;

                double lineTotal = (Math.round(item.getTotal() * 100.0) / 100.0); // CartItem.getTotal() ya descuenta
                double effectiveRate = product.resolveEffectiveIva(config.getTaxRate());

                if (isInclusive) {
                    double lineBase = Math.round((lineTotal / (1.0 + (effectiveRate / 100.0))) * 100.0) / 100.0;
                    totalBase += lineBase;
                    totalInclusive += lineTotal;
                } else {
                    double lineTax = Math.round((lineTotal * (effectiveRate / 100.0)) * 100.0) / 100.0;
                    totalBase += lineTotal;
                    totalInclusive += (lineTotal + lineTax);
                }
            }
        }

        double globalDiscount = promoResult.getItemDiscounts().getOrDefault(-1, 0.0);
        if (globalDiscount > 0) {
            // El descuento global no puede superar el total acumulado
            if (globalDiscount > totalInclusive) {
                globalDiscount = totalInclusive;
            }
            totalInclusive -= globalDiscount;
            grossSavingsTotal += globalDiscount;
        }

        subtotal.set(Math.round(totalBase * 100.0) / 100.0);
        tax.set(Math.round((totalInclusive - totalBase) * 100.0) / 100.0);
        grandTotal.set(Math.round(totalInclusive * 100.0) / 100.0);
        totalSavings.set(Math.round(grossSavingsTotal * 100.0) / 100.0);

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

    public DoubleProperty totalSavingsProperty() {
        return totalSavings;
    }

    public double getTotalSavings() {
        return totalSavings.get();
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

    public StringProperty generalObservationProperty() {
        return generalObservation;
    }

    public String getGeneralObservation() {
        return generalObservation.get();
    }

    public void setGeneralObservation(String observation) {
        this.generalObservation.set(observation);
    }

    public int getPriceListId() {
        return priceListId.get();
    }

    public IntegerProperty selectedCategoryIdProperty() {
        return selectedCategoryId;
    }

    public int getSelectedCategoryId() {
        return selectedCategoryId.get();
    }

    public void setSelectedCategoryId(int id) {
        this.selectedCategoryId.set(id);
    }

    public void addItem(Product product) {
        if (locked.get()) {
            throw new IllegalStateException(
                    "El carrito est\u00c3\u00a1 BLOQUEADO temporalmente. Debe realizar el cierre de caja o fichar inicio de turno.");
        }
        addItem(product, 1);
    }

    public void addItem(Product product, int quantity) {
        if (product.isManageStock()) {
            Optional<CartItem> existingItem = cartItems.stream()
                    .filter(item -> item.getProduct().getId() == product.getId())
                    .findFirst();
            int currentQty = existingItem.map(CartItem::getQuantity).orElse(0);
            if (currentQty + quantity > product.getStockQuantity()) {
                throw new IllegalArgumentException(
                        "Stock insuficiente (" + product.getStockQuantity() + " disponibles)");
            }
        }

        Optional<CartItem> existingItem = cartItems.stream()
                .filter(item -> item.getProduct().getId() == product.getId() && product.getId() > 0)
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
        } else {
            cartItems.add(new CartItem(product, quantity));
        }
    }

    public void addCustomItem(String name, double price) {
        try {
            Product genericProduct = productRepository.findBySku("SYS-GEN-001");
            if (genericProduct != null) {
                // Creamos un producto virtual basado en el gen\u00c3\u00a9rico
                Product customProduct = new Product(
                        genericProduct.getId(),
                        genericProduct.getCategoryId(),
                        name,
                        price,
                        false,
                        true,
                        genericProduct.getImagePath(),
                        genericProduct.getCategoryName(),
                        genericProduct.getIva(),
                        genericProduct.getCategoryIva(),
                        genericProduct.getSku(),
                        0,
                        true,
                        0,
                        0,
                        false);
                customProduct.setCurrentPrice(price);

                // Agrupamos solo si el nombre y el precio coinciden exactamente
                Optional<CartItem> existingItem = cartItems.stream()
                        .filter(item -> {
                            Product p = item.getProduct();
                            return "SYS-GEN-001".equals(p.getSku()) &&
                                    name.equals(p.getName()) &&
                                    item.getUnitPrice() == price;
                        })
                        .findFirst();

                if (existingItem.isPresent()) {
                    existingItem.get().setQuantity(existingItem.get().getQuantity() + 1);
                } else {
                    cartItems.add(new CartItem(customProduct, 1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
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

        if (product.isManageStock() && quantity > product.getStockQuantity()) {
            throw new IllegalArgumentException("Stock insuficiente (" + product.getStockQuantity() + " disponibles)");
        }

        cartItems.stream()
                .filter(item -> item.getProduct().getId() == product.getId())
                .findFirst()
                .ifPresent(item -> item.setQuantity(quantity));
    }

    public void addObservation(String text) {
        setGeneralObservation(text);
    }

    public void clear() {
        cartItems.clear();
        selectedClient.set(null);
        generalObservation.set("");
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

    public BooleanProperty lockedProperty() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked.set(locked);
    }

    public boolean isLocked() {
        return locked.get();
    }
}
