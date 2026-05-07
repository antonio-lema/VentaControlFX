package com.mycompany.ventacontrolfx.presentation.controller.cart;

import com.mycompany.ventacontrolfx.application.usecase.CartUseCase;
import com.mycompany.ventacontrolfx.infrastructure.navigation.NavigationService;
import com.mycompany.ventacontrolfx.presentation.renderer.CartListRenderer;
import com.mycompany.ventacontrolfx.infrastructure.config.ServiceContainer;
import com.mycompany.ventacontrolfx.infrastructure.config.Injectable;
import com.mycompany.ventacontrolfx.presentation.util.AlertUtil;
import javafx.application.Platform;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;

public class CartController implements Injectable {

    @FXML private VBox cartItemsContainer, emptyCartView, observationContainer;
    @FXML private Label subtotalLabel, taxLabel, savingsLabel, itemsCountLabel, totalButtonLabel, lblSelectedClient, lblCurrentPriceList, lblGeneralObservation;
    @FXML private HBox hboxSavings;
    @FXML private Button btnClearCart, payButton, btnRemoveClient, showAddClientDialog;
    @FXML private TextField txtPromoCode;

    private ServiceContainer container;
    private CartUseCase cartUseCase;
    private CartListRenderer cartRenderer;
    
    // Managers Delegados
    private CartUIManager uiManager;
    private CartPaymentManager paymentManager;
    private CartSummaryManager summaryManager;
    private CartClientManager clientManager;
    private CartSuspensionManager suspensionManager;

    @Override
    public void inject(ServiceContainer container) {
        this.container = container;
        this.cartUseCase = container.getCartUseCase();
        
        // 1. Inicializar Managers
        this.uiManager = new CartUIManager(container);
        this.paymentManager = new CartPaymentManager(container, cartUseCase);
        this.summaryManager = new CartSummaryManager(container, cartUseCase);
        this.clientManager = new CartClientManager(container, cartUseCase, lblSelectedClient, lblCurrentPriceList, btnRemoveClient, showAddClientDialog);
        this.suspensionManager = new CartSuspensionManager(container, cartUseCase, uiManager);

        // 2. Setup UI & Bindings
        setupRenderer(container);
        setupViewVisibility();
        summaryManager.bind(subtotalLabel, taxLabel, savingsLabel, itemsCountLabel, totalButtonLabel, hboxSavings, txtPromoCode);
        
        initListeners();
        clientManager.refreshPriceListLabel();
        clientManager.update(cartUseCase.getSelectedClient());
    }

    private void setupRenderer(ServiceContainer container) {
        com.mycompany.ventacontrolfx.domain.model.SaleConfig cfg = container.getICompanyConfigRepository().load();
        this.cartRenderer = new CartListRenderer(cartItemsContainer, cartUseCase, cfg.getTaxRate(), cfg.isPricesIncludeTax(), container);
    }

    private void setupViewVisibility() {
        cartItemsContainer.visibleProperty().bind(cartUseCase.itemCountProperty().greaterThan(0));
        cartItemsContainer.managedProperty().bind(cartItemsContainer.visibleProperty());
        emptyCartView.visibleProperty().bind(cartUseCase.itemCountProperty().isEqualTo(0));
        emptyCartView.managedProperty().bind(emptyCartView.visibleProperty());
        payButton.disableProperty().bind(cartUseCase.itemCountProperty().isEqualTo(0));
        payButton.visibleProperty().bind(cartUseCase.itemCountProperty().greaterThan(0));
        payButton.managedProperty().bind(payButton.visibleProperty());
        btnClearCart.visibleProperty().bind(cartUseCase.itemCountProperty().greaterThan(0));
        btnClearCart.managedProperty().bind(btnClearCart.visibleProperty());
        observationContainer.visibleProperty().bind(cartUseCase.generalObservationProperty().isNotEmpty());
        observationContainer.managedProperty().bind(observationContainer.visibleProperty());
        lblGeneralObservation.textProperty().bind(cartUseCase.generalObservationProperty());
    }

    private void initListeners() {
        cartUseCase.selectedClientProperty().addListener((obs, old, nv) -> clientManager.update(nv));
        cartUseCase.priceListIdProperty().addListener((obs, old, nv) -> {
            if (nv != null) {
                clientManager.refreshPriceListLabel();
                new Thread(() -> { 
                    try { Thread.sleep(400); } catch (Exception ignored) {} 
                    Platform.runLater(cartRenderer::refreshAllPrices);
                }, "cart-ui-refresh").start();
            }
        });
    }

    @FXML private void clearCart() {
        if (container.getUserSession().hasPermission("venta.limpiar")) cartUseCase.clear();
        else AlertUtil.showError(container.getBundle().getString("cart.error.clear_denied.title"), container.getBundle().getString("cart.error.clear_denied.msg"));
    }

    @FXML private void handlePayButton() { paymentManager.processPayment(); }
    @FXML private void handleRemoveSelectedClient() { cartUseCase.setSelectedClient(null); }
    @FXML private void showAddClientDialog() { container.getNavigationService().navigateTo("/view/dialog/clients.fxml"); }
    @FXML private void handleApplyPromo() { cartUseCase.updateTotals(); AlertUtil.showToast("C\u00f3digo procesado"); }
    @FXML private void handleSuspendCart() { suspensionManager.suspendCart(); }
    @FXML private void handleShowSuspendedCarts() { suspensionManager.showSuspendedCarts(); }
    @FXML private void handleAddManualItem() { uiManager.showManualItemDialog(cartUseCase::addCustomItem); }

    @FXML private void handleAddObservation() { uiManager.showObservationDialog(cartUseCase.getGeneralObservation(), cartUseCase::setGeneralObservation); }
    @FXML private void handleClearObservation() { cartUseCase.setGeneralObservation(""); }
    @FXML private void handleChangePriceList() {
        uiManager.showPriceListSelector(cartUseCase.getPriceListId(), sel -> { if (sel != null) cartUseCase.setPriceListId(sel.getId()); });
    }
}


